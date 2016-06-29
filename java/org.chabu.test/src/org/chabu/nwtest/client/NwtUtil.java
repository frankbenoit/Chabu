package org.chabu.nwtest.client;

import java.io.FileWriter;
import java.io.IOException;

public class NwtUtil {

	private static FileWriter fw;
	public static void log( String fmt, Object ... args ){
		try {
			if( fw == null ){
				fw = new FileWriter( "client.log.txt" );
			}
			fw.append(String.format( "%-4d %s\n", System.currentTimeMillis() % 10000, String.format( fmt, args)));
			fw.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static void closeLog() {
		try {
			if( fw != null ){
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println("NwtUtil.closeLog");
	}

}
