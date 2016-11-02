/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <fr@nk-benoit.de>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
using System;

namespace Org.Chabu.Prot.V1.Internal
{
    using global::System.Text;
    using ByteBuffer = Org.Chabu.Prot.Util.ByteBuffer;

    public class TestUtils {

        private readonly static char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        public static String text2Hex(String text) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.Length; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(String.Format("{0:X2}", 0xFF & bytes[i]));
            }
            return sb.toString();
        }

        public static String text2LengthAndHex(String text) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            sb.append(String.Format("{0:X2} {1:X2} {2:X2} {3:X2}", bytes.Length >> 24, bytes.Length >> 16, bytes.Length >> 8, 0xFF & bytes.Length));
            if (bytes.Length > 0) {

                for (int i = 0; i < bytes.Length; i++) {
                    sb.append(String.Format(" {0:X2}", 0xFF & bytes[i]));
                }
                int idx = bytes.Length;
                while ((idx & 3) != 0) {
                    idx++;
                    sb.append(" 00");
                }
            }
            return sb.toString();
        }


        public static String dumpHexString(byte[] array) {
            return dumpHexString(array, 0, array.Length);
        }
        public static String dumpHexString(ByteBuffer bb) {
            byte[] data = new byte[bb.remaining()];
            int position = bb.position();
            bb.get(data);
            bb.position(position);
            return dumpHexString(data);
        }

        public static String dumpHexString(byte[] array, int offset, int length)
        {
            if (length == 0) return "-- data length 0 --";
            StringBuilder result = new StringBuilder();

            byte[] line = new byte[16];
            int lineIndex = 0;

            for (int i = offset; i < offset + length; i++) {
                if ((lineIndex % 16) == 0) {
                    result.append("\n0x");
                    result.append(toHexString(i));
                }
                byte b = array[i];
                result.append(" ");
                result.append(HEX_DIGITS[(b >> 4) & 0x0F]);
                result.append(HEX_DIGITS[b & 0x0F]);

                line[lineIndex] = b;

                if ((lineIndex % 16) == 15) {
                    result.append(" ");

                    for (int j = 0; j < 16; j++) {
                        if (line[j] > ' ' && line[j] < '~') {
                            result.append((char)line[j]);
                        }
                        else {
                            result.append(".");
                        }
                    }

                }
                lineIndex++;
                if (lineIndex >= 16) {
                    lineIndex = 0;
                }
            }

            if (lineIndex != 16) {
                int count = (16 - lineIndex) * 3;
                count++;
                for (int i = 0; i < count; i++) {
                    result.append(" ");
                }

                for (int i = 0; i < lineIndex; i++) {
                    if (line[i] > ' ' && line[i] < '~') {
                        result.append((char)line[i]);
                    }
                    else {
                        result.append(".");
                    }
                }
            }

            return result.toString();
        }

        public static String toHexString(byte b) {
            return toHexString(toByteArray(b), false);
        }

        public static String toHexString(byte[] array, bool withSpaces) {
            return toHexString(array, 0, array.Length, withSpaces);
        }

        public static String toHexString(ByteBuffer bb, bool withSpaces) {
            byte[] data = new byte[bb.remaining()];
            int position = bb.position();
            bb.get(data);
            bb.position(position);
            return toHexString(data, withSpaces);
        }

        public static String toHexString(byte[] array, int offset, int length, bool withSpaces) {

            if (length == 0) {
                return "";
            }
            if (withSpaces) {
                char[] buf = new char[length * 2 + length - 1];

                int bufIndex = 0;
                for (int i = offset; i < offset + length; i++) {
                    if (i > offset) {
                        buf[bufIndex++] = ' ';
                    }
                    byte b = array[i];
                    buf[bufIndex++] = HEX_DIGITS[(b >> 4) & 0x0F];
                    buf[bufIndex++] = HEX_DIGITS[b & 0x0F];
                }
                return new String(buf);
            }
            else {
                char[] buf = new char[length * 2];

                int bufIndex = 0;
                for (int i = offset; i < offset + length; i++) {
                    byte b = array[i];
                    buf[bufIndex++] = HEX_DIGITS[(b >> 4) & 0x0F];
                    buf[bufIndex++] = HEX_DIGITS[b & 0x0F];
                }
                return new String(buf);
            }
        }

        public static String toHexString(int i) {
            return toHexString(toByteArray(i), false);
        }

        public static byte[] toByteArray(byte b) {
            byte[] array = new byte[1];
            array[0] = b;
            return array;
        }

        public static byte[] toByteArray(int i) {
            byte[] array = new byte[4];

            array[3] = (byte)(i & 0xFF);
            array[2] = (byte)((i >> 8) & 0xFF);
            array[1] = (byte)((i >> 16) & 0xFF);
            array[0] = (byte)((i >> 24) & 0xFF);

            return array;
        }

        private static int toByte(char c) {
            if (c >= '0' && c <= '9') return (c - '0');
            if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
            if (c >= 'a' && c <= 'f') return (c - 'a' + 10);

            throw new SystemException("Invalid hex char '" + c + "'");
        }

        public static byte[] hexStringToByteArray(String hexString) {
            String[] parts = hexString.Split(' ');
            int length = parts.Length;
            byte[] buffer = new byte[length];

            for (int i = 0; i < length; i++) {
                buffer[i] = (byte)((toByte(parts[i].charAt(0)) << 4) | toByte(parts[i].charAt(1)));
            }

            return buffer;
        }

        public static void ensure(bool b) {
            if (b) return;
            throw new SystemException();
        }

        public static String getChabuVersionAsHex() {
            return String.Format("{0:X2} {1:X2} {2:X2} {3:X2} ",
                    (Constants.PROTOCOL_VERSION >> (8 * 3)) & 0xFF,
                    (Constants.PROTOCOL_VERSION >> (8 * 2)) & 0xFF,
                    (Constants.PROTOCOL_VERSION >> (8 * 1)) & 0xFF,
                    (Constants.PROTOCOL_VERSION >> (8 * 0)) & 0xFF);
        }
    }
}