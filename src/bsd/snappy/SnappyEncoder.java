package bsd.snappy;

/**
 * An implementation of the 'snappy' compression algorithm
 * based on the go-lang source code at https://github.com/google/snappy
 * <p>
 * This implementation only supports the 'Literal',
 * 'Copy with 1-byte offset', and 'Copy with 2-byte offset' tags.
 */
@bsd.LICENSE
public class SnappyEncoder {

    // maxBlockSize is the maximum size of the input to encodeBlock.
    static final int maxBlockSize = 65536; // 0x010000

    // inputMargin is the minimum number of extra input bytes to keep, inside encodeBlock's inner loop. 
    static final int inputMargin = 16 - 1;
    
    // minNonLiteralBlockSize is the minimum size of the input to encodeBlock that could be encoded with a copy tag.
    static final int minNonLiteralBlockSize = 1 + 1 + inputMargin;

    public static ByteSequence encode(byte[] b) {
        return encode(b,0,b.length);
    }
    
    /**
     * Encode returns the encoded form of src.
     * 
     * @param src - the input data bytes
     * @param srcPos - the start offset of the input data
     * @param len - the length of the input data
     * @return a ByteSequence that holds the entire encoded block
     */
    public static ByteSequence encode(byte[] src, int srcPos, int len) {

        final int srcEnd = srcPos + len;

        // Calculate the maximum length of a snappy block, given its uncompressed length.
        final int maxEncodedLen = 32 + len + len/6;
        
        // allocate the encode workarea
        byte[] dst = new byte[maxEncodedLen];
        int dstPos = 0;

        // The block starts with the varint-encoded length of the decompressed bytes.
        long v = len;
        while (v > 0x07fL) {
            dst[dstPos++] = (byte)( (v & 0x07fL) | 0x080L );
            v >>>= 7;
        }
        dst[dstPos++] = (byte) v;

        while (srcPos < srcEnd) {
            len = srcEnd - srcPos;
            if (len > maxBlockSize) {
                len = maxBlockSize;
            }
            if (len < minNonLiteralBlockSize) {
                dstPos = emitLiteral(src,srcPos,dst,dstPos,len);
            } else {
                dstPos = encodeBlock(src,srcPos,dst,dstPos,len);
            }
            srcPos += len;
        }
        return new ByteSequence(dst,0,dstPos);
    }

    static int emitLiteral(byte[] src, int srcPos, byte[] dst, int dstPos, int len) {
        assert (len < maxBlockSize);
        int n = len - 1;
        if (n < 60) {
            dst[dstPos++] = (byte)(n << 2);
        } else if (n < 0x100) { // 1 << 8
            dst[dstPos++] = (byte) 0xf0; // 60 << 2 | 0x00
            dst[dstPos++] = (byte) n;
        } else {
            dst[dstPos++] = (byte) 0xf4; // 61 << 2 | 0x00
            dst[dstPos++] = (byte) n;
            dst[dstPos++] = (byte)(n >> 8);
        }
        System.arraycopy(src,srcPos,dst,dstPos,len);
        return dstPos + len;
    }

    static int emitCopy(byte[] dst, int dstPos, int off, int len) {
        while (len >= 68) {
            dst[dstPos++] = (byte) 0x0fe; // 63 << 2 | 0x02
            dst[dstPos++] = (byte) off;
            dst[dstPos++] = (byte)(off >> 8);
            len -= 64;
        }
        if (len > 64) {
            dst[dstPos++] = (byte) 0X0ee; // 59 << 2 | 0x02
            dst[dstPos++] = (byte) off;
            dst[dstPos++] = (byte)(off >> 8);
            len -= 60;
        }
        if (len >= 12 || off > 0x07ff) { // >= 2048
            dst[dstPos++] = (byte)( ((len-1) << 2) | 0x02 );
            dst[dstPos++] = (byte) off;
            dst[dstPos++] = (byte)(off >> 8);
        } else {
            dst[dstPos++] = (byte)( ((off >> 3) & 0x0e0) | ((len-4) << 2) | 0x01 );
            dst[dstPos++] = (byte) off;
        }
        return dstPos;
    }

    static final int maxTableSize = 1 << 14;
    static final int tableMask = maxTableSize - 1;

    static int encodeBlock(byte[] src, int srcPos, byte[] dst, int dstPos, int len) {

        int shift = 32 - 8;
        for (int tableSize = 1 << 8; tableSize < maxTableSize && tableSize < len; tableSize <<= 1) {
            shift--;
        }
        short[] table = new short[maxTableSize];

        int sLimit = len - inputMargin;

        int nextEmit = 0;

        int s = 1;
        int nextHash = hash( load32(src,srcPos+s), shift );

        A: for(;;) {
            int skip = 32; // 1 << 5

            int nextS = s;
            int candidate = 0;
            B: for(;;) {
                s = nextS;
                int bytesBetweenHashLookups = skip >> 5;
                nextS = s + bytesBetweenHashLookups;
                skip += bytesBetweenHashLookups;
                if (nextS > sLimit) {
                    break A; // goto emitRemainder
                }
                candidate =  table[ nextHash & tableMask ] & 0x0ffff;
                table[ nextHash & tableMask ] = (short)(s);
                nextHash = hash( load32(src,srcPos+nextS), shift );
                if (load32(src,srcPos+s) == load32(src,srcPos+candidate)) {
                    break B;
                }
            } // B:

            dstPos = emitLiteral( src, srcPos+nextEmit, dst, dstPos, s-nextEmit ); // d += emitLiteral(dst[d:], src[nextEmit:s])

            C: for(;;) {
                int base = s;

                s += 4;
                int i = srcPos + candidate + 4;
                int j = srcPos + s;
                while (s < len && src[i] == src[j]) { i++; j++; s++; }

                dstPos = emitCopy( dst, dstPos, base-candidate, s-base ); // d += emitCopy(dst[d:], base-candidate, s-base)
                nextEmit = s;
                if (s >= sLimit) {
                    break A; // goto emitRemainder
                }

                long x = load64(src,srcPos+s-1);
                int prevHash = hash( (int)(x>>0), shift );
                table[ prevHash & tableMask ] = (short)(s - 1);
                int currHash = hash( (int)(x>>8), shift );
                candidate = table[ currHash & tableMask ] & 0x0ffff;
                table[ currHash & tableMask ] = (short)(s);
                if ( (int)(x>>8) != load32(src,srcPos+candidate)) {
                    nextHash = hash( (int)(x>>16), shift );
                    s++;
                    break C;
                }
            } // C:
        } // A:

        // emitRemainder:
        if (nextEmit < len) {
            dstPos = emitLiteral( src, srcPos+nextEmit, dst, dstPos, len-nextEmit ); // d += emitLiteral(dst[d:], src[nextEmit:])
        }
        return dstPos;
    }

    static int hash(int u, int shift) {
        return (u * 0x1e35a7bd) >>> shift;
    }

    static int load32(byte[] b, int i) {
        return (b[i] & 0x0ff) | ((b[i+1] & 0x0ff) << 8) | ((b[i+2] & 0x0ff) << 16) | ((b[i+3] & 0x0ff) << 24);
    }
    static long load64(byte[] b, int i) {
        return (b[i] & 0x0ffL) | ((b[i+1] & 0x0ffL) << 8) | ((b[i+2] & 0x0ffL) << 16) | ((b[i+3] & 0x0ffL) << 24) |
              ((b[i+4] & 0x0ffL) << 32) | ((b[i+5] & 0x0ffL) << 40) | ((b[i+6] & 0x0ffL) << 48) | ((b[i+7] & 0x0ffL) << 56);
    }

}
