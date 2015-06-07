package chabu;

@SuppressWarnings("serial")
public class ChabuConnectionAbortedException extends RuntimeException {

	public enum AbortCause {
		UNKNOWN,
		PROTOCOL_VERSION,
		ENDIANESS,
		APPL_VERSION,
		APPL_NAME, 
		VALIDATOR,
		;
	}
	
	private final AbortCause c;

	public ChabuConnectionAbortedException(String message, Object ... args ) {
		this( AbortCause.UNKNOWN, message, args );
	}

	public ChabuConnectionAbortedException(AbortCause c, String message, Object ... args ) {
		super(String.format(message, args));
		this.c = c;
	}
	
	public AbortCause getAbortCause(){
		return c;
	}
}
