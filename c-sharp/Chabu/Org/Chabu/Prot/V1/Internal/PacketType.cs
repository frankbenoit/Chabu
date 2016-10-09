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

    /**
     *
     * @author Frank Benoit
     */
    internal enum PacketType : int {
	    SETUP   = 0xF0 ,
	    ACCEPT  = 0xE1 ,
	    ABORT   = 0xD2 ,
	    ARM     = 0xC3 ,
	    SEQ     = 0xB4 ,
	    RST_REQ = 0xA5 ,
	    RST_ACK = 0x96 ,
	    DAVAIL  = 0x87 ,
	    NOP     = 0x78 ,
	    NONE    =   -1 ,
    }

    internal static class PacketTypeExtension
    {
        public static PacketType findPacketType(int id)
        {
            PacketType[] values = (PacketType[])global::System.Enum.GetValues(typeof(PacketType));
            for (int i = 0; i < values.Length; i++)
            {
                if ((int)values[i] == id) return values[i];
            }
            return PacketType.NONE;
        }

    }
}