package com.plusot.senselib.ant;

import com.dsi.ant.AntDefine;
import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.javacommon.util.Format;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.store.SRMLog;
import com.plusot.senselib.values.Device;
import com.plusot.senselib.values.ValueType;

public class AntPowerDevice extends AntDevice  {

	private static final String CLASSTAG = AntPowerDevice.class.getSimpleName();

	private static final int CALIBRATION_PAGE = 0x01;
	private static final int POWER_PAGE = 0x10;
	private static final int WHEELTORQUE_PAGE = 0x11;
	private static final int CRANKTORQUE_PAGE = 0x12;
	private static final int CRANKFREQ_PAGE = 0x20;

	private static final byte CALIBRATIONREQUEST_MANUALZERO = (byte) 0xAA;
	private static final byte CALIBRATIONREQUEST_AUTOZERO = (byte) 0xAB;
	private static final byte CALIBRATIONRESPONSE_SUCCESSFULL = (byte) 0xAC;
	private static final byte CALIBRATIONREQUEST_FAILED = (byte) 0xAD;
	private static final byte CALIBRATIONRESPONSE_FAILED = (byte) 0xAF;
	private static final byte CALIBRATION_CRANKTORQUEFREQ_DEFINEDMESSAGE = (byte) 0x10; // CTF message
	private static final byte CALIBRATION_TORQUEMETERCAPABILITIES = (byte) 0x12;

	private static final byte CTF_OFFSET = (byte) 0x1;				// Values 0 - 65535
	private static final byte CTF_SLOPE = (byte) 0x2;				// Values 100 - 500
	private static final byte CTF_SERIALNUMBER = (byte) 0x3;		// Values 0 - 65535
	private static final byte CTF_ACKNOWLEDGEMENT = (byte) 0xAC;	// Values N/A

	private enum CalibrationRequest {
		MANUALZERO(CALIBRATIONREQUEST_MANUALZERO),
		AUTOZERO(CALIBRATIONREQUEST_AUTOZERO);

		private final byte requestId;

		private CalibrationRequest(final byte requestId) {
			this.requestId = requestId;
		}

		public byte toByte() {
			return requestId;
		}
	}

	private int powerAccum = Integer.MIN_VALUE;
	private int powerEventCount = Integer.MIN_VALUE;
	private int wheelEventCount = Integer.MIN_VALUE;
	//	private int wheelTicks = 0;
	private int wheelPeriodInt = Integer.MIN_VALUE;
	private int wheelTorqueAccum = Integer.MIN_VALUE;
	private int crankEventCount = Integer.MIN_VALUE;
	private int cadenceInt = 0;
	private int crankPeriodInt = Integer.MIN_VALUE;
	private int crankTorqueAccum = Integer.MIN_VALUE;

	private int freqEventCount = 0;
	private int freqSlope = 0;
	private int freqTimeStamp = 0;
	private int freqTorqueTicks = 0;
	private double torqueFreqOffset = 0;

	private double crankPeriod = Double.NaN;
	private double wheelTorque = Double.NaN;
	private double wheelPeriod = Double.NaN;
	private double wheelRevs = Double.NaN;
	private double pedalPower = Double.NaN;
	private double cadence = Double.NaN;
	private double power = Double.NaN;
	private double crankTorque = Double.NaN;
	private boolean autoZeroSupported = false;
	private boolean calibrationInProces = false;
	private long calibrationStartTime = 0;
	private long lastCall = 0;
	private boolean debug = false;

	private enum AutoZeroStatus {
		CALIBRATION_AUTOZERO_OFF((byte) 0x0, "Off"),
		CALIBRATION_AUTOZERO_ON((byte) 0x1, "On"),
		CALIBRATION_AUTOZERO_NOTSUPPORTED((byte) 0xFF, "Not supported"),
		CALIBRATION_AUTOZERO_UNKNOWN((byte) 0x2, "Unknown");

		private final String label;
		private final byte id;

