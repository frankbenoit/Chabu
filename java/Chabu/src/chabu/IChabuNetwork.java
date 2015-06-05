package chabu;


public interface IChabuNetwork {

	public void setChabu( Chabu chabu );
	
	public void evUserRecvRequest();
	
	public void evUserXmitRequest();
	
}
