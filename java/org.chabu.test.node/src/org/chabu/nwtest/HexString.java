package org.chabu.nwtest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;


/**
 * @author Frank Benoit
 */
public class HexString {

    byte[] data;
    
	public HexString() {
		data = new byte[0];
	}

	public HexString( String str ){
    	this.data = toBytes(str);
    }
    
	/**
	 * Parser a string in format "[ 255, 0, 99 ]" or "[ 0x23, 0xFD, 0x3D ]" where the numbers are unsigned decimal or 
	 * "0x" prefixed hexadecimal and each number is filled into a single byte.
	 */
	public static HexString parseNumArray(String str){
		// remove [ ]
		str = str.substring(1, str.length()-1);
		String[] parts = str.split(", *");
		byte[] bytes = new byte[ parts.length ];
		int idx = 0;
		for( String part : parts){
			if( part.startsWith("0x")){
				bytes[idx] = (byte) Integer.parseInt(part.substring(2), 16 );
			}
			else {
				bytes[idx] = (byte)Integer.parseInt(part);
			}
			idx++;
		}
		return new HexString(bytes);
	}
	
	/**
	 * Parser a string in format "[ 01 FE 2D ]" or "\"01 FE 2D\"" where the numbers are in HexString format.
	 */
	public static HexString parseRawArray(String str) {
		if( str.startsWith("[") && str.endsWith("]")){
			str = str.substring( 1, str.length() -1 );
		}
		if( str.startsWith("\"") && str.endsWith("\"")){
			str = str.substring( 1, str.length() -1 );
		}
		if( str.startsWith("\'") && str.endsWith("\'")){
			str = str.substring( 1, str.length() -1 );
		}
		str = str.trim();
		String[] parts = str.split(" +");
		byte[] bytes = new byte[ parts.length ];
		try{
			int idx = 0;
			for( String part : parts){
				bytes[idx] = (byte)Integer.parseInt(part, 16 );
				idx++;
			}
		}
		catch( NumberFormatException e ){
			throw new RuntimeException(e);
		}
		return new HexString(bytes);
	}
	
    public HexString( HexString other ){
        
        if( other.data == null ){
            data = new byte[0];
            return;
        }
        
        data = new byte[ other.data.length ];
        System.arraycopy( other.data, 0, data, 0, other.data.length );
    }
    public HexString(byte[] data) {
    	this.data = data;
	}

	public HexString(byte[] data, int offset, int length) {
		this.data = new byte[ length ];
		System.arraycopy(data, offset, this.data, 0, length);
	}

	public byte[] toArray(){
        return data;
    }

	private static final char getHexChar( int value ){
		if( value < 10 ) return (char)('0' + value);
		return (char)('A' + value - 10);
	}
	
	/**
	 * Build uppercase hex string of contained data
	 */
    public String toString(){
    	StringBuilder sb = new StringBuilder();
    	for( byte b : data ){
    		if( sb.length() > 0 ){
    			sb.append(' ');
    		}
    		sb.append( getHexChar( (b>>4)&0x0F ));
    		sb.append( getHexChar(  b    &0x0F ));
    	}
        return sb.toString();
    }
    /**
     * Build a uppercase hex string of the contained data, starting with offset and using length count of data bytes.
     * @param offset the 0-based index within the data as bytes.
     * @param length the amount of bytes
     */
	public String toString(int offset, int length) {
		return toString( this.data, offset, length );
	}
	
	public int length() {
		return data.length;
	}

	public static String toString(byte[] data) {
		if( data == null ) return "";
		return toString( data, 0, data.length );
	}
	
