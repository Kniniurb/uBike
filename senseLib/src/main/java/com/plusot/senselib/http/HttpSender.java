package com.plusot.senselib.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.Watchdog;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.senselib.db.DbData;
import com.plusot.senselib.online.OnlineAckPacket;
import com.plusot.senselib.online.OnlineActionType;
import com.plusot.senselib.online.OnlineDataPacket;
import com.plusot.senselib.online.OnlineMessage;
import com.plusot.senselib.online.OnlineMessageElement;
import com.plusot.senselib.online.OnlinePacket;
import com.plusot.senselib.online.OnlinePacketType;
import com.plusot.senselib.online.OnlineSessionPacket;
import com.plusot.senselib.online.OnlineUserPacket;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.util.Hardware;
import com.plusot.senselib.util.SenseUserInfo;
import com.plusot.senselib.values.Value;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

public class HttpSender {
	private static final String CLASSTAG = HttpSender.class.getSimpleName();
    private static final boolean DEBUG = true;
	private static final int MAX_POSTS_TO_DRAIN = 10; 
	private static int runners = 0;

	public static final String MSG_RESULT = "MSG_RESULT";
	public static final String MSG_WARN = "MSG_WARNING";
	public static int httpRecvCount = 0;
	public static int httpSentCount = 0;
	public static int httpFailCount = 0;
	private static HttpSender instance = null;
	private String baseUri;
	private final ConnectivityManager connMgr; 
	//	private BlockingQueue<JSONObject> posts = new LinkedBlockingQueue<JSONObject>(); 
	//	private BlockingQueue<OnlineDataPacket> packetBuf = new LinkedBlockingQueue<OnlineDataPacket>(); 
	private HttpRunner runner = null; 
	private long lastSent = 0;
	private static OnlineSessionPacket sessionPacket = null;
	private static int sessionId = -1;
	private int minId = 0;
	private static long lastTimeSent = 0;
	
	public static boolean isHttpPost = false;

	public static long getLastTimeSent() {
		if (lastTimeSent == 0) {
			lastTimeSent = PreferenceKey.LASTTIMESENTHTTP.getLong();
		}
		return lastTimeSent;
	}

	private HttpSender(final Context context, final String baseUri) {
		this.connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		this.baseUri = baseUri;
		LLog.d(Globals.TAG, CLASSTAG + " to " + baseUri);

		this.runner = new HttpRunner();
		lastTimeSent = PreferenceKey.LASTTIMESENTHTTP.getLong();
		this.runner.start();
	}

	private void addToQueue(OnlinePacketType type, JSONObject value) throws InterruptedException {
		NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
		if (netInfo == null) return;
		long now = System.currentTimeMillis();
		if (lastSent != 0 && now - lastSent > 180000 && runners < 8) {
			LLog.e(Globals.TAG, CLASSTAG + ".addToQueue: Time to send HTTP too long !!!!!!!!!!!!!!!!!!!!! (" + (now - lastSent) / 1000 + " seconds), starting runner " + (runners + 1));
			LLog.i(Globals.TAG, CLASSTAG + ".addToQueue: Sent = " + httpSentCount + ", Received =" + httpRecvCount + ", Failed = " + httpFailCount);
			this.runner = new HttpRunner();
			this.runner.start();
		}
	}

	public static void stopInstance() {
		if (instance != null) synchronized(HttpSender.class) {
			if (instance != null) instance.stop();
			instance = null;
		}
	}

	private void stop() {
		if (runner != null) {
			synchronized(HttpSender.this) {
				if (runner != null) {
					runner.stopIt();
					runner = null;
				}
			}
		}
	}

	private class HttpRunner extends Thread implements DbData.User {
		private final HttpClient client;
		private boolean stopped = false;

		public HttpRunner() {
			client = new DefaultHttpClient();

			//progress = ProgressDialog.show(context, "", "Loading: " + uri, true);
			//this.context = context;
			this.setName("HttpRunner_" + this.getId());
		}

		private void sendHandlerMsg(String key, String msgStr) {
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putString(key, msgStr);
			msg.setData(bundle);
			handler.sendMessage(msg);
			//LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.sendHandlerMsg: " + key + " = " + OnlineMessage.decode(msgStr));
		}

