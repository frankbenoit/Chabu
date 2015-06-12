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

	public void setChabu( IChabu chabu );
	
//	public void evUserRecvRequest();
	
	public void evUserXmitRequest();
	
}