    public static String toHexDump(byte[] data){
		if( data == null || data.length == 0 ) return "";
    	return toHexDump("", data, 0, data.length);
    }
    public static String toHexDump( String indent, byte[] data){
    	if( data == null || data.length == 0 ) return "";
    	return toHexDump( indent, data, 0, data.length);
    }
    public static String toHexDump(byte[] data, int offset, int length ){
    	return toHexDump( "", data, offset, length );
    }
    public static String toHexDump( String indent, byte[] data, int offset, int length ){
		if( data == null || data.length == 0 ) return "";

		StringBuilder sb = new StringBuilder();
    	for( int i = 0; i < length; i+=16 ){
    		sb.append( String.format("%s%04X:", indent, i ));
        	for( int k = i; k < length && k < i+16; k++ ){
        		int v = data[k+offset];
        		sb.append(' ');
        		if( k%4 == 0 ) sb.append(' ');
        		sb.append( getHexChar( (v>>4)&0x0F ));
        		sb.append( getHexChar(  v    &0x0F ));
        	}
        	sb.append('\n');
    	}
        return sb.toString();
    }
	public static String toString(byte[] data, int offset, int length) {
    	StringBuilder sb = new StringBuilder();
    	for( int i = offset; i < offset+length; i++ ){
    		if( sb.length() > 0 ){
    			sb.append(' ');
    		}
    		int v = data[i];
    		sb.append( getHexChar( (v>>4)&0x0F ));
    		sb.append( getHexChar(  v    &0x0F ));
    	}
        return sb.toString();
	}

	/**
	 * Convert ASCII string in format "00 11 22" to a byte array with those values.
	 * @param str ASCII string
	 * @return the values as byte array
	 */
	public static byte[] toBytes(String str) {
		if( str == null ){
			return new byte[0];
		}
		if( str.startsWith("\"") && str.endsWith("\"")){
			str = str.substring(1, str.length()-1 );
		}
        String singleSpaces = str.replaceAll("\\s\\s+", " ");
        if( singleSpaces.length() == 0 ){
        	return new byte[0];
        }
		String[] tokens = singleSpaces.split( "[ \\t]+" );
        byte[] data = new byte[ tokens.length ];
        try {
        	for( int i = 0; i < tokens.length; i++ ){
        		data[i] = (byte) Integer.valueOf( tokens[i], 16 ).intValue();
        	}
        }
        catch( NumberFormatException e ) {
        	throw new RuntimeException("str: "+str, e);
        }
		return data;
	}

	public HexString appendUint8(int aValue ) {
		byte[] newData = new byte[ data.length + 1 ];
		System.arraycopy(data, 0, newData, 0, data.length );
		newData[data.length+0] = (byte)((aValue>> 0) & 0xFF);
		data = newData;
		return this;
	}

	public HexString appendUint16(int aValue, boolean isBigEndian ) {
		byte[] newData = new byte[ data.length + 2 ];
		System.arraycopy(data, 0, newData, 0, data.length );
		if( isBigEndian ){
			newData[data.length+0] = (byte)((aValue>> 8) & 0xFF);
			newData[data.length+1] = (byte)((aValue>> 0) & 0xFF);
		}
		else {
			newData[data.length+0] = (byte)((aValue>> 0) & 0xFF);
			newData[data.length+1] = (byte)((aValue>> 8) & 0xFF);
		}
		data = newData;
		return this;
	}

	public HexString appendUint24(int aValue, boolean isBigEndian ) {
		byte[] newData = new byte[ data.length + 3 ];
		System.arraycopy(data, 0, newData, 0, data.length );
		if( isBigEndian ){
			newData[data.length+0] = (byte)((aValue>>16) & 0xFF);
			newData[data.length+1] = (byte)((aValue>> 8) & 0xFF);
			newData[data.length+2] = (byte)((aValue>> 0) & 0xFF);
		}
		else {
			newData[data.length+0] = (byte)((aValue>> 0) & 0xFF);
			newData[data.length+1] = (byte)((aValue>> 8) & 0xFF);
			newData[data.length+2] = (byte)((aValue>>16) & 0xFF);
		}
		data = newData;
		return this;
	}

	public HexString appendUint32(int aValue, boolean isBigEndian ) {
		byte[] newData = new byte[ data.length + 4 ];
		System.arraycopy(data, 0, newData, 0, data.length );
		if( isBigEndian ){
			newData[data.length+0] = (byte)((aValue>>24) & 0xFF);
			newData[data.length+1] = (byte)((aValue>>16) & 0xFF);
			newData[data.length+2] = (byte)((aValue>> 8) & 0xFF);
			newData[data.length+3] = (byte)((aValue>> 0) & 0xFF);
		}
		else {
			newData[data.length+0] = (byte)((aValue>> 0) & 0xFF);
			newData[data.length+1] = (byte)((aValue>> 8) & 0xFF);
			newData[data.length+2] = (byte)((aValue>>16) & 0xFF);
			newData[data.length+3] = (byte)((aValue>>24) & 0xFF);
		}
		data = newData;
		return this;
	}

