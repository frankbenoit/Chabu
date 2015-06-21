package org.chabu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chabu.ChabuBuilder;
import org.chabu.ChabuSetupInfo;
import org.chabu.IChabu;
import org.json.JSONArray;
import org.json.JSONObject;



public class TraceRunner {

	private static final String WIRE_RX         = "WIRE_RX:";
	private static final String WIRE_TX         = "WIRE_TX:";
	private static final String CHANNEL_TO_APPL = "CHANNEL_TO_APPL:";
	private static final String APPL_TO_CHANNEL = "APPL_TO_CHANNEL:";
	
	private static final Pattern commentPattern = Pattern.compile("^[ \\t0-9A-Fa-f]*//.*");
	
	private final ByteBuffer bb    = ByteBuffer.allocate(10000);
	private final ByteBuffer txBuf = ByteBuffer.allocate(10000);
//	private final ByteBuffer rxBuf = ByteBuffer.allocate(10000);
	
	String line;
	int ln = 0;
	private BufferedReader br;
	private IChabu chabu;
	
	private int blockLineNum;

	public TraceRunner(){
	}

	public TraceRunner(IChabu chabu) {
		this.chabu = chabu;
	}

	static void ensure( boolean test, String fmt, Object ... args ){
		if( !test){
			throw new RuntimeException(String.format(fmt, args));
		}
	}
	public void run( IChabu givenChabu, Reader reader ) throws IOException {
		br = new BufferedReader(reader);
		nextLine();
		ensure( "<ChabuTrace Format=1>".equals( line ), "wrong format: %s", line);

		nextLine();
		skipEmpyLines();

		// setup
		if( givenChabu == null && this.chabu == null ){
			ensure( line.startsWith("SETUP:"), "Not starting with SETUP: in line %s: %s", ln, line );
			
			StringBuilder sb = new StringBuilder();
			sb.append( line.substring("SETUP:".length()));
			nextLine();
			while( !line.trim().isEmpty() ){
				sb.append(line);
				nextLine();
			}
			skipEmpyLines();
			JSONObject setupParams = new JSONObject(sb.toString());
//			System.out.println( "Setup: "+ setupParams);
			
			ChabuSetupInfo ci = new ChabuSetupInfo();
			
			ci.maxReceiveSize = setupParams.getInt    ("MaxReceiveSize");
			ci.applicationVersion    = setupParams.getInt    ("ApplicationVersion"   );
			ci.applicationName       = setupParams.getString ("ApplicationName"      );
			
			ChabuBuilder builder = ChabuBuilder.start(ci, new TestNetwork(), setupParams.getInt ("PriorityCount" ));
			
			JSONArray channels = setupParams.getJSONArray("Channels");
			for( int channelIdx = 0; channelIdx < channels.length(); channelIdx ++ ){
				JSONObject channelParams = (JSONObject)channels.get(channelIdx);
				ensure( channelIdx == channelParams.getInt("ID"), "ID should be %s, but is %s", channelIdx, channelParams.getInt("ID") );
				
				TestChannelUser channelUser = new TestChannelUser();
				builder.addChannel( 
						channelIdx, 
						channelParams.getInt("RxSize"), 
						channelParams.getInt("Priority"),
						channelUser );
				
			}
			
			chabu = builder.build();
		}
		else if( givenChabu != null ){
			chabu = givenChabu;
		}
		

		PrintWriter trcPrinter = new PrintWriter(System.out);
		chabu.setTracePrinter( trcPrinter );
		try{
			while( line != null ){
				blockLineNum = ln;
				if( line.startsWith(WIRE_RX)){
					JSONObject params = getParams(WIRE_RX.length());
					getRawData();
					wireRx( params, bb );
					continue;
				}
				if( line.startsWith(WIRE_TX)){
					JSONObject params = getParams(WIRE_TX.length());
					getRawData();
					wireTx( params, bb );
					continue;
				}
				if( line.startsWith(CHANNEL_TO_APPL)){
					JSONObject params = getParams(CHANNEL_TO_APPL.length());
					getRawData();
					channelToAppl( params, bb );
					continue;
				}
				if( line.startsWith(APPL_TO_CHANNEL)){
					JSONObject params = getParams(APPL_TO_CHANNEL.length());
					getRawData();
					applToChannel( params, bb );
					continue;
				}
				ensure( line.trim().isEmpty(), "Unrecognized non empty line: %s %d", line, ln);
				nextLine();
			}
		} catch( Exception e ){
			System.err.printf("Block @%s\n", blockLineNum);
			e.printStackTrace(System.err);
			throw e;
		} finally {
			trcPrinter.flush();
		}
	}

	public void wireRxAutoLength(String hexData) {
		int len = (hexData.length() + 1) / 3;
		len += 4;
		hexStringToBB(String.format( "%02X %02X %02X %02X %s", len >> 24, len >> 16, len >> 8, 0xff & len, hexData));
		JSONObject params = new JSONObject();
		wireRx( params, bb );
	}
	public void wireRx(String hexData) {
		hexStringToBB(hexData);
		JSONObject params = new JSONObject();
		wireRx( params, bb );
	}
	
	public void wireTxAutoLength(String hexData) {
		int len = (hexData.length() + 1) / 3;
		len += 4;
		hexStringToBB(String.format( "%02X %02X %02X %02X %s", len >> 24, len >> 16, len >> 8, 0xff & len, hexData));
		JSONObject params = new JSONObject();
		wireTx( params, bb );
	}
	public void wireTx(String hexData) {
		hexStringToBB(hexData);
		JSONObject params = new JSONObject();
		wireTx( params, bb );
	}
	public void wireTx( int more, String hexData) {
		hexStringToBB(hexData);
		JSONObject params = new JSONObject();
		params.put( "More", more );
		wireTx( params, bb );
	}

