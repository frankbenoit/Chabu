/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <keinfarbton@gmail.com>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu.prot.v1;

/**
 * Value object to represent the information exchanged on connection setup. 
 * These are passed to {@link ChabuConnectingValidator}. 
 * 
 * @author Frank Benoit
 */
public final class ChabuSetupInfo {
	
	public final int     maxReceiveSize;
	public final int     applicationVersion;
	public final String  applicationName;

	public ChabuSetupInfo( int maxReceiveSize, int applicationVersion, String applicationName ){
		this.maxReceiveSize = maxReceiveSize;
		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
	}

	public ChabuSetupInfo( ChabuSetupInfo other ){
		this.maxReceiveSize = other.maxReceiveSize;
		this.applicationName = other.applicationName;
		this.applicationVersion = other.applicationVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((applicationName == null) ? 0 : applicationName.hashCode());
		result = prime * result + applicationVersion;
		result = prime * result + maxReceiveSize;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		ChabuSetupInfo other = (ChabuSetupInfo) obj;
		
		if (applicationName == null) {
			if (other.applicationName != null)
				return false;
		} 
		else if (!applicationName.equals(other.applicationName))
			return false;
		
		if (applicationVersion != other.applicationVersion)
			return false;
		if (maxReceiveSize != other.maxReceiveSize)
			return false;
		return true;
	}

}
