#include <stdlib.h>
#include <stdarg.h>
#include <Common.h>
#include "TraceRunner.h"
#include "TestUtil.h"
#include "TestChannelUser.h"

#include <string.h>
#include <stdlib.h>
#include <jansson.h>

#define WIRE_RX         "WIRE_RX:"
#define WIRE_TX         "WIRE_TX:"
#define CHANNEL_TO_APPL "CHANNEL_TO_APPL:"
#define APPL_TO_CHANNEL "APPL_TO_CHANNEL:"
	
//#define Regex commentPattern = new Regex(@"^[ \\t0-9A-Fa-f]*//.*", RegexOptions.IgnoreCase );
	
static void TraceRunner_WireRx(struct TraceRunner_Data* data, json_t* params_, struct ByteBuffer_Data* bb);
static void TraceRunner_HexStringToBB(struct TraceRunner_Data* data, const char* hexData);
static void TraceRunner_ApplToChannel(struct TraceRunner_Data* data, json_t* params__, struct ByteBuffer_Data* bb);
static void TraceRunner_ChannelToAppl(struct TraceRunner_Data* data, json_t* params__, struct ByteBuffer_Data* bb);
	

void TraceRunner_Init(struct TraceRunner_Data* data, struct Chabu_Data* chabu) {
	data->chabu = chabu;
	ByteBuffer_Init( &data->bb, data->bbMem, sizeof(data->bbMem));
	ByteBuffer_Init( &data->txBuf, data->txBufMem, sizeof(data->txBufMem));

}

void TraceRunner_EnsureImpl( const char* file, long line, bool test, const char* fmt, ... ){
    va_list ap;
    va_start(ap, fmt);

	if( !test){
		printf("TraceRunner_Ensure %s %d\n", file, line );
	    vprintf(fmt, ap);
		//printf("TraceRunner Ensure\n");
		getchar();
		exit(EXIT_FAILURE);
		//throw new RuntimeException(const char*.Format(fmt, args));
	}

	va_end(ap);
}
//void TraceRunner_Run(struct TraceRunner_Data* data,  struct Chabu_Data_Data* givenChabu, Stream stream ) {
//	br = new BufferedReader(stream);
//	TraceRunner_NextLine(data);
//	TraceRunner_Ensure( data, strcmp("<ChabuTrace Format=1>", data->line ) == 0, "wrong format: %s", data->line);
//
//	TraceRunner_NextLine(data);
//	TraceRunner_SkipEmpyLines(data);
//
//	// setup
//	if( givenChabu == NULL && data->chabu == NULL ){
//		TraceRunner_Ensure( data, data->line.StartsWith("SETUP:"), "Not starting with SETUP: in line %s: %s", data->ln, data->line );
//			
//		StringBuilder sb = new StringBuilder();
//		sb.Append( data->line.Substring("SETUP:".Length ));
//		TraceRunner_NextLine(data);
//		while( data->line.Trim().Length > 0 ){
//			sb.Append(line);
//			TraceRunner_NextLine(data);
//		}
//		TraceRunner_SkipEmpyLines(data);
//		JSONObject setupParams = new JSONObject(sb.ToString());
////			System.out.println( "Setup: "+ setupParams);
//			
//		ChabuBuilder builder = ChabuBuilder.start(
//				setupParams.getInt    ("ApplicationVersion"),
//				setupParams.getString ("ApplicationName"   ),
//				setupParams.getInt    ("MaxReceiveSize"    ),
//				setupParams.getInt    ("PriorityCount"     ));
//			
//		JSONArray channels = setupParams.getJSONArray("Channels");
//		for( int channelIdx = 0; channelIdx < channels.length(); channelIdx ++ ){
//			JSONObject channelParams = (JSONObject)channels.get(channelIdx);
//			TraceRunner_Ensure( data, channelIdx == channelParams.getInt("ID"), "ID should be %s, but is %s", channelIdx, channelParams.getInt("ID") );
//				
//			TestChannelUser channelUser = new TestChannelUser();
//			builder.addChannel( 
//					channelIdx, 
//					channelParams.getInt("RxSize"), 
//					channelParams.getInt("Priority"),
//					channelUser );
//				
//		}
//			
//		data->chabu = builder.build();
//	}
//	else if( givenChabu != NULL ){
//		data->chabu = givenChabu;
//	}
//
//
//    TextWriter trcPrinter = Console.Out;
//	chabu.setTracePrinter( trcPrinter );
//	try{
//		while( data->line != NULL ){
//			data->blockLineNum = data->ln;
//			if( data->line.StartsWith(WIRE_RX)){
//				JSONObject params_ = getParams(WIRE_RX.Length);
//				getRawData();
//				TraceRunner_WireRx( data, params_, bb );
//				continue;
//			}
//			if( line.StartsWith(WIRE_TX)){
//				JSONObject params_ = getParams(WIRE_TX.Length);
//				getRawData();
//				TraceRunner_WireTx( data, params_, bb );
//				continue;
//			}
//			if( line.StartsWith(CHANNEL_TO_APPL)){
//				JSONObject params_ = getParams(CHANNEL_TO_APPL.Length);
//				getRawData();
//				TraceRunner_ChannelToAppl( data, params_, bb );
//				continue;
//			}
//			if( line.StartsWith(APPL_TO_CHANNEL)){
//				JSONObject params_ = getParams(APPL_TO_CHANNEL.Length);
//				getRawData();
//				TraceRunner_ApplToChannel( data, params_, bb );
//				continue;
//			}
//			TraceRunner_Ensure( data, data->line.Trim().Length == 0, "Unrecognized non empty line: %s %d", data->line, data->ln);
//			TraceRunner_NextLine(data);
//		}
//	} catch( Exception e ){
//		Console.Error.WriteLine("Block @{1}", blockLineNum);
//		Console.Error.WriteLine(e.StackTrace);
//		throw e;
//	} finally {
//		trcPrinter.Flush();
//	}
//}

