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
package org.chabu.prot.v1;

import org.chabu.prot.v1.internal.Constants;

/**
 * Value object to represent the information exchanged on connection setup. 
 * These are passed to {@link ChabuConnectingValidator}. 
 * 
 * @author Frank Benoit
 */
public final class ChabuSetupInfo {
	
	public final int     recvPacketSize;
	public final int     applicationVersion;
	public final String  applicationProtocolName;

	public ChabuSetupInfo(){
		recvPacketSize = Constants.MAX_RECV_LIMIT_LOW;
		applicationVersion = 0;
		applicationProtocolName = "";
	}
	public ChabuSetupInfo( int recvPacketSize, int applicationVersion, String applicationProtocolName ){
		this.recvPacketSize = recvPacketSize;
		this.applicationProtocolName = applicationProtocolName;
		this.applicationVersion = applicationVersion;
	}

	public ChabuSetupInfo( ChabuSetupInfo other ){
		this.recvPacketSize = other.recvPacketSize;
		this.applicationProtocolName = other.applicationProtocolName;
		this.applicationVersion = other.applicationVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((applicationProtocolName == null) ? 0 : applicationProtocolName.hashCode());
		result = prime * result + applicationVersion;
		result = prime * result + recvPacketSize;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		ChabuSetupInfo other = (ChabuSetupInfo) obj;
		
		if (applicationProtocolName == null) {
			if (other.applicationProtocolName != null)
				return false;
		} 
		else if (!applicationProtocolName.equals(other.applicationProtocolName))
			return false;
		
		if (applicationVersion != other.applicationVersion)
			return false;
		if (recvPacketSize != other.recvPacketSize)
			return false;
		return true;
	}

}