		private AutoZeroStatus(final byte id, final String label) {
			this.id = id;
			this.label = label;
		}

		public byte toByte() {
			return id;
		}

		@Override
		public String toString() {
			return label;
		}

		public static AutoZeroStatus fromId(byte id) {
			switch (id) {
			case 0x0: return AutoZeroStatus.CALIBRATION_AUTOZERO_OFF;
			case 0x1: return AutoZeroStatus.CALIBRATION_AUTOZERO_ON;
			case (byte)0xFF: return AutoZeroStatus.CALIBRATION_AUTOZERO_NOTSUPPORTED;
			}
			LLog.d(Globals.TAG, CLASSTAG + ".AutoZeroStatus.fromId: id = " + id);
			return AutoZeroStatus.CALIBRATION_AUTOZERO_UNKNOWN;
		}

	}

	public AntPowerDevice(AntMesgCallback callback) {
		super(AntProfile.BIKEPOWER, callback);
		torqueFreqOffset = PreferenceKey.CRANKFREQOFFSET.getInt();
	}

	private void reset() {
		powerAccum = Integer.MIN_VALUE;
		powerEventCount = Integer.MIN_VALUE;
		wheelEventCount = Integer.MIN_VALUE;
		wheelTorqueAccum = Integer.MIN_VALUE;
		wheelPeriodInt = Integer.MIN_VALUE;	
		crankEventCount = Integer.MIN_VALUE;
		crankTorqueAccum = Integer.MIN_VALUE;
		crankPeriodInt = Integer.MIN_VALUE;	
	}
	
	private double prevPower = 0;
	private long prevPowerTime = 0;
	
	protected void fireOnDeviceValueChanged(ValueType valueType, Object value, long timeStamp) {
		if (valueType.equals(ValueType.POWER) && value instanceof Double) {
			double power = (Double) value;
			if (prevPower > 0 && prevPowerTime > 0 && prevPowerTime - timeStamp < 5000)
				fireOnDeviceValueChanged(ValueType.ENERGY, (Double)((power + prevPower) * (timeStamp - prevPowerTime) / 2000.0), timeStamp, true);
			prevPower = power;
			prevPowerTime = timeStamp;
		}
		fireOnDeviceValueChanged(valueType, value, timeStamp, true);
	}