	public HexString append(HexString hexStr) {
		byte[] bytes = hexStr.toArray();
		byte[] newData = new byte[ data.length + bytes.length ];
		System.arraycopy(data, 0, newData, 0, data.length );
		System.arraycopy(bytes, 0, newData, data.length, bytes.length );
		data = newData;
		return this;
	}
	
	public HexString append(HexString hexStr, int offset, int length) {
		byte[] bytes = hexStr.toArray();
		byte[] newData = new byte[ data.length + length ];
		System.arraycopy(data, 0, newData, 0, data.length );
		System.arraycopy(bytes, offset, newData, data.length, length );
		data = newData;
		return this;
	}

	public HexString appendBytes(byte[] bytes, int offset, int length) {
		byte[] newData = new byte[ data.length + length ];
		System.arraycopy(data, 0, newData, 0, data.length );
		System.arraycopy(bytes, offset, newData, data.length, length );
		data = newData;
		return this;
	}

	public int getUint8(int i) {
		return data[i] & 0xFF;
	}
	public int getBcd(int i) {
		int value = data[i] & 0xFF;
		int res = (value / 16) * 10;
		res += (value % 16);
		return res;
	}
	
	public Integer getUint16(int offset, boolean isBigEndian) {
		int res = 0;
		if( isBigEndian ){
			res = data[offset] & 0xFF;
			res <<= 8;
			res += data[offset+1] & 0xFF;
		}
		else {
			res = data[offset+1] & 0xFF;
			res <<= 8;
			res += data[offset] & 0xFF;
		}
		return res;
	}
	public Integer getBcd16(int offset) {
		int res = 0;
		res = data[offset] & 0xFF;
		res <<= 8;
		res += data[offset+1] & 0xFF;
		return Integer.parseInt(Integer.toHexString(res));
	}
	public Integer getBcd32(int offset) {
		int res = 0;
		res = data[offset] & 0xFF;
		res <<= 8;
		res += data[offset+1] & 0xFF;
		res <<= 8;
		res += data[offset+2] & 0xFF;
		res <<= 8;
		res += data[offset+3] & 0xFF;
		return Integer.parseInt(Integer.toHexString(res));
	}
	public Integer getBcd8(int offset) {
		int res = 0;
		res = data[offset] & 0xFF;
		return Integer.parseInt(Integer.toHexString(res));
	}
	public long getUint64(int offset, boolean isBigEndian) {
		long res = 0;
		if( isBigEndian ){
			res = data[offset] & 0xFF;
			res <<= 8;
			res += data[offset+1] & 0xFF;
			res <<= 8;
			res += data[offset+2] & 0xFF;
			res <<= 8;
			res += data[offset+3] & 0xFF;
			res <<= 8;
			res += data[offset+4] & 0xFF;
			res <<= 8;
			res += data[offset+5] & 0xFF;
			res <<= 8;
			res += data[offset+6] & 0xFF;
			res <<= 8;
			res += data[offset+7] & 0xFF;
		}
		else {
			res = data[offset+7] & 0xFF;
			res <<= 8;
			res += data[offset+6] & 0xFF;
			res <<= 8;
			res += data[offset+5] & 0xFF;
			res <<= 8;
			res += data[offset+4] & 0xFF;
			res <<= 8;
			res += data[offset+3] & 0xFF;
			res <<= 8;
			res += data[offset+2] & 0xFF;
			res <<= 8;
			res += data[offset+1] & 0xFF;
			res <<= 8;
			res += data[offset] & 0xFF;
		}
		return res;
	}
	public int getUint32(int offset, boolean isBigEndian) {
		int res = 0;
		if( isBigEndian ){
			res = data[offset] & 0xFF;
			res <<= 8;
			res += data[offset+1] & 0xFF;
			res <<= 8;
			res += data[offset+2] & 0xFF;
			res <<= 8;
			res += data[offset+3] & 0xFF;
		}
		else {
			res = data[offset+3] & 0xFF;
			res <<= 8;
			res += data[offset+2] & 0xFF;
			res <<= 8;
			res += data[offset+1] & 0xFF;
			res <<= 8;
			res += data[offset] & 0xFF;
		}
		return res;
	}
	public int getSint16(int offset, boolean isBigEndian ) {
		if( isBigEndian ){
			int res = 0;
			res = data[offset];
			res <<= 8;
			res &= ~0xFF;
			res |= data[offset+1] & 0xFF;
			return res;
		}
		else {
			int res = 0;
			res = data[offset+1];
			res <<= 8;
			res &= ~0xFF;
			res |= data[offset] & 0xFF;
			return res;
		}
	}

