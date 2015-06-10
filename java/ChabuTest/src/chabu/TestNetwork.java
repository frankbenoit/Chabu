package chabu;


public class TestNetwork implements IChabuNetwork {

	@SuppressWarnings("unused")
	private IChabu chabu;
	boolean hadXmitReq = false;

	@Override
	public void evUserXmitRequest() {		
		hadXmitReq = true;
	}

	@Override
	public void setChabu(IChabu chabu) {
		this.chabu = chabu;
	}

}
