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

public enum AntMesgLookup {
	MESG_INVALID_ID                      ((byte)0x00),
	MESG_EVENT_ID                        ((byte)0x01),

	MESG_VERSION_ID                      ((byte)0x3E),
	MESG_RESPONSE_EVENT_ID               ((byte)0x40),

	MESG_UNASSIGN_CHANNEL_ID             ((byte)0x41),
	MESG_ASSIGN_CHANNEL_ID               ((byte)0x42),
	MESG_CHANNEL_MESG_PERIOD_ID          ((byte)0x43),
	MESG_CHANNEL_SEARCH_TIMEOUT_ID       ((byte)0x44),
	MESG_CHANNEL_RADIO_FREQ_ID           ((byte)0x45),
	MESG_NETWORK_KEY_ID                  ((byte)0x46),
	MESG_RADIO_TX_POWER_ID               ((byte)0x47),
	MESG_RADIO_CW_MODE_ID                ((byte)0x48),
	
	MESG_SYSTEM_RESET_ID                 ((byte)0x4A),
	MESG_OPEN_CHANNEL_ID                 ((byte)0x4B),
	MESG_CLOSE_CHANNEL_ID                ((byte)0x4C),
	MESG_REQUEST_ID                      ((byte)0x4D),

	MESG_BROADCAST_DATA_ID               ((byte)0x4E),
	MESG_ACKNOWLEDGED_DATA_ID            ((byte)0x4F),
	MESG_BURST_DATA_ID                   ((byte)0x50),

	MESG_CHANNEL_ID_ID                   ((byte)0x51),
	MESG_CHANNEL_STATUS_ID               ((byte)0x52),
	MESG_RADIO_CW_INIT_ID                ((byte)0x53),
	MESG_CAPABILITIES_ID                 ((byte)0x54),

	MESG_STACKLIMIT_ID                   ((byte)0x55),

	MESG_SCRIPT_DATA_ID                  ((byte)0x56),
	MESG_SCRIPT_CMD_ID                   ((byte)0x57),

	MESG_ID_LIST_ADD_ID                  ((byte)0x59),
	MESG_ID_LIST_CONFIG_ID               ((byte)0x5A),
	MESG_OPEN_RX_SCAN_ID                 ((byte)0x5B),

	MESG_EXT_CHANNEL_RADIO_FREQ_ID       ((byte)0x5C),  // OBSOLETE: (for 905 radio)
	MESG_EXT_BROADCAST_DATA_ID           ((byte)0x5D),
	MESG_EXT_ACKNOWLEDGED_DATA_ID        ((byte)0x5E),
	MESG_EXT_BURST_DATA_ID               ((byte)0x5F),

	MESG_CHANNEL_RADIO_TX_POWER_ID       ((byte)0x60),
	MESG_GET_SERIAL_NUM_ID               ((byte)0x61),
	MESG_GET_TEMP_CAL_ID                 ((byte)0x62),
	MESG_SET_LP_SEARCH_TIMEOUT_ID        ((byte)0x63),
	MESG_SET_TX_SEARCH_ON_NEXT_ID        ((byte)0x64),
	MESG_SERIAL_NUM_SET_CHANNEL_ID_ID    ((byte)0x65),
	MESG_RX_EXT_MESGS_ENABLE_ID          ((byte)0x66), 
	MESG_RADIO_CONFIG_ALWAYS_ID          ((byte)0x67),
	MESG_ENABLE_LED_FLASH_ID             ((byte)0x68),
	
	MESG_XTAL_ENABLE_ID                  ((byte)0x6D),
	
	MESG_STARTUP_MESG_ID                 ((byte)0x6F),
	MESG_AUTO_FREQ_CONFIG_ID             ((byte)0x70),
	MESG_PROX_SEARCH_CONFIG_ID           ((byte)0x71),
	MESG_EVENT_BUFFERING_CONFIG_ID       ((byte)0x74),

	
	MESG_CUBE_CMD_ID                     ((byte)0x80),

	MESG_GET_PIN_DIODE_CONTROL_ID        ((byte)0x8D),
	MESG_PIN_DIODE_CONTROL_ID            ((byte)0x8E),
	MESG_FIT1_SET_AGC_ID                 ((byte)0x8F),

	MESG_FIT1_SET_EQUIP_STATE_ID         ((byte)0x91),
	MESG_UNKNOWN						 ((byte)0xFF);	
	
	private final byte value;
	
	private AntMesgLookup(final byte value) {
		this.value = value;
	}
	
	public static AntMesgLookup fromByte(byte value) {
		for (AntMesgLookup msg : AntMesgLookup.values()) {
			if (msg.value == value) return msg;
		}
		return AntMesgLookup.MESG_UNKNOWN;
	}
}
