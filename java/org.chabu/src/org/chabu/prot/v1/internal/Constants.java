/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <fr@nk-benoit.de>
 *
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 *
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu.prot.v1.internal;

public interface Constants {
	
	static final String PROTOCOL_NAME         = "CHABU";
	
	static final int    PROTOCOL_VERSION      = 0x0001_0000 + 1;

	static final int    MAX_RECV_LIMIT_HIGH   = 0x1000_0000;
	static final int    MAX_RECV_LIMIT_LOW    = 0x0000_0100;
	
	static final int    APV_MAX_LENGTH        = 56;
	static final int    ABORT_MSG_MAX_LENGTH  = 56;

}