void TraceRunner_WireRxAutoLength(struct TraceRunner_Data* data, const char* hexData) {
	int len;
	char* formattedText;
	int formattedTextLen;
	json_t* params__ = json_object();
	len = (strlen(hexData) + 1) / 3;
	len += 4;
	formattedTextLen = strlen(hexData) + 12+1;
	formattedText = (char*)malloc(formattedTextLen);
	snprintf( formattedText, formattedTextLen, "%02X %02X %02X %02X %s", len >> 24, len >> 16, len >> 8, 0xff & len, hexData);
	TraceRunner_HexStringToBB(data, formattedText );
	TraceRunner_WireRx( data, params__, &data->bb );
	json_decref(params__);
}
void TraceRunner_WireRxHex(struct TraceRunner_Data* data, const char* hexData) {
	json_t* params__ = json_object();
	TraceRunner_HexStringToBB(data, hexData);
	TraceRunner_WireRx( data, params__, &data->bb );
	json_decref(params__);
}
	
void TraceRunner_WireTxAutoLength(struct TraceRunner_Data* data, const char* hexData) {
	char* formattedText;
	int formattedTextLen;
	int len;
	json_t* params__ = json_object();

	len = (strlen(hexData) + 1) / 3;
	len += 4;
	formattedTextLen = strlen(hexData) + 12+1;
	formattedText = (char*)malloc(formattedTextLen);
	snprintf( formattedText, formattedTextLen, "%02X %02X %02X %02X %s", len >> 24, len >> 16, len >> 8, 0xff & len, hexData);

	TraceRunner_HexStringToBB(data, formattedText);
	TraceRunner_WireTx( data, params__, &data->bb );
	json_decref(params__);
}
void TraceRunner_WireTxHex(struct TraceRunner_Data* data, const char* hexData) {
	json_t* params__ = json_object();
	TraceRunner_HexStringToBB(data, hexData);
	TraceRunner_WireTx( data, params__, &data->bb );
	json_decref(params__);
}

void TraceRunner_WireTxHexMore(struct TraceRunner_Data* data,  int more, const char* hexData) {
	json_t* params__ = json_object();
	TraceRunner_HexStringToBB(data, hexData);
	json_object_set_new( params__, "More", json_integer( more ));
	TraceRunner_WireTx( data, params__, &data->bb );
	json_decref(params__);
}

