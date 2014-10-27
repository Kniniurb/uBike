package com.plusot.senselib.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Base64OutputStream;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.Watchdog;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.store.PwxLog;
import com.plusot.senselib.values.Value;

public class TrainingPeaksHttpUpload {
	private static String CLASSTAG = TrainingPeaksHttpUpload.class.getSimpleName();
	private final Listener listener;

	public interface Listener {
		void onResult(Result result);
		void onUpdate(String value);
	}

	public TrainingPeaksHttpUpload(final Listener listener) {
		this.listener = listener;
	}

	public void doSend() {
		new SendTask().execute();
	}

	public class Result {
		public final int id;
		public final boolean success;
		public final int msg;
		public final String details;
		public final long bytes;
		public Result(final int id,
				final boolean success,
				final int msg,
				final String details,
				final long bytes) {
			this.success = success;
			this.msg = msg;
			this.details = details;
			this.bytes = bytes;
			this.id = id;

		}
	}

	private class ProgressItem {
		final long count;
		final String msg;
		public ProgressItem(final long count) {
			this.count = count;
			this.msg = null;
		}
		
		public ProgressItem(final String msg) {
			this.count = -1;
			this.msg = msg;
		}
	}

	private class SendTask extends AsyncTask<Void, ProgressItem, Result> {
		private static final String urlString = "https://www.trainingpeaks.com/TPWebServices/Service.asmx";

