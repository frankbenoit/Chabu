package chabu.tester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

import chabu.Utils;

public class Logger {

	public final static HashMap<String, Logger> loggers = new HashMap<>();
	
	public final String      name;
	public final PrintStream pw;
	
	public static Logger getLogger(String name) throws IOException{
		synchronized(Logger.class){
			if( !loggers.containsKey(name) ){
				loggers.put( name, new Logger(name));
			}
			return loggers.get(name);
		}
	}

	private Logger(String name) throws IOException {
		this.name = name;
		File file = new File( name + ".txt");
		
		Utils.ensure( !file.isDirectory() );
		if( file.exists() ){
			boolean success = file.delete();
			Utils.ensure(success);
		}
		boolean success = file.createNewFile();
		Utils.ensure(success);

		pw = new PrintStream( new FileOutputStream(file), true, "UTF-8");
		
	}

	public void printfln(String fmt, Object ... args ) {
		pw.printf(fmt, args);
		pw.println();
	}

	
	
}
