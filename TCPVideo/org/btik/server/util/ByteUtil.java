package org.btik.server.util;

/**
 * @author lustre
 * @version 1.0
 * @since 2021/5/15 12:37
 * 字节工具
 */
public class ByteUtil {
    private static final char[] DIGEST = {
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F'
    };

    private static final byte[] LOW_DIGEST = {
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    private static final byte[] UP_DIGEST = {
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F'
    };

    private static final byte[] DigitTens = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    };

    private static final byte[] DigitOnes = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    };

    public static String toHexString(byte[] toByteArray) {

        if (null == toByteArray || toByteArray.length == 0) {
            return null;
        }
        byte[] bytes = toHexBytes(toByteArray);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    /**
     * 用于直接发送IO时使用，发送的也是十六进制字符串。
     * 在不需要使用String相关的函数时，在io层面也是toHexString的含义
     */
    public static byte[] toHexBytes(byte[] bytes) {
        if (null == bytes || bytes.length == 0) {
            return null;
        }
        byte[] result = new byte[bytes.length << 1];
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            result[2 * i] = UP_DIGEST[(b & 0xf0) >> 4];
            result[2 * i + 1] = UP_DIGEST[b & 0xf];
        }
        return result;
    }

    public static String toHexString(byte[] toByteArray, int offset, int length) {

        if (null == toByteArray || toByteArray.length == 0) {
            return null;
        }
        char[] result = new char[length << 1];
        for (int i = offset; i < length; i++) {
            byte b = toByteArray[i];
            result[2 * i] = DIGEST[(b & 0xf0) >> 4];
            result[2 * i + 1] = DIGEST[b & 0xf];
        }
        return new String(result);
    }

    public static byte[] toHexString(int num) {
        int mag = Integer.SIZE - Integer.numberOfLeadingZeros(num);
        int len = Math.max(((mag + (4 - 1)) / 4), 1);
        byte[] result = new byte[len];
        int charPos = len;
        int mask = 0xf;
        do {
            result[--charPos] = LOW_DIGEST[num & mask];
            num >>>= 4;
        } while (charPos > 0);
        return result;
    }

    public static byte[] toHexString(long val) {
        int mag = Long.SIZE - Long.numberOfLeadingZeros(val);
        int len = Math.max(((mag + (4 - 1)) / 4), 1);
        byte[] buf = new byte[len];

        int charPos = len;
        int mask = 0xf;
        do {
            buf[--charPos] = LOW_DIGEST[((int) val) & mask];
            val >>>= 4;
        } while (charPos > 0);

        return buf;
    }

    public static byte[] toFullHexString(long val) {
        byte[] buf = new byte[8 << 1];
        int charPos = buf.length;
        int mask = 0xf;
        do {
            buf[--charPos] = LOW_DIGEST[((int) val) & mask];
            val >>>= 4;
        } while (charPos > 0);

        return buf;
    }

    public static int getChars(long i, int index, byte[] buf) {
        long q;
        int r;
        int charPos = index;

        boolean negative = (i < 0);
        if (!negative) {
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i <= Integer.MIN_VALUE) {
            q = i / 100;
            r = (int) ((q * 100) - i);
            i = q;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        return fillCharBuf(buf, charPos, negative, (int) i);
    }

    private static int fillCharBuf(byte[] buf, int charPos, boolean negative, int i) {
        int q;
        int r;
        while (i <= -100) {
            q = i / 100;
            r = (q * 100) - i;
            i = q;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // We know there are at most two digits left at this point.
        q = i / 10;
        r = (q * 10) - i;
        buf[--charPos] = (byte) ('0' + r);

        // Whatever left is the remaining digit.
        if (q < 0) {
            buf[--charPos] = (byte) ('0' - q);
        }

        if (negative) {
            buf[--charPos] = (byte) '-';
        }
        return charPos;
    }

    public static int stringSize(long x) {
        int d = 1;
        if (x >= 0) {
            d = 0;
            x = -x;
        }
        long p = -10;
        for (int i = 1; i < 19; i++) {
            if (x > p)
                return i + d;
            p = 10 * p;
        }
        return 19 + d;
    }

    public static byte[] toString(long i) {
        int size = stringSize(i);
        byte[] buf = new byte[size];
        getChars(i, size, buf);
        return buf;
    }

    static int getChars(int i, int index, byte[] buf) {

        boolean negative = i < 0;
        if (!negative) {
            i = -i;
        }
        return fillCharBuf(buf, index, negative, i);
    }

    private static int fillCharBuf(int i, byte[] buf, int charPos, boolean negative) {
        return fillCharBuf(buf, charPos, negative, i);
    }

    static int stringSize(int x) {
        int d = 1;
        if (x >= 0) {
            d = 0;
            x = -x;
        }
        int p = -10;
        for (int i = 1; i < 10; i++) {
            if (x > p)
                return i + d;
            p = 10 * p;
        }
        return 10 + d;
    }

    public static byte[] toString(int i) {
        int size = stringSize(i);
        byte[] buf = new byte[size];
        getChars(i, size, buf);
        return buf;
    }

    public static int ipToInt(byte[] ip) {
        if (ip.length != 4) {
            return 0;
        }
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (ip[i] & 0xff) << ((3 - i) * 8);
        }
        return result;
    }

    /**
     * ipv4 to String
     *
     * @param ip 不能为空，不能为0长
     * @throws IndexOutOfBoundsException 当为0长时
     * @throws NullPointerException      当为null时
     */
    public static String ipv42Str(byte[] ip) {
        StringBuilder ipStr = new StringBuilder();
        int top = ip.length - 1;
        for (int i = 0; i < top; i++) {
            ipStr.append(0xff & ip[i])
                    .append('.');
        }
        ipStr.append(ip[top] & 0xff);
        return ipStr.toString();
    }

    /**
     * 字节数组比较
     *
     * @param bytes1  字节数组1
     * @param offset1 字节数组1的偏移量
     * @param bytes2  字节数组2
     * @param offset2 字节数组2的偏移量
     * @param len     比较的长度,当然不含偏移量
     * @return 片段是否相等
     */
    public static boolean equals(byte[] bytes1, int offset1, byte[] bytes2, int offset2, int len) {
        if (bytes1.length - offset1 < len || bytes2.length - offset2 < len) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (bytes1[offset1 + i] != bytes2[offset2 + i]) {
                return false;
            }
        }
        return true;
    }


}