	@Override
	public boolean decode(byte[] antMsg) {
		if (!super.decode(antMsg)) return false;

		long now = System.currentTimeMillis();
		if (now - lastCall > ANT_MSG_TIMEOUT) reset();
		lastCall = now;

		switch (antMsg[PAGE_OFFSET]) {
		case CALIBRATION_PAGE: 
			//LLog.i(Globals.TAG, CLASSTAG + ": Calibration page"); 
			byte calibrationId = antMsg[PAGE_OFFSET + 1];
			switch (calibrationId) {
			case CALIBRATIONREQUEST_MANUALZERO:
				LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Manual Zero request"); 
				break;
			case CALIBRATIONREQUEST_AUTOZERO: 
				byte autoZeroStatusByte = antMsg[PAGE_OFFSET + 2];
				LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: AutoZeroStatus = " + AutoZeroStatus.fromId(autoZeroStatusByte)); 
				break;
			case CALIBRATIONRESPONSE_SUCCESSFULL:
				autoZeroStatusByte = antMsg[PAGE_OFFSET + 2];
				int calibrationData = (0xFF & antMsg[PAGE_OFFSET + 6]) + ((0x7F & antMsg[PAGE_OFFSET + 7]) << 8);
				if (((byte) 0x80 & antMsg[PAGE_OFFSET + 7]) == (byte) 0x80) calibrationData = -calibrationData;
				LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Response successful (AutoZero = " + AutoZeroStatus.fromId(autoZeroStatusByte) + ", Data = " + calibrationData + ")"); 
				if (calibrationInProces)
					ToastHelper.showToastShort(Globals.appContext.getString(R.string.calibration_success, AutoZeroStatus.fromId(autoZeroStatusByte).toString(), calibrationData));
				calibrationInProces = false;
				break;
			case CALIBRATIONRESPONSE_FAILED:
				autoZeroStatusByte = antMsg[PAGE_OFFSET + 2];
				calibrationData = (0xFF & antMsg[PAGE_OFFSET + 6]) + ((0x7F & antMsg[PAGE_OFFSET + 7]) << 8);
				if (((byte) 0x80 & antMsg[PAGE_OFFSET + 7]) == (byte) 0x80) calibrationData = -calibrationData;
				LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Response failed (AutoZeroStatus = " + AutoZeroStatus.fromId(autoZeroStatusByte) + ", Data = " + calibrationData + ")");
				if (now - calibrationStartTime < 10000)
					ToastHelper.showToastShort(Globals.appContext.getString(R.string.calibration_failed, AutoZeroStatus.fromId(autoZeroStatusByte).toString(), calibrationData));
				break;
			case CALIBRATIONREQUEST_FAILED:
				LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Calibration request failed"); 
				break;
			case CALIBRATION_CRANKTORQUEFREQ_DEFINEDMESSAGE:
				switch (antMsg[PAGE_OFFSET + 2]) {
				case CTF_OFFSET:
					int offset = ((0xFF & antMsg[PAGE_OFFSET + 6]) << 8) + (0xFF & antMsg[PAGE_OFFSET + 7]);
					LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Crank torque frequency message with offset = " + offset); 
					if (calibrationInProces)  {
						if (torqueFreqOffset != 1.0 * offset) {
							torqueFreqOffset = 1.0 * offset;
							PreferenceKey.CRANKFREQOFFSET.set(offset);
						}
						//if (SRMLog.getInstance() != null) SRMLog.getInstance().setOffset(now, torqueFreqOffset);
						ToastHelper.showToastShort(Globals.appContext.getString(R.string.torque_freq_offset, offset));
					}
					calibrationInProces = false;
					break;
				case CTF_SLOPE:
				case CTF_SERIALNUMBER:
				case CTF_ACKNOWLEDGEMENT:
					switch (antMsg[PAGE_OFFSET + 3]) {
					case CTF_SLOPE:
						LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Crank torque frequency acknowledgement of setting slope."); 
						break;
					case CTF_SERIALNUMBER:
						LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Crank torque frequency acknowledgement of setting serial number."); 
						break;
					default:
						LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Crank torque frequency unknown acknowledgement."); 
						break;
					}
					break;
				default:
					LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Unknowen Crank torque frequency message: " + antMsg[PAGE_OFFSET + 2]); 
				}
				break;
			case CALIBRATION_TORQUEMETERCAPABILITIES:
				byte zeroConfig = antMsg[PAGE_OFFSET + 2];
				autoZeroSupported = false;
				AutoZeroStatus autoZeroStatus = AutoZeroStatus.CALIBRATION_AUTOZERO_OFF;
				if (((byte) 0x1 & zeroConfig) == (byte) 0x1) autoZeroSupported = true;
				if (((byte) 0x2 & zeroConfig) == (byte) 0x2) autoZeroStatus = AutoZeroStatus.CALIBRATION_AUTOZERO_ON;
				LLog.i(Globals.TAG, CLASSTAG + ".decode calibration: Torque meter capabilities (AutoZeroStatus = " + autoZeroStatus + ", AutoZeroEnable = " + autoZeroSupported + ")"); 

				break;
			}
			break;
		case POWER_PAGE: 
			//LLog.d(Globals.TAG, CLASSTAG + ": Power Page"); 
			int eventCount = 0xFF & antMsg[PAGE_OFFSET + 1];
			cadenceInt = (0xFF & antMsg[PAGE_OFFSET + 3]);
			if (debug) LLog.i(Globals.TAG, CLASSTAG + ".decode: Power Page:" +
					" Event: " + eventCount); 	
			
			
			if ( antMsg[PAGE_OFFSET + 2]  != 0xFF) {
				int iPedalPower = (0x7F & antMsg[PAGE_OFFSET + 2]);
				int rightPedalPart = (0xFF & antMsg[PAGE_OFFSET + 2]) >> 7;
					//LLog.d(Globals.TAG, "Pedal power: " + pedalPower + " " + rightPedalPart);
					if (rightPedalPart == 0) 
						pedalPower = 0.01 * iPedalPower;
					else
						pedalPower = 0.01 * (100.0 - iPedalPower);
					fireOnDeviceValueChanged(ValueType.POWER_PEDAL, pedalPower, now);
			}
			if (cadenceInt >= 0 && cadenceInt != 0xFF) {
				cadence = 1.0 * cadenceInt;
				fireOnDeviceValueChanged(ValueType.CADENCE, cadence, now);
				//LLog.d(Globals.TAG, CLASSTAG + ": Power page cadence = " + cadence);
			}
			int iPower = (0xFF & antMsg[PAGE_OFFSET + 4]) + ((0xFF & antMsg[PAGE_OFFSET + 5]) << 8);
			if (powerEventCount != Integer.MIN_VALUE && powerAccum != Integer.MIN_VALUE) {
				if (eventCount < powerEventCount) powerEventCount -= 256; 

				if (eventCount - powerEventCount > 1) {
					if (iPower < powerAccum) powerAccum -= 65536;
					if (powerAccum > 0) {
						power = 1.0 * (iPower - powerAccum) / (eventCount - powerEventCount);
						if (power != Double.NaN) fireOnDeviceValueChanged(ValueType.POWER, power, now);
						if (debug) LLog.i(Globals.TAG, CLASSTAG + ".decode: Power Page - power = " + power + " W"); 			
					}
				} else {
					int instantPower = (0xFF & antMsg[PAGE_OFFSET + 6]) + ((0xFF & antMsg[PAGE_OFFSET + 7]) << 8);
					power = 1.0 * instantPower;
					
					if (power != Double.NaN) fireOnDeviceValueChanged(ValueType.POWER, power, now);
					if (debug) LLog.i(Globals.TAG, CLASSTAG + ".decode: Power Page - instant power = " + power + " W"); 
					rawValue = "" + Format.format(power, 2) + " W";

				}
			}
			powerAccum = iPower;
			powerEventCount = eventCount;
			break;
		case WHEELTORQUE_PAGE: 
			//LLog.d(Globals.TAG, CLASSTAG + ": Wheel Torque page"); 
			eventCount = 0xFF & antMsg[PAGE_OFFSET + 1];
			cadenceInt = (0xFF & antMsg[PAGE_OFFSET + 3]);
			int period = (0xFF & antMsg[PAGE_OFFSET + 4]) + ((0xFF & antMsg[PAGE_OFFSET + 5]) << 8);
			int torqueInt = (0xFF & antMsg[PAGE_OFFSET + 6]) + ((0xFF & antMsg[PAGE_OFFSET + 7]) << 8);
			int ticks = (0xFF & antMsg[PAGE_OFFSET + 2]);
			if (debug) LLog.i(Globals.TAG, CLASSTAG + ".decode: Wheel Torque Page: Event: " + eventCount + 
			//		" Cadence: " + cadenceInt +
					" Period: " + period +
					" Ticks: " + ticks +
					" Torque: " + torqueInt); 	
			//			
			//			if (ticks < wheelTicks) wheelTicks -= 256;
			//			LLog.d(Globals.TAG, CLASSTAG + ": WheelTicks = " + (ticks - wheelTicks) + " Ticks " + (eventCount - wheelEventCount));


			if (cadenceInt >= 0 && cadenceInt != 0xFF) {
				cadence = 1.0 * cadenceInt;
				fireOnDeviceValueChanged(ValueType.CADENCE, cadence, now);
				//LLog.d(Globals.TAG, CLASSTAG + ": Wheel Torque page cadence = " + cadence); 

			}

			if (wheelPeriodInt != Integer.MIN_VALUE  && wheelEventCount!= Integer.MIN_VALUE && wheelTorqueAccum != Integer.MIN_VALUE) {
				if (period < wheelPeriodInt) wheelPeriodInt -= 65536;
				if (eventCount < wheelEventCount) wheelEventCount -= 256;
				if (wheelPeriodInt > 0 && eventCount - wheelEventCount > 0) {
					double wp =  1.0 * (period - wheelPeriodInt) / (2048.0 * (eventCount - wheelEventCount));
					//LLog.d(Globals.TAG, CLASSTAG + ": Wheel period = " + wp);
					wheelPeriod = 1000.0 * wp;
//					fireOnDeviceValueChanged(ValueType.WHEEL_PERIOD, wheelPeriod, now);
					if (wp > 0) {
						wheelRevs = 60.0 / wp;
						fireOnDeviceValueChanged(ValueType.WHEEL_REVS, wheelRevs, now);
						fireOnDeviceValueChanged(ValueType.SPEED, wheelRevs * 0.001 * SenseGlobals.wheelCirc / 60.0, now);
					} else {
						wheelRevs = 0;
						fireOnDeviceValueChanged(ValueType.WHEEL_REVS, wheelRevs, now);
						fireOnDeviceValueChanged(ValueType.SPEED, wheelRevs * 0.001 * SenseGlobals.wheelCirc / 60.0, now);
					}
				}
				if (torqueInt < wheelTorqueAccum) wheelTorqueAccum -= 65536;
				if (wheelTorqueAccum > 0 && eventCount - wheelEventCount > 0) {
					wheelTorque = 1.0 * (torqueInt - wheelTorqueAccum) / (32.0 * (eventCount - wheelEventCount));
					fireOnDeviceValueChanged(ValueType.WHEEL_TORQUE, wheelTorque, now);
					if (wheelPeriodInt > 0 && period - wheelPeriodInt > 0) {
						power = 128.0 * Math.PI * (torqueInt - wheelTorqueAccum) / (period - wheelPeriodInt);
						rawValue = "" + Format.format(power, 2) + " W";
						
						if (power != Double.NaN) fireOnDeviceValueChanged(ValueType.POWER, power, now);
						if (debug) LLog.d(Globals.TAG, CLASSTAG + ".decode: Wheel Torque Page - power = " + power + " W, torque = " + wheelTorque + " Nm"); 	
					} else {
						power = 0;
						rawValue = "" + Format.format(power, 2) + " W";
						if (power != Double.NaN) fireOnDeviceValueChanged(ValueType.POWER, power, now);
						if (debug) LLog.d(Globals.TAG, CLASSTAG + ".decode: Wheel Torque Page - power = " + power + " W, torque = " + wheelTorque + " Nm"); 	
					}
						
				}
			}
			wheelEventCount = eventCount;
			wheelTorqueAccum = torqueInt;
			wheelPeriodInt = period;
			break;

		case CRANKTORQUE_PAGE: 
			eventCount = 0xFF & antMsg[PAGE_OFFSET + 1];
			period = (0xFF & antMsg[PAGE_OFFSET + 4]) + ((0xFF & antMsg[PAGE_OFFSET + 5]) << 8);
			torqueInt = (0xFF & antMsg[PAGE_OFFSET + 6]) + ((0xFF & antMsg[PAGE_OFFSET + 7]) << 8);
			ticks = (0xFF & antMsg[PAGE_OFFSET + 2]);
			cadenceInt = (0xFF & antMsg[PAGE_OFFSET + 3]);
			if (debug) LLog.i(Globals.TAG, CLASSTAG + ".decode: Crank Torque Page: Event: " + eventCount + 
					" Cadence: " + cadenceInt +
					" Period: " + period +
					" Ticks: " + ticks +
					" Torque: " + torqueInt); 	
			
		
			//			if (ticks < crankTicks) crankTicks -= 256;
			//			LLog.d(Globals.TAG, CLASSTAG + ": Crank Torque Page with CrankTicks = " + (ticks - crankTicks) + " Ticks = " + (eventCount - crankEventCount));

			if (cadenceInt >= 0 && cadenceInt != 0xFF) {
				cadence = 1.0 * cadenceInt;
				fireOnDeviceValueChanged(ValueType.CADENCE, cadence, now);
			}

			if (crankPeriodInt != Integer.MIN_VALUE  && crankEventCount!= Integer.MIN_VALUE && crankTorqueAccum != Integer.MIN_VALUE) {
				if (eventCount < crankEventCount) crankEventCount -= 256;
				if (period < crankPeriodInt) crankPeriodInt -= 65536;
				if (crankPeriodInt > 0 && eventCount - crankEventCount > 0) {
					crankPeriod =  1.0 * (period - crankPeriodInt) / (2048.0 * (eventCount - crankEventCount));
					//fireOnDeviceValueChanged(ValueType.CRANK_PERIOD, crankPeriod, now);
					fireOnDeviceValueChanged(ValueType.CADENCE, 60.0 / crankPeriod, now);
				}

				if (torqueInt < crankTorqueAccum) crankTorqueAccum -= 65536;
				if (crankTorqueAccum > 0 && eventCount - crankEventCount > 0) {
					//LLog.i(Globals.TAG, CLASSTAG + ": Crank Torque page"); 
					crankTorque = 1.0 * (torqueInt - crankTorqueAccum) / (32.0 * (eventCount - crankEventCount));
					fireOnDeviceValueChanged(ValueType.CRANK_TORQUE, crankTorque, now);
					if (crankPeriodInt > 0 && period - crankPeriodInt > 0) {
						power = 128.0 * Math.PI * (torqueInt - crankTorqueAccum) / (period - crankPeriodInt);
						
						if (power != Double.NaN) fireOnDeviceValueChanged(ValueType.POWER, power, now);

						if (debug) LLog.i(Globals.TAG, CLASSTAG + ".decode: Crank Torque Page - power = " + power + " W"); 
						rawValue = "" + Format.format(power, 2) + " W";

					}
				}
			}
			crankEventCount = eventCount;
			crankTorqueAccum = torqueInt;
			crankPeriodInt = period;
			break;

		case CRANKFREQ_PAGE: 
			//LLog.i(Globals.TAG, CLASSTAG + ": Crank Frequency page"); 
			//this page uses big endian byte order!!!
			eventCount = 0xFF & antMsg[PAGE_OFFSET + 1];
			int timeStamp = (0xFF & antMsg[PAGE_OFFSET + 5]) + ((0xFF & antMsg[PAGE_OFFSET + 4]) << 8);
			int torqueTicks = (0xFF & antMsg[PAGE_OFFSET + 7]) + ((0xFF & antMsg[PAGE_OFFSET + 6]) << 8);
			int slope = (0xFF & antMsg[PAGE_OFFSET + 3]) + ((0xFF & antMsg[PAGE_OFFSET + 2]) << 8);
			//LLog.d(Globals.TAG, CLASSTAG + ".decode crankfreq: Slope = " + 0.1 * slope + " Nm/Hz");
			if (debug) LLog.i(Globals.TAG, CLASSTAG + ".decode: Crank Frequency Page:" +
					" Event: " + eventCount + 
					" Slope: " + slope +
					" Timestamp: " + timeStamp +
					" Torque: " + torqueTicks); 	
			
			
			if (freqSlope != slope) {
				if (SRMLog.getInstance() != null) SRMLog.getInstance().setSlope(now, 0.1 * slope);
				freqSlope = slope;
			}

			if (freqEventCount != Integer.MIN_VALUE  && freqTimeStamp != Integer.MIN_VALUE && freqTorqueTicks != Integer.MIN_VALUE) {
				if (eventCount < freqEventCount) freqEventCount -= 256;
				if (timeStamp < freqTimeStamp) freqTimeStamp -= 65536;
				if (torqueTicks < freqTorqueTicks) freqTorqueTicks -= 65536;

				if (timeStamp - freqTimeStamp > 0) {
					cadence = 120000.0 * (eventCount - freqEventCount) / (timeStamp - freqTimeStamp);
					//LLog.d(Globals.TAG, CLASSTAG + ".decode crankfreq: Cadence = " + cadence + " rpm, Slope = " + 0.1 * slope + " Nm/Hz");
					fireOnDeviceValueChanged(ValueType.CADENCE, cadence, now);	
				}

				if (freqTorqueTicks > 0 && freqTimeStamp > 0 && timeStamp - freqTimeStamp > 0 && freqSlope > 0) {
					if (debug) LLog.i(Globals.TAG, CLASSTAG + ": Crank Frequency page"); 
					double elapsedTime = 0.0005 * (timeStamp - freqTimeStamp);
					int deltaTorqueTicks = torqueTicks - freqTorqueTicks;
					double torqueFreq = 1.0 * deltaTorqueTicks / elapsedTime - torqueFreqOffset;
					crankTorque = 10.0 * torqueFreq / freqSlope;
					fireOnDeviceValueChanged(ValueType.CRANK_TORQUE, crankTorque, now);
					power = crankTorque * cadence * Math.PI / 30.0;
					//LLog.d(Globals.TAG, CLASSTAG + ".decode crankfreq: Power = " + power + " W");
					if (debug) LLog.d(Globals.TAG, CLASSTAG + ".decode Crank Freq Page - power = " + power + " W" + // ", Torqueticks = " + deltaTorqueTicks + 
							", torque = " + crankTorque + " Nm, cadence = " + cadence + " rpm" +
							", slope = " + 0.1 * slope + " Nm/Hz, offset = " + torqueFreqOffset);

					
					if (power != Double.NaN) fireOnDeviceValueChanged(ValueType.POWER, power, now);	
					rawValue = "" + Format.format(power, 2) + " W";

				}
			}

			freqEventCount = eventCount;
			freqTimeStamp = timeStamp;
			freqTorqueTicks = torqueTicks;
			break;
		}
		return true;
	}

