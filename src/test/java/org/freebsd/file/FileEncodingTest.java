package org.freebsd.file;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileEncodingTest {

    private FileEncoding rule;

    @Before
    public void initFields() {
        rule = new FileEncoding();
    }

    @Test
    public void detectAscii() {
        assertEncoding(
                new byte[] {'a', 'b', 'c', 'A', 'B', 'C', '1', '2', '3'}, StandardCharsets.US_ASCII.name(), "ASCII");
    }

    @Test
    public void detectISO() {
        assertEncoding(
                new byte[] {'a', 'b', 'c', 'A', 'B', 'C', '1', '2', '3', (byte) 0xF7},
                StandardCharsets.ISO_8859_1.name(),
                "ISO-8859");
    }

    @Test
    public void detectUTF7() {
        assertEncoding(new byte[] {'+', '/', 'v', '8', 'B', 'C', '1', '2', '3'}, "UTF-7", "UTF-7 UNICODE");
        assertEncoding(new byte[] {'+', '/', 'v', '9', 'B', 'C', '1', '2', '3'}, "UTF-7", "UTF-7 UNICODE");
        assertEncoding(new byte[] {'+', '/', 'v', '+', 'B', 'C', '1', '2', '3'}, "UTF-7", "UTF-7 UNICODE");
        assertEncoding(new byte[] {'+', '/', 'v', '/', 'B', 'C', '1', '2', '3'}, "UTF-7", "UTF-7 UNICODE");
    }

    @Test
    public void detectUTF8() {
        assertEncoding(
                new byte[] {'a', 'b', 'c', 'A', 'B', 'C', '1', '2', '3', (byte) 0xC3, (byte) 0xB6},
                StandardCharsets.UTF_8.name(),
                "UTF-8 UNICODE");
    }

    @Test
    public void detectUTF8WithBoom() {
        assertEncoding(
                new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b', 'c', 'A', 'B', 'C', '1', '2', '3'},
                StandardCharsets.UTF_8.name(),
                "UTF-8 UNICODE (WITH BOM)");
        assertEncoding(
                new byte[] {
                    (byte) 0xEF,
                    (byte) 0xBB,
                    (byte) 0xBF,
                    'a',
                    'b',
                    'c',
                    'A',
                    'B',
                    'C',
                    '1',
                    '2',
                    '3',
                    (byte) 0xC3,
                    (byte) 0xB6
                },
                StandardCharsets.UTF_8.name(),
                "UTF-8 UNICODE (WITH BOM)");
    }

    @Test
    public void detectUTF16LE() {
        assertEncoding(
                new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0xD6, (byte) 0x00, (byte) 0x41, (byte) 0x00},
                StandardCharsets.UTF_16LE.name(),
                "LITTLE-ENDIAN UTF-16 UNICODE");
    }

    @Test
    public void detectUTF16BE() {
        assertEncoding(
                new byte[] {(byte) 0xFE, (byte) 0xFF, (byte) 0x00, (byte) 0xD6, (byte) 0x00, (byte) 0x41},
                StandardCharsets.UTF_16BE.name(),
                "BIG-ENDIAN UTF-16 UNICODE");
    }

    @Test
    public void detectExtendedAscii() {
        assertEncoding(
                new byte[] {'a', 'b', 'c', 'A', 'B', 'C', '1', '2', '3', (byte) 0x96},
                "UNKNOWN-8BIT",
                "NON-ISO EXTENDED-ASCII");
    }

    private void assertEncoding(byte[] data, String codeMime, String code) {
        rule.guessFileEncoding(data);
        assertEquals(codeMime, rule.getCodeMime().toUpperCase());
        assertEquals(code, rule.getCode().toUpperCase());
    }
}
