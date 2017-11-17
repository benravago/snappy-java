package snappy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintStream;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ByteCase {

    static void cmp(byte[] a, byte[] b) {
        cmp(a,0,a.length,b,0,b.length);
    }    
    static void cmp(byte[] a, byte[] b, int b_off, int b_len) {
        cmp(a,0,a.length,b,b_off,b_len);
    }
    static void cmp(byte[] a, int a_off, int a_len, byte[] b) {
        cmp(a,a_off,a_len,b,0,b.length);
    }

    static void cmp(byte[] a, int a_off, int a_len, byte[] b, int b_off, int b_len) {
        if (a == b && a_off == b_off && a_len == b_len) {
            return;
        }
        if (a_len != b_len) {
            fail("got "+a_len+" bytes, want "+b_len);
        }
        while (a_len-- > 0) {
            if (a[a_off] != b[b_off]) {
                fail("byte a["+a_off+"]: got "+hex(a[a_off])+", want "+hex(b[b_off]));
            }
            a_off++; b_off++;
        }
    }

    static String hex(byte b) { return new String(new byte[]{hex[(b>>4)&0x0f],hex[b&0x0f]}); }
    static byte[] hex = {0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0x61,0x62,0x63,0x64,0x65,0x66};

    static void dump(byte[] b) {
        dump(b,0,b.length);
    }

    static void dump(byte[] b, int off, int len) {
        dump(System.out,b,off,len);
    }

    static void dump(PrintStream out, byte[] b, int off, int len) {
        byte[] a = new byte[80];
        int limit = off + len;
        int p = off & 0x0f;
        while (off < limit) {
            Arrays.fill(a,(byte)0x20);
            a[60+p] = '|';
            int i = 8;
            int j = off & 0x7ffffff0;
            while (i > 0) {
                a[--i] = hex[j&0x0f]; j >>>= 4;
            }
            while (p < 16 && off < limit) {
                i = p * 3 + (p < 8 ? 0 : 1);
                j = b[off] & 0x0ff;
                a[10+i] = hex[j>>>4];
                a[11+i] = hex[j&0x0f];
                a[61+p] = (byte)(j < 0x20 ? '.' : j > 0x7e ? '.' : j);
                p++; off++;
            }
            j = 61+p;
            a[j] = '|';
            a[j+1] = '\n';
            out.write(a,0,j+2);
            p = 0;
        }
    }

    @Test
    void test() {
        byte[] a = new byte[] {0,0,0,1,2,3,4,5,0,0,0};
        byte[] b = new byte[] {9,9,1,2,3,4,5,9,9};
        cmp(a,3,5,b,2,5);
        dump(a);
    }

}