	@Override
	public void fadeOut(ValueType type) {
		long now = System.currentTimeMillis();
		switch (type) {
		case POWER_PEDAL: 
			if (pedalPower != Double.NaN && Math.abs(pedalPower - 0.5) > 0.01) {
				pedalPower = (pedalPower - 0.5) * Device.FADEOUT_FACTOR + 0.5;
				fireOnDeviceValueChanged(ValueType.POWER_PEDAL, pedalPower, now, false);
			}
			break;
		case POWER: 
			if (power != Double.NaN && power > 1.0) {
				power *= Device.FADEOUT_FACTOR;
				if (power < 1.0) power = 0;
				fireOnDeviceValueChanged(ValueType.POWER, power, now, false);
			}
			if (crankTorque != Double.NaN && crankTorque > 1.0) {
				crankTorque *= Device.FADEOUT_FACTOR;
				if (crankTorque < 1.0) crankTorque = 0;
				fireOnDeviceValueChanged(ValueType.CRANK_TORQUE, crankTorque, now, false);
			}
			if (crankPeriod != Double.NaN && crankPeriod < 300000) {
				crankPeriod /= Device.FADEOUT_FACTOR;
				if (crankPeriod > 300) crankTorque = 1000;
				//fireOnDeviceValueChanged(ValueType.CRANK_PERIOD, crankPeriod, now, false);
			}
			if (wheelTorque != Double.NaN && wheelTorque > 0.1) {
				wheelTorque *= Device.FADEOUT_FACTOR;
				if (wheelTorque < 1.0) wheelTorque = 0;
				fireOnDeviceValueChanged(ValueType.WHEEL_TORQUE, wheelTorque, now, false);
			}
			if (wheelPeriod != Double.NaN && wheelPeriod < 300000) {
				wheelPeriod /= Device.FADEOUT_FACTOR;
				if (wheelPeriod > 300) wheelPeriod = 1000;
				//fireOnDeviceValueChanged(ValueType.WHEEL_PERIOD, wheelPeriod, now, false);
			}		
			break;
		case CADENCE: 
			if (cadence != Double.NaN && cadence > 1.0) {
				cadence *= Device.FADEOUT_FACTOR;
				if (cadence < 1.0) cadence = 0;
				fireOnDeviceValueChanged(ValueType.CADENCE, cadence, now, false);
			}
			break;
		case WHEEL_REVS:
			if (wheelRevs != Double.NaN && wheelRevs > 1.0) {
				wheelRevs *= Device.FADEOUT_FACTOR;
				if (wheelRevs < 1.0) wheelRevs = 0;
				fireOnDeviceValueChanged(ValueType.WHEEL_REVS, wheelRevs, now, false);
				fireOnDeviceValueChanged(ValueType.SPEED, wheelRevs * 0.001 * SenseGlobals.wheelCirc / 60.0, now, false);
			}
			break;
		default:
			break;
		}
	}

