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
using System;
namespace org.chabu{

/**
 * Value object to represent the information exchanged on connection setup. 
 * These are passed to {@link ChabuConnectingValidator}. 
 * 
 * @author Frank Benoit
 */
public sealed class ChabuSetupInfo {
	
	public readonly int     maxReceiveSize;
	public readonly int     applicationVersion;
    public readonly String applicationName;

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

}
}