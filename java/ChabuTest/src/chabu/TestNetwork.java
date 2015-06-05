package chabu;

public class TestNetwork implements IChabuNetwork {

	@SuppressWarnings("unused")
	private Chabu chabu;
	boolean hadRecvReq = false;
	boolean hadXmitReq = false;

	@Override
	public void evUserRecvRequest() {
		hadRecvReq = true;
	}

	@Override
	public void evUserXmitRequest() {		
		hadXmitReq = true;
	}

	@Override
	public void setChabu(Chabu chabu) {
		this.chabu = chabu;
	}

}