	public void setCrankTorqueFrequencySlope(int slope) {
		byte[] txBuffer = new byte[AntDefine.ANT_STANDARD_DATA_PAYLOAD_SIZE];
		LLog.d(Globals.TAG, CLASSTAG + ".calibrationStart");

		txBuffer[0] = CALIBRATION_PAGE;
		txBuffer[1] = CALIBRATION_CRANKTORQUEFREQ_DEFINEDMESSAGE;
		txBuffer[2] = CTF_SLOPE;
		for (int i = 3; i < txBuffer.length; i++) txBuffer[i] = (byte) 0xFF; 
		txBuffer[6] = (byte) (slope >> 8);
		txBuffer[7] = (byte) (slope & 0xF);
		sendMessage(txBuffer);
	}

	public void setCrankTorqueFrequencySerialNumber(int serial) {
		byte[] txBuffer = new byte[AntDefine.ANT_STANDARD_DATA_PAYLOAD_SIZE];
		LLog.d(Globals.TAG, CLASSTAG + ".calibrationStart");

		txBuffer[0] = CALIBRATION_PAGE;
		txBuffer[1] = CALIBRATION_CRANKTORQUEFREQ_DEFINEDMESSAGE;
		txBuffer[2] = CTF_SERIALNUMBER;
		for (int i = 3; i < txBuffer.length; i++) txBuffer[i] = (byte) 0xFF; 
		txBuffer[6] = (byte) (serial >> 8);
		txBuffer[7] = (byte) (serial & 0xF);
		sendMessage(txBuffer);
	}

	private void calibrationStart(CalibrationRequest request) {
		byte[] txBuffer = new byte[AntDefine.ANT_STANDARD_DATA_PAYLOAD_SIZE];
		LLog.d(Globals.TAG, CLASSTAG + ".calibrationStart");
		calibrationInProces = true;
		calibrationStartTime = System.currentTimeMillis();
		txBuffer[0] = CALIBRATION_PAGE;
		txBuffer[1] = request.toByte();
		switch(request) {
		case AUTOZERO:
			txBuffer[2] = AutoZeroStatus.CALIBRATION_AUTOZERO_ON.toByte();
			break;
		case MANUALZERO:
			txBuffer[2] = AutoZeroStatus.CALIBRATION_AUTOZERO_NOTSUPPORTED.toByte();
			break;
		}		
		for (int i = 3; i < txBuffer.length; i++) txBuffer[i] = (byte) 0xFF; 
		sendMessage(txBuffer);
	}

	public void calibrate() {
		if (autoZeroSupported)
			calibrationStart(CalibrationRequest.AUTOZERO);
		else
			calibrationStart(CalibrationRequest.MANUALZERO);
	}

	/*@Override
	public void onWake() {
		calibrate();
	}*/


}


