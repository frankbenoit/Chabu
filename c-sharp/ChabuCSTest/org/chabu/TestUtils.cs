using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace org.chabu
{

    using System.Text;
    using System.Text.RegularExpressions;

    using TextWriter = System.IO.TextWriter;
    using ByteBuffer = System.IO.MemoryStream;
    using RuntimeException = System.SystemException;
    using BufferedReader = System.IO.StreamReader ;

    public class TestUtils {

	    public static String test2Hex( String text ){
		    byte[] bytes = text.getBytes( Encoding.UTF8 );
		    StringBuilder sb = new StringBuilder();
		    for (int i = 0; i < bytes.Length; i++) {
			    if( sb.length() > 0 ) sb.append(" ");
			    sb.append( String.Format("{1:2X}", 0xFF & bytes[i] ));
		    }
		    return sb.toString();
	    }
	
	    public static String test2LengthAndHex( String text ){
		    byte[] bytes = text.getBytes( Encoding.UTF8 );
		    StringBuilder sb = new StringBuilder();
            sb.append(String.Format("{1:2X} {2:2X} {3:2X} {4:2X}", bytes.length() >> 24, bytes.length() >> 16, bytes.length() >> 8, 0xFF & bytes.length()));
		    if( bytes.Length > 0 ){
			
			    for (int i = 0; i < bytes.Length; i++) {
				    sb.append( String.Format(" {0:X2}", 0xFF & bytes[i] ));
			    }
			    int idx = bytes.Length;
			    while( ( idx & 3 ) != 0 ){
				    idx++;
				    sb.append(" 00");
			    }
		    }
		    return sb.toString();
	    }
	
        private static readonly char[] HEX_DIGITS = { (char)'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    
        public static String dumpHexString(byte[] array)
        {
            return dumpHexString(array, 0, array.Length);
        }
        public static String dumpHexString(ByteBuffer bb)
        {
    	    byte[] data = new byte[ bb.remaining() ];
    	    int position = bb.position();
    	    bb.get(data);
    	    bb.position(position);
    	    return dumpHexString(data);
        }
    
        public static String dumpHexString(byte[] array, int offset, int length)
        {
            StringBuilder result = new StringBuilder();
        
            char[] line = new char[16];
            int lineIndex = 0;
        
            result.append("\n0x");
            result.append(toHexString(offset));
        
            for (int i = offset ; i < offset + length ; i++)
            {
                if (lineIndex == 16)
                {
                    result.append(" ");
                
                    for (int j = 0 ; j < 16 ; j++)
                    {
                        if (line[j] > ' ' && line[j] < '~')
                        {
                            result.append(new String(line, j, 1));
                        }
                        else
                        {
                            result.append(".");
                        }
                    }
                
                    result.append("\n0x");
                    result.append(toHexString(i));
                    lineIndex = 0;
                }
            
                byte b = array[i];
                result.append(" ");
                result.append(HEX_DIGITS[(b >> 4) & 0x0F]);
                result.append(HEX_DIGITS[b & 0x0F]);
            
                line[lineIndex++] = (char)b;
            }
        
            if (lineIndex != 16)
            {
                int count = (16 - lineIndex) * 3;
                count++;
                for (int i = 0 ; i < count ; i++)
                {
                    result.append(" ");
                }
            
                for (int i = 0 ; i < lineIndex ; i++)
                {
                    if (line[i] > ' ' && line[i] < '~')
                    {
                        result.append(new String(line, i, 1));
                    }
                    else
                    {
                        result.append(".");
                    }
                }
            }
        
            return result.toString();
        }
    
        public static String toHexString(byte b)
        {
            return toHexString(toByteArray(b));
        }

        public static String toHexString(byte[] array)
        {
            return toHexString(array, 0, array.Length);
        }
    
        public static String toHexString(byte[] array, int offset, int length)
        {
            char[] buf = new char[length * 2];

            int bufIndex = 0;
            for (int i = offset ; i < offset + length; i++) 
            {
                byte b = array[i];
                buf[bufIndex++] = HEX_DIGITS[(b >> 4) & 0x0F];
                buf[bufIndex++] = HEX_DIGITS[b & 0x0F];
            }

            return new String(buf);        
        }
    
        public static String toHexString(int i)
        {
            return toHexString(toByteArray(i));
        }
    
        public static byte[] toByteArray(byte b)
        {
            byte[] array = new byte[1];
            array[0] = b;
            return array;
        }
    
        public static byte[] toByteArray(int i)
        {
            byte[] array = new byte[4];
        
            array[3] = (byte)(i & 0xFF);
            array[2] = (byte)((i >> 8) & 0xFF);
            array[1] = (byte)((i >> 16) & 0xFF);
            array[0] = (byte)((i >> 24) & 0xFF);
        
            return array;
        }
    
        private static int toByte(char c)
        {
            if (c >= '0' && c <= '9') return (c - '0');
            if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
            if (c >= 'a' && c <= 'f') return (c - 'a' + 10);

            throw new RuntimeException ("Invalid hex char '" + c + "'");
        }
    
        public static byte[] hexStringToByteArray(String hexString)
        {
            int length = hexString.length();
            byte[] buffer = new byte[length / 2];

            for (int i = 0 ; i < length ; i += 2)
            {
                buffer[i / 2] = (byte)((toByte(hexString.charAt(i)) << 4) | toByte(hexString.charAt(i+1)));
            }
        
            return buffer;
        }

	    public static void ensure(bool b) {
		    if( b ) return;
		    throw new RuntimeException();
	    }    
    }
}