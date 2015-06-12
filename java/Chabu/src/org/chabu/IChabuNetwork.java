/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit.
 * 
 * See the LICENSE.txt or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu;


/**
 * Methods needed to be given to org.chabu, to call back to the network.
 * Chabu needs the possibility to tell the network, when it wants to send (xmit) or receive (recv) data.
 * This can be used e.g. for setting interest keys for a selector.
 * 
 * @author Frank Benoit
 * 
 */
public interface IChabuNetwork {

	/**
	 * Called in the startup once.
	 * If needed store this for later use.
	 */
	public void setChabu( IChabu chabu );
	
	/**
	 * Notifies the network about interest in getting called to {@link IChabu#evXmit(java.nio.ByteBuffer)}.
	 */
	public void evUserXmitRequest();
	
}
