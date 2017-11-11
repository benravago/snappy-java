package snappy;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import static snappy.SnappyTest.*;

class T1 {

    static String in =
        "Wikipedia Encyclopedia Encyclical\n";

    static byte[] out = {
        0x22, 0x40, 0x57, 0x69, 0x6b, 0x69, 0x70, 0x65,
        0x64, 0x69, 0x61, 0x20, 0x45, 0x6e, 0x63, 0x79,
        0x63, 0x6c, 0x6f, 0x2e, 0x0d, 0x00, 0x10, 0x69,
        0x63, 0x61, 0x6c, 0x0a,
    };
    
    @Test
    void test() {
        byte[] b = in.getBytes();
        roundtrip(b);
    }

}

// https://golang.org/pkg/testing/
