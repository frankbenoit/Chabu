using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using ChabuCSTest.org.chabu;

namespace org.chabu
{
    using System.Text;
    using System.Text.RegularExpressions;

    using TextWriter = System.IO.TextWriter;
    using ByteBuffer = System.IO.MemoryStream;
    using RuntimeException = System.SystemException;
    using BufferedReader = System.IO.StreamReader ;
    using System.IO;

    //import java.io.BufferedReader;
    //import java.io.FileReader;
    //import java.io.IOException;
    //import java.io.PrintWriter;
    //import java.io.Reader;
    //import java.io.StringReader;
    //import java.nio.ByteBuffer;
    //import java.util.StringTokenizer;
    //import java.util.regex.Matcher;
    //import java.util.regex.Pattern;

    //import org.json.JSONArray;
    //import org.json.JSONObject;

    public class TraceRunner {

	    private const String WIRE_RX         = "WIRE_RX:";
	    private const String WIRE_TX         = "WIRE_TX:";
	    private const String CHANNEL_TO_APPL = "CHANNEL_TO_APPL:";
	    private const String APPL_TO_CHANNEL = "APPL_TO_CHANNEL:";
	
	    private readonly Regex commentPattern = new Regex(@"^[ \\t0-9A-Fa-f]*//.*", RegexOptions.IgnoreCase );
	
	    private readonly ByteBuffer bb    = new ByteBuffer(10000);
        private readonly ByteBuffer txBuf = new ByteBuffer(10000);
        //	private readonly ByteBuffer rxBuf = new ByteBuffer(10000);
	
	    String line;
	    int ln = 0;
	    private BufferedReader br;
	    private Chabu chabu;
	
	    private int blockLineNum;

	    public TraceRunner(){
	    }

	    public TraceRunner(Chabu chabu) {
		    this.chabu = chabu;
	    }