	public HexString insertUint8(int offset, byte b) {
		
		byte[] newdata = new byte[ data.length + 1 ];
		System.arraycopy(data, 0, newdata, 0, offset);
		newdata[ offset ] = b;
		if( data.length - offset > 0 ){
			System.arraycopy(data, offset, newdata, offset+1, data.length - offset);
		}
		data = newdata;
		return this;
	}

	public static BigDecimal[] toBigDecimals(String value) {
		byte[] bytes = toBytes(value);
		BigDecimal[] res = new BigDecimal[bytes.length];
		for( int i = 0; i < res.length; i++ ){
			res[i] = BigDecimal.valueOf( bytes[i] & 0xFFL );
		}
		return res;
	}

	/**
	 * Converts the Java string to a HexString encoded string.
	 * The byte values are encoded with UTF-8.
	 * <br>
	 * Example: stringToUtf8Hex("ABC") results in "41 42 43" 
	 */
	public static String stringToUtf8Hex(String string) {
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		return toString(bytes);
	}

	public byte[] getSlice(int offset, int length) {
		byte[] res = new byte[length];
		System.arraycopy(data, offset, res, 0, length);
		return res;
	}

   /**
    * Creates a new HexString initializedinstance with the given length
   */
   public static HexString withLength( int length, int byteInitializeValue) {
	   byte[] initializedData = new byte[length];
	   for (int i = 0; i < length; i++) {
		   initializedData[i] = (byte)byteInitializeValue;
	   }
	   return new HexString(initializedData);
   }
   
   /**
    * Copies the stored data and inverts all bits in each databyte, then returns a new instance of HexString
   */
   public HexString invert() {
	   byte[] invertedData = new byte[data.length];
	   
	   for (int i = 0; i < this.data.length; i++) {
		   invertedData[i] = (byte)(~data[i] & 0xFF);
	   }
	   
	   return new HexString(invertedData);
   }

	public static String longToStringBE(long value) {
		return String.format("%02X %02X %02X %02X %02X %02X %02X %02X",
				(value >> 56) & 0xFF, (value >> 48) & 0xFF,
				(value >> 40) & 0xFF, (value >> 32) & 0xFF, 
				(value >> 24) & 0xFF, (value >> 16) & 0xFF,
				(value >>  8) & 0xFF, (value >>  0) & 0xFF);
	}
	public static String longToStringLE(long value) {
		return String.format("%02X %02X %02X %02X %02X %02X %02X %02X",
				(value >>  0) & 0xFF, (value >>  8) & 0xFF,
				(value >> 16) & 0xFF, (value >> 24) & 0xFF, 
				(value >> 32) & 0xFF, (value >> 40) & 0xFF,
				(value >> 48) & 0xFF, (value >> 56) & 0xFF);
	}
	public static String intToStringBE(int value) {
		return String.format("%02X %02X %02X %02X",
				(value >> 24) & 0xFF, (value >> 16) & 0xFF,
				(value >>  8) & 0xFF, (value >>  0) & 0xFF);
	}
	public static String intToStringLE(int value) {
		return String.format("%02X %02X %02X %02X",
				(value >>  0) & 0xFF, (value >>  8) & 0xFF,
				(value >> 16) & 0xFF, (value >> 24) & 0xFF);
	}
	public static String shortToStringBE(short value) {
		return String.format("%02X %02X",
				(value >>  8) & 0xFF, (value >>  0) & 0xFF);
	}
	public static String shortToStringLE(short value) {
		return String.format("%02X %02X",
				(value >>  0) & 0xFF, (value >>  8) & 0xFF);
	}
}
