package chabu;

public class Utils {
	
	public static RuntimeException fail( String fmt, Object ... args ){
		throw new RuntimeException( String.format( fmt, args ));
	}
	
	/**
	 * Throw an exception if the condition is not true.
	 * @param cond
	 */
	public static void ensure( boolean cond, String fmt, Object ... args ){
		if( cond ) return;
		throw new RuntimeException( String.format( fmt, args ));
	}
	
	/**
	 * Throw an exception if the condition is not true.
	 * @param cond
	 */
	public static void ensure( boolean cond ){
		if( cond ) return;
		throw new RuntimeException();
	}
	
	/**
	 * Wrap Object.wait() and throw RuntimeException instead of InterruptedException
	 */
	public static void waitOn( Object o ){
		synchronized( o ){
			try {
				o.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Wrap Object.wait() and throw RuntimeException instead of InterruptedException
	 */
	public static void waitOn( Object o, long timeout ){
		synchronized( o ){			
			try {
				o.wait( timeout );
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void notifyAllOn( Object o ){
		synchronized(o){
			o.notifyAll();
		}
	}
}
