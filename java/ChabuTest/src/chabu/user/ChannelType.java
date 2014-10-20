package chabu.user;

public enum ChannelType {
	ServerRx( true , false ),
	ServerTx( true , true  ), 
	ClientRx( false, false ), 
	ClientTx( false, true  );
	
	public final boolean isServer;
	public final boolean isTx;

	private ChannelType( boolean isServer, boolean isTx ){
		this.isServer = isServer;
		this.isTx = isTx;
		
	}
}
