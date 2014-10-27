/*******************************************************************************
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 * 
 *******************************************************************************/
package com.plusot.senselib.ant;


import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.share.SimpleLogType;
import com.plusot.common.util.ElapsedTime;
import com.plusot.javacommon.util.Format;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.values.ValueType;

public class SuuntoHrmDevice extends AntDevice {
	private static final String CLASSTAG = SuuntoHrmDevice.class.getSimpleName();

	private int bpm = Integer.MAX_VALUE;
	private double avgPlus = 0;
	private double avgMin = 0;
	private int prevBeat = 0;
	private int count = 0;
	private final boolean suuntoTesting = false;

	public SuuntoHrmDevice(AntMesgCallback callback) {
		super(AntProfile.SUUNTODUAL, callback);
	}
//	public SuuntoHrmDevice(final AntManager mgr, Context context, final int sub) {
//		super(mgr, DeviceType.SUUNTOHEARTRATEMONITOR, AntDeviceProfile.SUUNTODUAL, sub, EnumSet.of(ValueType.HEARTRATE, ValueType.PULSEWIDTH));
//		//prepareLog(ValueType.HEARTBEATS);
//	}

	@Override
	public boolean decode(byte[] antMsg) {
		//LLog.i(Globals.TAG, CLASSTAG + ".decode for " + getName() + ": " + StringUtil.toHexString(antMsg));
		if (!super.decode(antMsg)) return false;

		int mesgId = 0xFF & antMsg[PAGE_OFFSET];
		long now = System.currentTimeMillis();

		//LLog.i(Globals.TAG, CLASSTAG + ".decode for " + getName() + ": id = " + mesgId + ", " + StringUtil.toHexString(antMsg));
		if (mesgId == 1 || (mesgId >= 30 && mesgId <= 240)) {
			int beat = 0xFF & antMsg[PAGE_OFFSET + 1];
			if (beat == prevBeat) return true;
			count += (0xFF + beat - prevBeat) % 0xFF;
			prevBeat = beat;
			int time1 = (0xFF & antMsg[PAGE_OFFSET + 2]) + ((0xFF & antMsg[PAGE_OFFSET + 3]) << 8);
			int time2 = (0xFF & antMsg[PAGE_OFFSET + 4]) + ((0xFF & antMsg[PAGE_OFFSET + 5]) << 8);
			int time3 = (0xFF & antMsg[PAGE_OFFSET + 6]) + ((0xFF & antMsg[PAGE_OFFSET + 7]) << 8);
			int delta1 = (0xffff + time1 - time2) % 0xffff;
			int delta2 = (0xffff + time2 - time3) % 0xffff;
			if (delta1 < 2000) fireOnDeviceValueChanged(ValueType.PULSEWIDTH, delta1, now);
			if (delta1 > delta2) {
				avgPlus = 0.9 * avgPlus + 0.1 * delta1;
				avgMin = 0.9 * avgMin + 0.1 * delta2;
			} else {
				avgPlus = 0.9 * avgPlus + 0.1 * delta2;
				avgMin = 0.9 * avgMin + 0.1 * delta1;
			}
			int delta3 = (0xffff + time1 - time3) % 0xffff;
			bpm = (int) (120000.0 / delta3);
			if (suuntoTesting) {
				SimpleLog log = SimpleLog.getInstance(SimpleLogType.HRV, "suunto");
				if (log != null) {
					//				if (delta1 > delta2) {
					log.log(count + ";" + TimeUtil.formatMilli(now) + ";" + now + ";" + delta1 + ";" + delta2 + ";" + Format.format(1.0 * delta1/delta2, 3) + ";" + bpm);
					LLog.i(Globals.TAG, CLASSTAG + ".decode: " + count + ";" + TimeUtil.formatMilli(now) + ";" + now + ";" + delta1 + ";" + delta2 + ";" + Format.format(1.0 * delta1/delta2, 2) + ";" + bpm + "-" + mesgId);
					//				} else {
					//					log.log(count + ";" + TimeUtil.formatMilli(now) + ";" + now + ";" + delta2 + ";" + delta1 + ";" + Format.format(1.0 * delta2/delta1, 2) + ";" + bpm);
					//					LLog.i(Globals.TAG, CLASSTAG + ".decode: " + count + ";" + TimeUtil.formatMilli(now) + ";" + now + ";" + delta2 + ";" + delta1 + ";" + Format.format(1.0 * delta2/delta1, 2) + ";" + bpm + "-" + mesgId);
					//				}
				}
			}
			//			LLog.i(Globals.TAG, CLASSTAG + ".decode: " 
			//					+ "bpm = " + bpm
			//					+ ", variance = " + Format.format(100.0 * (1.0 * avgPlus / avgMin - 1.0), 1) + "% (" + (int) avgPlus + ", " + (int)avgMin + ")"
			//					);
			if (delta1 < 2000 && delta3 < 4000) fireOnDeviceValueChanged(ValueType.HEARTRATE, bpm, now, true);
			rawValue = "" + bpm + " bpm";
		} else if (mesgId == 2) {
			int volts = (0xFF & antMsg[PAGE_OFFSET + 1] + ((0xFF & antMsg[PAGE_OFFSET + 2]) << 8));
			battery = Format.format(0.001 * volts, 2);
			operatingTime = 60 * (0xFF & antMsg[PAGE_OFFSET + 3]+ ((0xFF & antMsg[PAGE_OFFSET + 4]) << 8));
			manufacturer = DeviceManufacturer.SUUNTO;
			revision = "" + (0xFF & antMsg[PAGE_OFFSET + 5]+ ((0xFF & antMsg[PAGE_OFFSET + 6]) << 8));
			if (Globals.testing.isVerbose()) 
				LLog.i(Globals.TAG, CLASSTAG + ".decode: voltage = " + battery + ", operating time: " + ElapsedTime.format(operatingTime * 1000, true, false) );
		} else if (mesgId == 0xFF) {
			LLog.i(Globals.TAG, CLASSTAG + ".decode PAIRING msg: " + StringUtil.toHexString(antMsg));
		} else {
			LLog.i(Globals.TAG, CLASSTAG + ".decode for: id = " + mesgId + ", " + StringUtil.toHexString(antMsg));
		}
		return true;
	}

	@Override
	public void fadeOut(ValueType types) {
	}

}
