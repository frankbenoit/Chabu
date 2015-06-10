package chabu;

public interface IChabuConnectingValidator {

	/**
	 * For accepted, either returns null or a ChabuConnectionAcceptInfo with code set to 0.
	 */
	public ChabuConnectionAcceptInfo isAccepted( ChabuSetupInfo local, ChabuSetupInfo remote );
	
}
