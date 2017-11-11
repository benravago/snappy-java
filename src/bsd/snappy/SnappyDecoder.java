package bsd.snappy;

import java.util.Arrays;

/**
 * An implementation of the 'snappy' decompression algorithm
 * based on the go-lang source code at https://github.com/google/snappy
 * <p>
 * This implementation only supports the 'Literal',
 * 'Copy with 1-byte offset', and 'Copy with 2-byte offset' tags.
 */
@bsd.LICENSE
public class SnappyDecoder {

    public static ByteSequence decode(byte[] b) {
        return decode(b,0,b.length);
    }
    
    /**
     * Decode returns the decoded form of src.
     * 
     * @param src - the input data bytes
     * @param srcPos - the start offset of the input data
     * @param len - the length of the input data
     * @return a ByteSequence that holds the entire decoded block
     */
    public static ByteSequence decode(byte[] src, int srcPos, int len) {

        final int srcEnd = srcPos + len;

        // Read the varint-encoded length of the decompressed bytes.
        long v = 0;
        int s = 0;
        byte b;
        do {
            b = src[srcPos++];
            v |= (b & 0x07fL) << s;
            s += 7;
        } while (b < 0);
        assert (s < 64);

        // allocate the decode workarea
        final int uncompressedLength = (int)v;
        byte[] dst = new byte[uncompressedLength];
        int dstPos = 0;

        while (srcPos < srcEnd) {
            s = src[srcPos++] & 0x0ff;
            int tag = s & 0x03;
            if (tag == 0x00) { // Literal
                if (s < 0x0f0) { // 60 << 2
                    len = s >> 2;
                } else {
                    len = 0;
                    if (s > 0x0f8) len |= (src[srcPos++] & 0x0ff) << 24; // 62 << 2
                    if (s > 0x0f4) len |= (src[srcPos++] & 0x0ff) << 16; // 61 << 2
                    if (s > 0x0f0) len |= (src[srcPos++] & 0x0ff) << 8;  // 60 << 2
                                   len |= (src[srcPos++] & 0x0ff);
                }
                len += 1;
                System.arraycopy(src,srcPos,dst,dstPos,len);
                srcPos += len;
                dstPos += len;
            } else {
                int off;
                if (tag == 0x01) { // Copy with 1-byte offset
                    len = ((s >> 2) & 0x0007) + 4;
                    off = ((s << 3) & 0x0700) | src[srcPos++] & 0x0ff;
                } else {
                    if (tag == 0x02) { // Copy with 2-byte offset
                        len = ((s >> 2) & 0x03f) + 1;
                        off = (src[srcPos++] & 0x0ff) | ((src[srcPos++] & 0x0ff) << 8);
                    } else {
                        throw new IllegalArgumentException("invalid tag "+s+" at "+srcPos);
                    }
                }
                // Copy from an earlier sub-slice of dst to a later sub-slice.
                if (off > len) {
                    System.arraycopy(dst,dstPos-off,dst,dstPos,len);
                    dstPos += len;
                } else {
                    // Slices overlap; copy forward like RLE (run-length encoding).
                    if (off == 1) {
                        off = dstPos;
                        dstPos += len;
                        Arrays.fill(dst,off,dstPos,dst[off-1]);
                    } else {
                        off = dstPos - off;
                        len = dstPos + len;
                        while (dstPos < len) {
                            dst[dstPos++] = dst[off++];
                        }
                    }
                }
            }
        }
        assert (dstPos == uncompressedLength);
        return new ByteSequence(dst,0,dstPos);
    }

}