static int hexCharToInt( char c ){
	if( c >= '0' && c <= '9' ) return c - '0';
	if( c >= 'a' && c <= 'f' ) return c - 'a' + 10;
	if( c >= 'A' && c <= 'F' ) return c - 'A' + 10;
	return 0xF;
}

static void TraceRunner_HexStringToBB(struct TraceRunner_Data* data, const char* hexData) {
	const char* p = hexData;
	uint8 b;
	ByteBuffer_clear(&data->bb);
	while( *p ){
		switch( *p ){
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			++p;
			continue;
		}
		b = hexCharToInt( *p );
		++p;
		if( !*p ) break;
		b <<= 4;
		b |= hexCharToInt( *p );
		++p;
		ByteBuffer_putByte( &data->bb, b );
	}
	ByteBuffer_flip(&data->bb);
	free((void*)hexData);
}

/**
 * Push the data into Chabu.
 */
static void TraceRunner_WireRx(struct TraceRunner_Data* data, json_t* params_, struct ByteBuffer_Data* bb) {
//		System.out.printf("TraceRunner.wireRx( %s, %s )\n", params_, bb.remaining());
	int more = 0;
	if( json_object_get(params_, "More") ){
		more = (int)json_integer_value( json_object_get(params_, "More"));
	}
	bb->limit += more;
	Chabu_PutRecvData( data->chabu, bb );
	TraceRunner_Ensure( ByteBuffer_remaining(bb) == more, "Chabu did not receive all data" );
}


/**
 * Test that Chabu is transmitting the expected data.
 */
static void TraceRunner_WireTx(struct TraceRunner_Data* data, json_t* params_, struct ByteBuffer_Data* bb) {
	int more = 0;
	bool isOk;
	int mismatchPos;
	
	if( json_object_get(params_, "More") ){
		more = (int)json_integer_value( json_object_get(params_, "More"));
	}
//		System.out.printf("TraceRunner.wireTx( %s, %s )\n", params_, bb.remaining());
	ByteBuffer_clear(&data->txBuf);
	data->txBuf.limit = data->bb.limit + more;
	Chabu_GetXmitData( data->chabu, &data->txBuf);
	ByteBuffer_flip(&data->txBuf);
		
	isOk = 1;
	mismatchPos = -1;
	if(data->txBuf.limit != data->bb.limit ){
		isOk = 0;
	}
	else {
		int i;
		for( i = 0; i < data->bb.limit; i++ ){
			int exp = 0xFF & data->bb.data[i];
			int cur = 0xFF & data->txBuf.data[i];
			if( exp != cur ){
				isOk = 0;
				mismatchPos = i;
				break;
			}
		}
	}

	if( !isOk ){
		int i;
		printf(">>> TraceRunner_WireTx problem <<< \n");
        printf("TX by org.chabu: \n");
		TestUtil_DumpHex(&data->txBuf);
        printf("Expected   :\n");
		TestUtil_DumpHex(bb);
		TraceRunner_Ensure( data->txBuf.limit == data->bb.limit, "WIRE_TX @%d: TX length (%d) does not match the expected length (%d). First mismatch at pos %d", data->blockLineNum, data->txBuf.limit, data->bb.limit, mismatchPos );
		for( i = 0; i < data->bb.limit; i++ ){
			int exp = 0xFF & data->bb.data[i];
			int cur = 0xFF & data->txBuf.data[i];
            TraceRunner_Ensure(cur == exp, "TX data (0x%02X) != expected (0x%02X) at index 0x%04X", cur, exp, i);
		}
	}
}
	
void TraceRunner_ChannelToApplHex(struct TraceRunner_Data* data, int channelId, const char* hexData) {
	json_t* params__ = json_object();
	TraceRunner_HexStringToBB(data, hexData);
	json_object_set_new( params__, "ID", json_integer(channelId));
	TraceRunner_ChannelToAppl(data, params__, &data->bb);
	json_decref(params__);
}
static void TraceRunner_ChannelToAppl(struct TraceRunner_Data* data, json_t* params__, struct ByteBuffer_Data* bb) {
//		System.out.printf("TraceRunner.channelToAppl( %s, %s )\n", params__, bb.remaining());
	int channelId = (int) json_integer_value( json_object_get(params__, "ID"));
	struct Chabu_Channel_Data* channel = Chabu_GetChannel( data->chabu, channelId);
	struct TestChannelUser_Data* cu = (struct TestChannelUser_Data*) Chabu_Channel_GetUserData(channel);
	TestChannelUser_ConsumeRxData( cu, bb );
}

