package mctcp;


public interface INetwork {

	public void setNetworkUser( INetworkUser user );
	
	public void evUserRecvRequest();
	
	public void evUserXmitRequest();
	
}
