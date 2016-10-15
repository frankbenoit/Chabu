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
using Org.Chabu.Prot.V1.Internal;
using System;

namespace Org.Chabu.Prot.V1
{

    /**
     * Value object to represent the information exchanged on connection setup. 
     * These are passed to {@link ChabuConnectingValidator}. 
     * 
     * @author Frank Benoit
     */
    public sealed class ChabuSetupInfo {
	
	    public readonly int     recvPacketSize;
	    public readonly int     applicationVersion;
        public readonly String applicationProtocolName;

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

        public override bool Equals(object o)
        {
            if (o == null) return false;
            if (o.GetType() != typeof(ChabuSetupInfo)) return false;
            ChabuSetupInfo c = (ChabuSetupInfo)o;
            if (recvPacketSize != c.recvPacketSize) return false;
            if (applicationVersion != c.applicationVersion) return false;
            if (applicationProtocolName == null) return false;
            if (applicationProtocolName != c.applicationProtocolName) return false;
            return true;
        }

        public override int GetHashCode()
        {
            return new {
                recvPacketSize,
                applicationVersion,
                applicationProtocolName
            }.GetHashCode();
        }
    }
}