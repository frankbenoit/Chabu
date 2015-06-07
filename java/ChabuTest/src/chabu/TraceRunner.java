package chabu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import chabu.internal.Utils;



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
	
	private ArrayList<TestChannelUser> channelUsers = new ArrayList<TestChannelUser>();
	private final TestNetwork testNw = new TestNetwork();
	private int blockLineNum;

	public TraceRunner(){
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
		if( givenChabu == null ){
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
			
			ChabuConnectingInfo ci = new ChabuConnectingInfo();
			
			ci.chabuProtocolVersion  = setupParams.getInt    ("ChabuProtocolVersion" );
			ci.byteOrderBigEndian    = setupParams.getBoolean("ByteOrderBigEndian"   );
			ci.maxReceivePayloadSize = setupParams.getInt    ("MaxReceivePayloadSize");
			ci.receiveCannelCount    = setupParams.getInt    ("ReceiveCannelCount"   );
			ci.applicationVersion    = setupParams.getInt    ("ApplicationVersion"   );
			ci.applicationName       = setupParams.getString ("ApplicationName"      );
			
			ChabuBuilder builder = ChabuBuilder.start(ci);
			
			builder.setPriorityCount(setupParams.getInt ("PriorityCount" ));
			builder.setNetwork( testNw );
			
			
			JSONArray channels = setupParams.getJSONArray("Channels");
			for( int channelIdx = 0; channelIdx < channels.length(); channelIdx ++ ){
				JSONObject channelParams = (JSONObject)channels.get(channelIdx);
				ensure( channelIdx == channelParams.getInt("ID"), "ID should be %s, but is %s", channelIdx, channelParams.getInt("ID") );
				
				TestChannelUser channelUser = new TestChannelUser();
				builder.addChannel( 
						channelIdx, 
						channelParams.getInt("RxSize"), 
						channelParams.getInt("TxSize"),
						channelParams.getInt("Priority"),
						channelUser );
				
				channelUsers.add(channelUser);
			}
			
			chabu = builder.build();
		}
		else {
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

	/**
	 * Push the data into Chabu.
	 */
	private void wireRx(JSONObject params, ByteBuffer bb) {
//		System.out.printf("TraceRunner.wireRx( %s, %s )\n", params, bb.remaining());
		int more = params.optInt("More");
		bb.limit( bb.limit() + more );
		chabu.evRecv(bb);
		ensure( bb.remaining() == 0, "Chabu did not receive all data" );
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
		if( txBuf.limit() != bb.limit() ){
			isOk = false;
		}
		else {
			for( int i = 0; i < bb.limit(); i++ ){
				int exp = 0xFF & bb.get(i);
				int cur = 0xFF & txBuf.get(i);
				if( exp != cur ){
					isOk = false;
				}
			}
		}

		if( !isOk ){
			System.out.println("TX by chabu:"+HexDump.dumpHexString( txBuf ));
			Utils.ensure( txBuf.limit() == bb.limit(), "WIRE_TX @%s: TX length (%s) does not match the expected length (%s)", blockLineNum, txBuf.limit(), bb.limit() );
			for( int i = 0; i < bb.limit(); i++ ){
				int exp = 0xFF & bb.get(i);
				int cur = 0xFF & txBuf.get(i);
				Utils.ensure( cur == exp, "TX data (0x%02X) != expected (0x%02X) at index %d", cur, exp, i );
			}
		}
	}
	
	private void channelToAppl(JSONObject params, ByteBuffer bb) {
//		System.out.printf("TraceRunner.channelToAppl( %s, %s )\n", params, bb.remaining());
		int channelId = params.getInt("ID");
		TestChannelUser cu = channelUsers.get( channelId );
		cu.consumeRxData(bb);
	}

	private void applToChannel(JSONObject params, ByteBuffer bb) {
//		System.out.printf("TraceRunner.applToChannel( %s, %s )\n", params, bb.remaining());
		int channelId = params.getInt("ID");
		TestChannelUser cu = channelUsers.get( channelId );
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
		r.run( null, new FileReader("src/chabu/"+string));
	}
	public static void testText(String string) throws Exception {
		TraceRunner r = new TraceRunner();
		r.run( null, new StringReader(string));
	}
	public static void testText(IChabu givenChabu, String string) throws Exception {
		TraceRunner r = new TraceRunner();
		r.run( givenChabu, new StringReader(string));
	}
}
