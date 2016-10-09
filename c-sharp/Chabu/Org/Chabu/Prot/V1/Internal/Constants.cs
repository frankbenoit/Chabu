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

namespace Org.Chabu.Prot.V1.Internal
{

    public static class Constants {

        static readonly string PROTOCOL_NAME = "CHABU";

        static readonly int PROTOCOL_VERSION = 0x00010000 + 1;

	    static readonly int MAX_RECV_LIMIT_HIGH = 0x10000000;
	    static readonly int MAX_RECV_LIMIT_LOW = 0x00000100;
	
	    static readonly int APV_MAX_LENGTH = 56;
        static readonly int ABORT_MSG_MAX_LENGTH = 56;

    }
}