	private void hexStringToBB(String hexData) {
		bb.clear();
		StringTokenizer tokenizer = new StringTokenizer(hexData);
		while( tokenizer.hasMoreTokens()){
			String token = tokenizer.nextToken();
			ensure( token.length() == 2, "RAW number has not length 2. Line: %s", ln );
			bb.put( (byte) Integer.parseInt(token, 16));
		}
		bb.flip();
	}
	/**
	 * Push the data into Chabu.
	 */
	private void wireRx(JSONObject params, ByteBuffer bb) {
//		System.out.printf("TraceRunner.wireRx( %s, %s )\n", params, bb.remaining());
		int more = params.optInt("More");
		bb.limit( bb.limit() + more );
		chabu.evRecv(bb);
		ensure( bb.remaining() == more, "Chabu did not receive all data" );
	}

	/**
	 * Test that Chabu is transmitting the expected data.
	 */
	private void wireTx(JSONObject params, ByteBuffer bb) {
		int more = params.optInt("More");
//		System.out.printf("TraceRunner.wireTx( %s, %s )\n", params, bb.remaining());
		txBuf.clear();
		txBuf.limit(bb.limit()+more);
		chabu.evXmit(txBuf);
		txBuf.flip();
		
		boolean isOk = true;
		int mismatchPos = -1;
		if( txBuf.limit() != bb.limit() ){
			isOk = false;
		}
		else {
			for( int i = 0; i < bb.limit(); i++ ){
				int exp = 0xFF & bb.get(i);
				int cur = 0xFF & txBuf.get(i);
				if( exp != cur ){
					isOk = false;
					mismatchPos = i;
					break;
				}
			}
		}

		if( !isOk ){
			System.out.println("TX by org.chabu:"+TestUtils.dumpHexString( txBuf ));
			System.out.println("Expected   :"+TestUtils.dumpHexString( bb ));
			ensure( txBuf.limit() == bb.limit(), "WIRE_TX @%s: TX length (%s) does not match the expected length (%s). First mismatch at pos %s", blockLineNum, txBuf.limit(), bb.limit(), mismatchPos );
			for( int i = 0; i < bb.limit(); i++ ){
				int exp = 0xFF & bb.get(i);
				int cur = 0xFF & txBuf.get(i);
				ensure( cur == exp, "TX data (0x%02X) != expected (0x%02X) at index 0x%X", cur, exp, i );
			}
		}
	}
	
	public void channelToAppl(int channelId, String hexData) {
		hexStringToBB(hexData);
		JSONObject params = new JSONObject();
		params.put("ID", channelId);
		channelToAppl(params, bb);
	}
	private void channelToAppl(JSONObject params, ByteBuffer bb) {
//		System.out.printf("TraceRunner.channelToAppl( %s, %s )\n", params, bb.remaining());
		int channelId = params.getInt("ID");
		TestChannelUser cu = (TestChannelUser)chabu.getChannel(channelId).getUser();
		cu.consumeRxData(bb);
	}

	public void applToChannel(int channelId, String hexData) {
		hexStringToBB(hexData);
		JSONObject params = new JSONObject();
		params.put("ID", channelId);
		applToChannel(params, bb);
	}
	private void applToChannel(JSONObject params, ByteBuffer bb) {
//		System.out.printf("TraceRunner.applToChannel( %s, %s )\n", params, bb.remaining());
		int channelId = params.getInt("ID");
		TestChannelUser cu = (TestChannelUser)chabu.getChannel(channelId).getUser();
		cu.addTxData(bb);
	}
	
	private static String removeComment( String str ){
		int idx = str.indexOf("//");
		if( idx < 0 ){
			return str;
		}
		return str.substring(0, idx);
	}
	private void getRawData() throws IOException {
		bb.clear();
		while( !line.trim().equals("<<") ) {
			//System.out.println("line: "+line);
			StringTokenizer tokenizer = new StringTokenizer(line);
			while( tokenizer.hasMoreTokens()){
				String token = tokenizer.nextToken();
				ensure( token.length() == 2, "RAW number has not length 2. Line: %s", ln );
				bb.put( (byte) Integer.parseInt(token, 16));
			}
			nextLine();
		}
		nextLine();
		skipEmpyLines();
		bb.flip();
	}

	private JSONObject getParams( int startIndex) throws IOException {
		JSONObject params = new JSONObject( line.substring(startIndex));
		nextLine();
		return params;
	}
	
	private void skipEmpyLines() throws IOException {
		while( line != null && line.trim().isEmpty() ){
			nextLine();
		}
	}

	private void nextLine() throws IOException {
		String l = br.readLine();
		if( l == null ){
			line = null;
			return;
		}
		Matcher m = commentPattern.matcher(l);
		line = m.matches() ? removeComment(l) : l;
		ln++;
	}

	public static void testFile(String string) throws Exception {
		System.out.println("Test: "+string);
		TraceRunner r = new TraceRunner();
		r.run( null, new FileReader("src/org.chabu/"+string));
	}
	public static void testText(String string) throws Exception {
		TraceRunner r = new TraceRunner();
		r.run( null, new StringReader(string));
	}
	public static void testText(IChabu givenChabu, String string) throws Exception {
		TraceRunner r = new TraceRunner();
		r.run( givenChabu, new StringReader(string));
	}

	public static TraceRunner test(IChabu chabu) {
		TraceRunner r = new TraceRunner(chabu);
		return r;
	}
}