		@Override
		protected Result doInBackground(Void ... vvv) {
			int id = Watchdog.addProcessS(CLASSTAG + ".SendTask");
			PwxLog log = PwxLog.getInstance();
			if (log == null) return new Result(id, false, R.string.tp_result_nofile,"",0);
			
			String session;
			switch (SenseGlobals.activity) {
			case RUN:	
				session = Value.getSessionString();
				File file = PwxLog.getFile(session);
				if (file == null || System.currentTimeMillis() - file.lastModified() > 300000) log.close(CLASSTAG, false, true);
				break;
			case REPLAY:
				session = SenseGlobals.replaySession;
				break;
			default:
			case STOP:
				session = Value.getSessionString();
				file = PwxLog.getFile(session);
				if (file == null) return new Result(id, false, R.string.tp_result_nofile,"",0);
				break;
			}
			String filename = PwxLog.getFileName(session);
			
			if (filename == null) return new Result(id, false, R.string.tp_result_nofile,"",0);
			//if (prefs == null) return new Result(id, false, R.string.tp_result_closeingapp,"",0);
			publishProgress(new ProgressItem(Globals.appContext.getString(R.string.tp_file, filename)));
			boolean result = false;
			URL url;
			HttpURLConnection con = null;
			RandomAccessFile is = null;
			OutputStream os = null;
			InputStream in = null;
			long total = 0;

			try {
				is = new RandomAccessFile(new File(filename), "r");
			} catch (FileNotFoundException e1) {
				return new Result(id, false, R.string.tp_result_filenotfound, filename, 0);
			}

			try {
				url = new URL(urlString);
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("POST");
				con.setRequestProperty("Content-Type", "application/soap+xml");
				con.setDoInput(true);
				con.setDoOutput(true);
				con.setChunkedStreamingMode(0);
				//con.connect();

				os = con.getOutputStream();
				//in = /*new BufferedReader(new InputStreamReader(*/con.getInputStream(); //));



				//				String authentication = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				//						"SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\">" +
				//						"<SOAP-ENV:Body xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
				//						"<AuthenticateAccount xmlns=\"http://www.trainingpeaks.com/TPWebServices/\">" +
				//						"<username xsi:type=\"xsd:string\" xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\">" + prefs.getTrainingPeaksUser() + "</username>" +
				//						"<password xsi:type=\"xsd:string\" xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\">" + prefs.getTrainingPeaksPassword() + "</password>" +
				//						"</AuthenticateAccount></SOAP-ENV:Body></SOAP-ENV:Envelope>";
				//
				//				ostream.write(authentication.getBytes());
				//				ostream.flush();
				//				
				//				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				//				StringBuffer sb = new StringBuffer("");
				//				String line = "";
				//				while ((line = in.readLine()) != null) {
				//					sb.append(line).append(Globals.ln);
				//					if (line.contains("<ImportFileForUserResult>true</ImportFileForUserResult>")) {
				//						result = true;
				//					}
				//				}
				//in.close();

				final String start = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
						"<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" +
						"<soap12:Body>" +
						"<ImportFileForUser xmlns=\"http://www.trainingpeaks.com/TPWebServices/\">" +
						"<username>" + PreferenceKey.TPUSER.getString() + "</username>" +
						"<password>" + PreferenceKey.TPPASS.getString() + "</password>" +
						"<byteData>";
				final String end = "</byteData>" +
						"</ImportFileForUser>" +
						"</soap12:Body>" +
						"</soap12:Envelope>";
				os.write(start.getBytes());
				//os.flush();


				byte[] buffer = new byte[1024]; //];
				int count;
				Base64OutputStream base64 = new Base64OutputStream(os, Base64.NO_WRAP | Base64.NO_CLOSE);
				while ((count = is.read(buffer)) != -1) {
					base64.write(buffer, 0, count); // This sometimes causes OutOfMemoryError
					total += count;
					base64.flush();
					publishProgress(new ProgressItem(total));
				}
				base64.close();
				os.write(end.getBytes());
				os.flush();
				os.close();


				LLog.d(Globals.TAG, CLASSTAG + ".SendTask.doInBackground sent " + total + " bytes.");
				LLog.d(Globals.TAG, CLASSTAG + ".SendTask.doInBackground response = " + con.getResponseMessage() + "(" + con.getResponseCode()  + ")");
				if (con.getResponseCode() == 500) {
					return new Result(id, false, R.string.tp_result_communicationfailure, con.getResponseMessage(), 0);
				}

				in = /*new BufferedReader(new InputStreamReader(*/con.getInputStream(); //));

				StringBuffer sb = new StringBuffer("");
				//String line = "";
				while ((count = in.read(buffer)) != -1) {
					sb.append(new String(buffer));//.append(Globals.ln);
				}
				if (sb.toString().contains("<ImportFileForUserResult>true</ImportFileForUserResult>")) {
					result = true;
				} else
					LLog.d(Globals.TAG, CLASSTAG +  ".SendTask.doInBackground response = " + sb.toString());


			} catch (MalformedURLException e) {
				LLog.d(Globals.TAG, CLASSTAG + ".SendTask.doInBackground MalformedURLException", e);
				return new Result(id, false, R.string.tp_result_wrongurl,urlString,0);
			} catch (FileNotFoundException e) {
				LLog.d(Globals.TAG, CLASSTAG + ".SendTask.doInBackground FileNotFoundException", e);
				return new Result(id, false, R.string.tp_result_unknownerror, e.getMessage(), 0);
			} catch (IOException e) {
				LLog.d(Globals.TAG, CLASSTAG + ".SendTask.doInBackground IOException", e);
				return new Result(id, false, R.string.tp_result_communicationfailure,e.getMessage(),0);
			} finally {

				if (is != null) try {
					is.close();
				} catch (IOException e) {
					LLog.d(Globals.TAG, CLASSTAG + ".SendTask.doInBackground Could not close stream", e);
				}
				if (os != null) try {
					os.close();
				} catch (IOException e) {
					LLog.d(Globals.TAG, CLASSTAG + ".SendTask.doInBackground Could not close stream", e);
				}
				if (in != null) try {
					in.close();
				} catch (IOException e) {
					LLog.d(Globals.TAG, CLASSTAG + ".SendTask.doInBackground Could not close stream", e);
				}
				if (con != null) con.disconnect();
			}

			if (result) return new Result(id,true, R.string.tp_result_success,"", total);
			return new Result(id, false, R.string.tp_result_notok,"", 0);
		}

		@Override
		protected void onProgressUpdate(ProgressItem... update) {
			if (update.length > 0 && listener != null) {
				if (update[0].msg != null)
					listener.onUpdate(update[0].msg); 
				else
					listener.onUpdate(Globals.appContext.getString(R.string.tp_progress, update[0].count)); 		
			}
		}

		@Override
		protected void onPostExecute(Result result) {
			try {
				if (listener != null) {

					if (result.success) 
						listener.onUpdate(Globals.appContext.getString(R.string.trainingpeaks_upload_success));
					else
						listener.onUpdate(Globals.appContext.getString(R.string.trainingpeaks_upload_failed));
				}
				if (listener != null) listener.onResult(result);
			} finally {
				Watchdog.removeProcessS(result.id, CLASSTAG);
			}
		}
	}

}
