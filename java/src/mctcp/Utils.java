package mctcp;

public class Utils {
	public static void ensure( boolean cond, String fmt, Object ... args ){
		if( cond ) return;
		throw new RuntimeException( String.format( fmt, args ));
	}
}
