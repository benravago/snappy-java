package snappy;

import bsd.snappy.ByteSequence;
import bsd.snappy.SnappyDecoder;
import bsd.snappy.SnappyEncoder;

class SnappyTest extends ByteCase {

    static void roundtrip(byte[] b) {
        roundtrip(b,0,b.length);
    }
    
    static void roundtrip(byte[] b, int off, int len) {
        ByteSequence ebuf = SnappyEncoder.encode(b,off,len);
        // dump(ebuf.data,ebuf.offset,ebuf.length);
        ByteSequence dbuf = SnappyDecoder.decode(ebuf.data,ebuf.offset,ebuf.length);
        // dump(dbuf.data,dbuf.offset,dbuf.length);
        cmp(dbuf.data,dbuf.offset,dbuf.length,b,off,len);
    }
    
    /*
    func roundtrip(b, ebuf, dbuf []byte) error {
        d, err := Decode(dbuf, Encode(ebuf, b))
        if err != nil {
            return fmt.Errorf("decoding error: %v", err)
        }
        if err := cmp(d, b); err != nil {
            return fmt.Errorf("roundtrip mismatch: %v", err)
        }
        return nil
    }
    */

}
