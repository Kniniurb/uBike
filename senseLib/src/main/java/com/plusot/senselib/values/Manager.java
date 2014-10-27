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
package com.plusot.senselib.values;

import java.util.EnumMap;

public interface Manager {
	public static EnumMap<ManagerType, Manager> managers = new EnumMap<ManagerType, Manager>(ManagerType.class);
	public boolean init();
	public void destroy();
	public void onStart();
	public void onStop();
	public void onResume();
//	public void onPause();
//	public EnumSet<DeviceType> supportedDevices();

}
