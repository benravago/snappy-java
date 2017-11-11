package bsd.snappy;

/**
 * A ByteSequence is a tuple of a readable sequence of byte values. 
 */
public class ByteSequence {

    public final byte[] data;
    public final int offset;
    public final int length;

    public ByteSequence(byte[] b, int off, int len) {
        data = b; offset = off; length = len;
    }
}
