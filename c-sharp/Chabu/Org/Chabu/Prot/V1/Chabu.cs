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
namespace Org.Chabu.Prot.V1{

    using PrintWriter= global::System.IO.TextWriter;
    using ByteBuffer= global::System.IO.MemoryStream;
    using Runnable = global::System.Action;

    /**
     * The main object communicating with the network and distributing/collecting the data to/from the
     * channels.
     *
     * @author Frank Benoit
     */
    public interface Chabu : ChabuNetworkHandler
    {

	    /**
	     * Get the count of configured channels.
	     */
	    int getChannelCount();

	    /**
	     * Get a specific channel.
	     * @param channelId the index of the channel to be returned.
	     *                  Must within 0 &le; channelId &lt; #getChannelCount().
	     * @return the channel.
	     */
	    ChabuChannel getChannel( int channelId );

    }
}