		private boolean executeGet(String params) throws URISyntaxException, ClientProtocolException, IOException  {
			BufferedReader in = null;
			String page = null;

			try {
				HttpGet request = new HttpGet(new URI(baseUri + params));
				String s;
				try {
					s = OnlineMessage.decode(params);
				} catch (OnlineMessage.OnlineDecodeException e) {
					LLog.d(Globals.TAG,  CLASSTAG + ".HttpRunner.executeHttpGet  Could not decode: " + params);
					s = params;
				}
				//String sub = s.substring(0, Math.min(s.length(), 80));
				//LLog.d(Globals.TAG, CLASSTAG + ".HttpRunner.executeHttpGet: " + baseUri + s + " (" + s.length() +")");
				if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".HttpRunner.executeHttpGet sending: " + s.length() + " characters: " + s);
				HttpResponse response = client.execute(request);

				//				if (runner != null) synchronized(runner) {
				//					runner.notify();	
				//				}

				in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuffer sb = new StringBuffer("");
				String line = "";
				String NL = System.getProperty("line.separator");
				while ((line = in.readLine()) != null) {
					sb.append(line + NL);
				}
				in.close();
				page = sb.toString();


			} finally {
				//progress.cancel();
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						LLog.e(Globals.TAG, CLASSTAG + ".HttpRunner.executeHttpGet: Error in closing BufferReader", e);
					}
				}
			}
			if (page != null) {
				//LLog.d(Globals.TAG, CLASSTAG + ".executeHttpGet: Page = " + URLDecoder.decode(page, "UTF-8"));
				sendHandlerMsg(MSG_RESULT, page);
				if (page.contains("ACK")) {
					if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".executeHttpGet: Page contains ACK");
					return true;
				} else {
					if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".executeHttpGet: Page contains no ACK: " + page);

				}
			}
			return false;
		}

		public void run() {
			runners++;
			int id = Watchdog.addProcessS(CLASSTAG + ".run");
			LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: Starting new thread: " + getName());
			//			Queue<JSONObject> list = new LinkedList<JSONObject>();
			DbData dbData;
			DbData.addUser(this);
			DbData.DataItems list = null;
			int size = 0;
			boolean netConnected = true;

            while (!stopped && this == runner && (dbData = DbData.getInstance()) != null &&
                    Globals.runMode.isRun()) {
                OnlineMessage msg = HttpSender.this.createUserPacket();
                try {
                    if (executeGet("json=" + URLEncoder.encode(msg.toJSON().toString(), "UTF-8"))) {
                        break;
                    }
                } catch (UnsupportedEncodingException e) {
                    LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run UnsupportedEncodingException: " + e.getMessage());
                    httpFailCount++;
                } catch (URISyntaxException e) {
                    LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: URI not correct:" + e.getMessage());
                    httpFailCount++;
                } catch (ClientProtocolException e) {
                    LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: Client protocol exception: " + e.getMessage());
                    httpFailCount++;
                } catch (IOException e) {
                    LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: IO exception: " + e.getMessage());
                    httpFailCount++;
                } catch (JSONException e) {
                    LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: JSON exception: " + e.getMessage());
                    httpFailCount++;
                }
            }
            while (!stopped && this == runner && (dbData = DbData.getInstance()) != null &&
					(Globals.runMode.isRun() || (netConnected && size > 0)) 
					) {
				NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
				if (netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) {
					netConnected = true;
					if (list == null) synchronized (this) {
						list = dbData.getEntries(MAX_POSTS_TO_DRAIN, minId);
						if (list == null) {
							size = 0;
							if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".run: Msgs to send = " + size);
						} else {
							minId = list.maxId;
							size = list.data.size();
							if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".run: Msgs to send = " + size);
						}
					}
					if (list != null) try {
						httpSentCount++;
						String jsonString = "{\"" + OnlineMessageElement.BLOCKS.toString() + "\":[" +
								StringUtil.toString(list.data, ",") + "]}";

						lastSent = System.currentTimeMillis();
						if (executeGet("json=" + URLEncoder.encode(jsonString, "UTF-8"))) {
							lastSent = 0;
							dbData.setShared(list.minId, list.maxId);
							lastTimeSent = Math.max(list.maxTime, lastTimeSent);
							list = null;
							httpRecvCount++;
						}
					} catch (UnsupportedEncodingException e) {
						LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run UnsupportedEncodingException: " + e.getMessage());
						httpFailCount++;
					}  catch (URISyntaxException e) {
						LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: URI not correct:" + e.getMessage());
						httpFailCount++;
					} catch (ClientProtocolException e) {
						LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: Client protocol exception: " + e.getMessage());
						httpFailCount++;
					} catch (IOException e) {
						LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: IO exception: " + e.getMessage());
						//synchronized(posts) {
						//	posts.addAll(list);
						//} 
						httpFailCount++;
					}
				} else
					netConnected = false;
				try {
					if ((runner == this && size < MAX_POSTS_TO_DRAIN) || !netConnected || (lastSent != 0)) {
						sleep(10000);
					} else
						sleep(10);
				} catch (InterruptedException e) {
					LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run interrupted !!!!!!!!!!!!!!!!!", e);
					//stopped = true;
				} 
			} 
			synchronized(HttpSender.this) {
				PreferenceKey.LASTTIMESENTHTTP.set(lastTimeSent);
			}

			if (runner != this) 
				LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: Finished ----------------------- unused ----------------------- thread: " + getName());
			else
				LLog.i(Globals.TAG, CLASSTAG + ".HttpRunner.run: Finished thread " + getName() + 
						", stopped = " + stopped + 
						", netconnected = " + netConnected + 
						", datasize = " + size); 
			runners--;
			DbData.removeUser(this);
			Watchdog.removeProcessS(id, CLASSTAG);

		}

		public void stopIt() {
			stopped = true;
			interrupt();
		}
	};

	//	private long askedForSession = 0;

	public void sendData(String time, Map<String, String> map) {
		//LLog.e(Globals.TAG, CLASSTAG + ".sendData: Trying to send data");
		checkSession();

		OnlineDataPacket packet = new OnlineDataPacket(String.valueOf(SenseUserInfo.getDeviceId()), time, - sessionPacket.getSecondTime(), map);
		//		checkSession();
		//		if (sessionId == -1) packet.setSessionId((int) (- sessionPacket.getTime()));
		try {
			addToQueue(OnlinePacketType.DATA, new OnlineMessage(OnlineActionType.POST, packet).toJSON());
		} catch (JSONException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".sendData: Could not construct JSON object", e);
		} catch (InterruptedException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".sendData: Interrupted exception", e);
		}
	}

    public void sendUser() {
        SenseUserInfo.UserData user = SenseUserInfo.getUserData();
        if (user == null) return;
        OnlineUserPacket packet = new OnlineUserPacket(String.valueOf(SenseUserInfo.getDeviceId()), user.name, user.email, PreferenceKey.DISPLAYNAME.getString(user.name), Hardware.getModel(), PreferenceKey.SHAREPRIVACY.isTrue());
        OnlineMessage msg = new OnlineMessage(OnlineActionType.POST, packet);
        try {
            addToQueue(OnlinePacketType.USERINFO, msg.toJSON());
            return;
        } catch (JSONException e) {
            LLog.e(Globals.TAG, CLASSTAG + ".sendUser: Could not construct JSON object", e);
        } catch (InterruptedException e) {
            LLog.e(Globals.TAG, CLASSTAG + ".sendUser: Interrupted exception", e);
        }
    }

    public OnlineMessage createUserPacket() {
        SenseUserInfo.UserData user = SenseUserInfo.getUserData();
        OnlineUserPacket packet;
        if (user == null)
            packet = new OnlineUserPacket(String.valueOf(SenseUserInfo.getDeviceId()), "bike", "bike@bikesenses.net", PreferenceKey.DISPLAYNAME.getString(user.name), Hardware.getModel(), PreferenceKey.SHAREPRIVACY.isTrue());
        else
            packet = new OnlineUserPacket(String.valueOf(SenseUserInfo.getDeviceId()), user.name, user.email, PreferenceKey.DISPLAYNAME.getString(user.name), Hardware.getModel(), PreferenceKey.SHAREPRIVACY.isTrue());
        return new OnlineMessage(OnlineActionType.POST, packet);
//        try {
//            addToQueue(OnlinePacketType.USERINFO, msg.toJSON());
//            return;
//        } catch (JSONException e) {
//            LLog.e(Globals.TAG, CLASSTAG + ".sendUser: Could not construct JSON object", e);
//        } catch (InterruptedException e) {
//            LLog.e(Globals.TAG, CLASSTAG + ".sendUser: Interrupted exception", e);
//        }
    }

	public static void newSession() {
		sessionId = -1;
		LLog.d(Globals.TAG, CLASSTAG + ".newSession = " + Value.getSessionTime());
		sessionPacket = new OnlineSessionPacket(
				String.valueOf(SenseUserInfo.getDeviceId()), 
				Value.getSessionTime(), Value.getRides(), Value.getTotalTime(), (int)Value.getTotalDistance(), 
				(int)(Value.getTotalEnergy() / 1000.0), (int)Value.getTotalAscent(), (int)Value.getHighestPeak());
	}

	public static void checkSession() {
		if (sessionPacket == null) newSession();
	}

	public static int getSession() {
		return sessionId;	
	}

	public void sendSession() {
		checkSession();
		OnlineMessage msg = new OnlineMessage(OnlineActionType.POST, sessionPacket);
		try {
			addToQueue(OnlinePacketType.SESSION, msg.toJSON());
			return;
		} catch (JSONException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".sendSession: Could not construct JSON object", e);
		} catch (InterruptedException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".sendSession: Interrupted exception", e);
		}
	}

	public static HttpSender getInstance() {
		if (instance != null) return instance;
		String baseURI = null;
		if (!PreferenceKey.HTTPPOST.isTrue()) return null;

		baseURI = PreferenceKey.POST_URL.getString();
		if (baseURI == null || !baseURI.startsWith("http"))
            baseURI = PreferenceKey.POST_URL.getDefaultStringValue();
        baseURI = "http://192.168.1.104:4000/store?";
		synchronized(HttpSender.class) {
			if (instance == null) instance = new HttpSender(Globals.appContext, baseURI);
		}
		return instance;
	}

	private static Handler handler = new Handler() {
		public void handleMessage (Message msg) {
			Bundle bundle = msg.getData();
			Set<String> keys = bundle.keySet();
			for (String key: keys) {
				if (key.equals(HttpSender.MSG_RESULT)) {
					String value = bundle.getString(HttpSender.MSG_RESULT);
					try {
						OnlineMessage msgs[] = OnlineMessage.fromJSON(value);
						if (msgs != null && msgs.length > 0) for (OnlineMessage mesg: msgs) {
							switch (mesg.getAction()) {
							case ACKGET:
								LLog.e(Globals.TAG, CLASSTAG + ".handleMessage: Message Acknowledge but someting has to be handled here: ACKGET!!!!");
								break;
							case ACK:
								OnlinePacket packet = mesg.getPacket();
								if (packet != null && packet instanceof OnlineAckPacket){
									OnlineAckPacket ack = (OnlineAckPacket) packet;
									Long someTime = ack.getSessionTime();						
									if (ack.getSessionId() != null && sessionId == -1 && someTime != null && someTime == (long) 1000 * sessionPacket.getSecondTime()) {
										sessionId = ack.getSessionId();
										LLog.d(Globals.TAG, CLASSTAG + ".handleMessage ACK sessionId = " + sessionId + ", " + someTime);
									}
									if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + ".handleMessage ACK with AckPacket: " + mesg.toString());
								} else
									LLog.d(Globals.TAG, CLASSTAG + ".handleMessage ACK without AckPacket: " + mesg.toString());
								break;	
							default:
								LLog.d(Globals.TAG, CLASSTAG + ".handleMessage: " + mesg.toString());
							}


						}	
					} catch (JSONException e) {
						LLog.e(Globals.TAG, CLASSTAG + ".handleMessage: JSON exception in: " + value);
					} catch (NumberFormatException e) {
						LLog.e(Globals.TAG, CLASSTAG + ".handleMessage: NumberFormat exception in: " + value);
					} catch (OnlineMessage.OnlineDecodeException e) {
						LLog.e(Globals.TAG, CLASSTAG + ".handleMessage: OnlineDecodeException exception in: " + value);
					}
				} else if (key.equals(HttpSender.MSG_WARN)) {
					String value = bundle.getString(HttpSender.MSG_WARN);
					LLog.e(Globals.TAG, CLASSTAG + ".handleMessage: " + value);
					//ToastHelper.showToastLong(value);
				} 
			}

		}
	};

}