	    public static void ensure( bool test, String fmt, params Object[] args ){
		    if( !test){
			    throw new RuntimeException(String.Format(fmt, args));
		    }
	    }
	    public void run( Chabu givenChabu, Stream stream ) {
		    br = new BufferedReader(stream);
		    nextLine();
		    ensure( "<ChabuTrace Format=1>".Equals( line ), "wrong format: %s", line);

		    nextLine();
		    skipEmpyLines();

		    // setup
		    if( givenChabu == null && this.chabu == null ){
			    ensure( line.StartsWith("SETUP:"), "Not starting with SETUP: in line %s: %s", ln, line );
			
			    StringBuilder sb = new StringBuilder();
			    sb.Append( line.Substring("SETUP:".Length ));
			    nextLine();
			    while( line.Trim().Length > 0 ){
				    sb.Append(line);
				    nextLine();
			    }
			    skipEmpyLines();
			    JSONObject setupParams = new JSONObject(sb.ToString());
    //			System.out.println( "Setup: "+ setupParams);
			
			    ChabuBuilder builder = ChabuBuilder.start(
					    setupParams.getInt    ("ApplicationVersion"),
					    setupParams.getString ("ApplicationName"   ),
					    setupParams.getInt    ("MaxReceiveSize"    ),
					    setupParams.getInt    ("PriorityCount"     ));
			
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


            TextWriter trcPrinter = Console.Out;
		    chabu.setTracePrinter( trcPrinter );
		    try{
			    while( line != null ){
				    blockLineNum = ln;
				    if( line.StartsWith(WIRE_RX)){
					    JSONObject params_ = getParams(WIRE_RX.Length);
					    getRawData();
					    wireRx( params_, bb );
					    continue;
				    }
				    if( line.StartsWith(WIRE_TX)){
					    JSONObject params_ = getParams(WIRE_TX.Length);
					    getRawData();
					    wireTx( params_, bb );
					    continue;
				    }
				    if( line.StartsWith(CHANNEL_TO_APPL)){
					    JSONObject params_ = getParams(CHANNEL_TO_APPL.Length);
					    getRawData();
					    channelToAppl( params_, bb );
					    continue;
				    }
				    if( line.StartsWith(APPL_TO_CHANNEL)){
					    JSONObject params_ = getParams(APPL_TO_CHANNEL.Length);
					    getRawData();
					    applToChannel( params_, bb );
					    continue;
				    }
				    ensure( line.Trim().Length == 0, "Unrecognized non empty line: %s %d", line, ln);
				    nextLine();
			    }
		    } catch( Exception e ){
			    Console.Error.WriteLine("Block @{1}", blockLineNum);
			    Console.Error.WriteLine(e.StackTrace);
			    throw e;
		    } finally {
			    trcPrinter.Flush();
		    }
	    }

	    public void wireRxAutoLength(String hexData) {
		    int len = (hexData.Length + 1) / 3;
		    len += 4;
		    hexStringToBB(String.Format( "{0:2X} {1:2X} {2:2X} {3:2X} {4}", len >> 24, len >> 16, len >> 8, 0xff & len, hexData));
		    JSONObject params__ = new JSONObject();
		    wireRx( params__, bb );
	    }
	    public void wireRx(String hexData) {
		    hexStringToBB(hexData);
		    JSONObject params__ = new JSONObject();
		    wireRx( params__, bb );
	    }
	
	    public void wireTxAutoLength(String hexData) {
		    int len = (hexData.Length + 1) / 3;
		    len += 4;
		    hexStringToBB(String.Format( "{0:2X} {1:2X} {2:2X} {3:2X} {4}", len >> 24, len >> 16, len >> 8, 0xff & len, hexData));
		    JSONObject params__ = new JSONObject();
		    wireTx( params__, bb );
	    }
	    public void wireTx(String hexData) {
		    hexStringToBB(hexData);
		    JSONObject params__ = new JSONObject();
		    wireTx( params__, bb );
	    }
	    public void wireTx( int more, String hexData) {
		    hexStringToBB(hexData);
		    JSONObject params__ = new JSONObject();
		    params__.put( "More", more );
		    wireTx( params__, bb );
	    }

	    private void hexStringToBB(String hexData) {
		    bb.clear();
		    StringTokenizer tokenizer = new StringTokenizer(hexData);
		    while( tokenizer.hasMoreTokens()){
			    String token = tokenizer.nextToken();
			    ensure( token.Length == 2, "RAW number has not length 2. Line: {1}", ln );
			    bb.put( (sbyte) Convert.ToInt32(token, 16));
		    }
		    bb.flip();
	    }
	    /**
	     * Push the data into Chabu.
	     */
	    private void wireRx(JSONObject params__, ByteBuffer bb) {
    //		System.out.printf("TraceRunner.wireRx( %s, %s )\n", params_, bb.remaining());
		    int more = params__.optInt("More");
		    bb.limit( bb.limit() + more );
		    chabu.recv(bb);
		    ensure( bb.remaining() == more, "Chabu did not receive all data" );
	    }

	    /**
	     * Test that Chabu is transmitting the expected data.
	     */
	    private void wireTx(JSONObject params_, ByteBuffer bb) {
		    int more = params_.optInt("More");
    //		System.out.printf("TraceRunner.wireTx( %s, %s )\n", params_, bb.remaining());
		    txBuf.clear();
		    txBuf.limit(bb.limit()+more);
		    chabu.xmit(txBuf);
		    txBuf.flip();
		
		    bool isOk = true;
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
			    Console.WriteLine("TX by org.chabu:"+TestUtils.dumpHexString( txBuf ));
                Console.WriteLine("Expected   :" + TestUtils.dumpHexString(bb));
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
		    JSONObject params__ = new JSONObject();
		    params__.put("ID", channelId);
		    channelToAppl(params__, bb);
	    }
	    private void channelToAppl(JSONObject params__, ByteBuffer bb) {
    //		System.out.printf("TraceRunner.channelToAppl( %s, %s )\n", params__, bb.remaining());
		    int channelId = params__.getInt("ID");
		    TestChannelUser cu = (TestChannelUser)chabu.getChannel(channelId).getUser();
		    cu.consumeRxData(bb);
	    }

	    public void applToChannel(int channelId, String hexData) {
		    hexStringToBB(hexData);
		    JSONObject params__ = new JSONObject();
		    params__.put("ID", channelId);
		    applToChannel(params__, bb);
	    }
	    private void applToChannel(JSONObject params__, ByteBuffer bb) {
    //		System.out.printf("TraceRunner.applToChannel( %s, %s )\n", params__, bb.remaining());
		    int channelId = params__.getInt("ID");
		    TestChannelUser cu = (TestChannelUser)chabu.getChannel(channelId).getUser();
		    cu.addTxData(bb);
	    }
	
	    private static String removeComment( String str ){
		    int idx = str.IndexOf("//");
		    if( idx < 0 ){
			    return str;
		    }
		    return str.Substring(0, idx);
	    }
	    private void getRawData()  {
		    bb.clear();
		    while( !line.Trim().Equals("<<") ) {
			    //System.out.println("line: "+line);
			    StringTokenizer tokenizer = new StringTokenizer(line);
			    while( tokenizer.hasMoreTokens()){
				    String token = tokenizer.nextToken();
				    ensure( token.Length == 2, "RAW number has not length 2. Line: %s", ln );
                    bb.put((sbyte)Int32.Parse(token, System.Globalization.NumberStyles.HexNumber));
			    }
			    nextLine();
		    }
		    nextLine();
		    skipEmpyLines();
		    bb.flip();
	    }

	    private JSONObject getParams( int startIndex)  {
		    JSONObject params__ = new JSONObject( line.Substring(startIndex));
		    nextLine();
		    return params__;
	    }
	
	    private void skipEmpyLines()  {
		    while( line != null && line.trim().isEmpty() ){
			    nextLine();
		    }
	    }

	    private void nextLine()  {
		    String l = br.ReadLine();
		    if( l == null ){
			    line = null;
			    return;
		    }
		    Match m = commentPattern.Match(l);
		    line = m.Success ? removeComment(l) : l;
		    ln++;
	    }

	    public static void testFile(String str)  {
		    Console.WriteLine("Test: "+str);
		    TraceRunner r = new TraceRunner();
		    r.run( null, new FileStream("src/org.chabu/"+str, FileMode.Open, FileAccess.Read));
	    }
	    public static void testText(String str)  {
		    TraceRunner r = new TraceRunner();
		    r.run( null, new MemoryStream(Encoding.UTF8.GetBytes(str)));
	    }
	    public static void testText(Chabu givenChabu, String str)  {
		    TraceRunner r = new TraceRunner();
            r.run(givenChabu, new MemoryStream(Encoding.UTF8.GetBytes(str)));
	    }

	    public static TraceRunner test(Chabu chabu) {
		    TraceRunner r = new TraceRunner(chabu);
		    return r;
	    }
    }
}