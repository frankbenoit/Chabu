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
namespace Org.Chabu.Prot.V1
{

    public interface ChabuChannelXmitter : ChabuChannelBase {

	    /**
	     * Set the index until which position chabu shall transmit data. 
	     * The value is only allowed to be increased. 
	     * If a value smaller as zero or smaller as a previously given value is given, a IndexOutofBoundsExcepiton will be thrown. 
	     * @param xmitLimit
	     */
	    long XmitLimit { get; set; }
	    long AddXmitLimit( int added );
	
	    long XmitPosition { get; }
	    int XmitRemaining { get; }
	
    }
}