void TraceRunner_ApplToChannelHex(struct TraceRunner_Data* data, int channelId, const char* hexData) {
	json_t* params__ = json_object();
	TraceRunner_HexStringToBB(data, hexData);
	json_object_set_new( params__, "ID", json_integer(channelId));
	TraceRunner_ApplToChannel(data, params__, &data->bb);
	json_decref(params__);
}
static void TraceRunner_ApplToChannel(struct TraceRunner_Data* data, json_t* params__, struct ByteBuffer_Data* bb) {
//		System.out.printf("TraceRunner.applToChannel( %s, %s )\n", params__, bb.remaining());
	int channelId = (int) json_integer_value( json_object_get(params__, "ID"));
	struct Chabu_Channel_Data* channel = Chabu_GetChannel( data->chabu, channelId);
	struct TestChannelUser_Data* cu = (struct TestChannelUser_Data*) Chabu_Channel_GetUserData(channel);
	TestChannelUser_AddTxData( cu, bb );
}
	
//static const char* removeComment( const char* str ){
//	int idx = str.IndexOf("//");
//	if( idx < 0 ){
//		return str;
//	}
//	return str.Substring(0, idx);
//}
//static void TraceRunner_GetRawData(struct TraceRunner_Data* data)  {
//	bb.clear();
//	while( !line.Trim().Equals("<<") ) {
//		//System.out.println("line: "+line);
//		StringTokenizer tokenizer = new StringTokenizer(line);
//		while( tokenizer.hasMoreTokens()){
//			const char* token = tokenizer.nextToken();
//			ensure( token.Length == 2, "RAW number has not length 2. Line: %s", ln );
//            bb.put((sbyte)Int32.Parse(token, System.Globalization.NumberStyles.HexNumber));
//		}
//		nextLine();
//	}
//	nextLine();
//	skipEmpyLines();
//	bb.flip();
//}
//
//static JSONObject TraceRunner_GetParams( struct TraceRunner_Data* data, int startIndex)  {
//	JSONObject params__ = new JSONObject( line.Substring(startIndex));
//	TraceRunner_NextLine(data);
//	return params__;
//}
//	
//static void TraceRunner_SkipEmpyLines(struct TraceRunner_Data* data )  {
//	while( data->line != NULL && data->line.Trim().isEmpty() ){
//		TraceRunner_NextLine(data);
//	}
//}
//
//static void TraceRunner_NextLine(struct TraceRunner_Data* data )  {
//	const char* l = br.ReadLine();
//	if( l == NULL ){
//		data->line = NULL;
//		return;
//	}
//	Match m = commentPattern.Match(l);
//	data->line = m.Success ? removeComment(l) : l;
//	data->ln++;
//}
//
//static void TraceRunner_TestFile(struct TraceRunner_Data* data, const char* str)  {
//	Console.WriteLine("Test: "+str);
//	TraceRunner r = new TraceRunner();
//	r.run( NULL, new FileStream("src/org.chabu/"+str, FileMode.Open, FileAccess.Read));
//}
//static void TraceRunner_TestText(struct TraceRunner_Data* data, const char* str)  {
//	TraceRunner r = new TraceRunner();
//	r.run( null, new MemoryStream(Encoding.UTF8.GetBytes(str)));
//}
//static void TraceRunner_TestText(struct TraceRunner_Data* data, struct Chabu_Data* givenChabu, const char* str)  {
//	TraceRunner r = new TraceRunner();
//    r.run(givenChabu, new MemoryStream(Encoding.UTF8.GetBytes(str)));
//}
//
//static TraceRunner TraceRunner_Test(struct TraceRunner_Data* data, struct Chabu_Data* chabu) {
//	TraceRunner r = new TraceRunner(chabu);
//	return r;
//}



