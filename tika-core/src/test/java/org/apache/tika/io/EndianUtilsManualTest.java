/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class EndianUtilsManualTest {

    private static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            b[i] = (byte) values[i];
        }
        return b;
    }

    private static ByteArrayInputStream stream(int... values) {
        return new ByteArrayInputStream(bytes(values));
    }

    
    @Test
    public void testReadShortBE() throws Exception {
        assertEquals(258, EndianUtils.readShortBE(stream(0x01, 0x02)));
        assertEquals(-2, EndianUtils.readShortBE(stream(0xFF, 0xFE)));
        assertEquals(0, EndianUtils.readShortBE(stream(0x00, 0x00)));
    }

    @Test
    public void testReadShortLESigned() throws Exception {
        assertEquals(-1, EndianUtils.readShortLE(stream(0xFF, 0xFF)));
        assertEquals(0, EndianUtils.readShortLE(stream(0x00, 0x00)));
    }

    @Test
    public void testReadUShortBE() throws Exception {
        assertEquals(258, EndianUtils.readUShortBE(stream(0x01, 0x02)));
        assertEquals(65535, EndianUtils.readUShortBE(stream(0xFF, 0xFF)));
        assertEquals(0, EndianUtils.readUShortBE(stream(0x00, 0x00)));
    }

    @Test
    public void testReadUShortLEZero() throws Exception {
        assertEquals(0, EndianUtils.readUShortLE(stream(0x00, 0x00)));
    }

    @Test
    public void testShortUnderrun() {
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUShortBE(stream(0x01)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUShortBE(stream()));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readShortBE(stream(0x01)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUShortLE(stream()));
    }


    @Test
    public void testReadUIntLE() throws Exception {
        assertEquals(67305985L, EndianUtils.readUIntLE(stream(1, 2, 3, 4)));
        assertEquals(4294967295L, EndianUtils.readUIntLE(stream(0xFF, 0xFF, 0xFF, 0xFF)));
        assertEquals(0L, EndianUtils.readUIntLE(stream(0, 0, 0, 0)));
    }

    @Test
    public void testReadUIntBE() throws Exception {
        assertEquals(16909060L, EndianUtils.readUIntBE(stream(1, 2, 3, 4)));
        assertEquals(4294967295L, EndianUtils.readUIntBE(stream(0xFF, 0xFF, 0xFF, 0xFF)));
        assertEquals(0L, EndianUtils.readUIntBE(stream(0, 0, 0, 0)));
    }

    @Test
    public void testReadIntLE() throws Exception {
        assertEquals(67305985, EndianUtils.readIntLE(stream(1, 2, 3, 4)));
        assertEquals(-1, EndianUtils.readIntLE(stream(0xFF, 0xFF, 0xFF, 0xFF)));
        assertEquals(0, EndianUtils.readIntLE(stream(0, 0, 0, 0)));
    }

    @Test
    public void testReadIntBE() throws Exception {
        assertEquals(16909060, EndianUtils.readIntBE(stream(1, 2, 3, 4)));
        assertEquals(-1, EndianUtils.readIntBE(stream(0xFF, 0xFF, 0xFF, 0xFF)));
        assertEquals(0, EndianUtils.readIntBE(stream(0, 0, 0, 0)));
    }

    @Test
    public void testReadIntME() throws Exception {
        assertEquals(0x02010403, EndianUtils.readIntME(stream(1, 2, 3, 4)));
        assertEquals(0, EndianUtils.readIntME(stream(0, 0, 0, 0)));
    }

    @Test
    public void testIntUnderrun() {
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUIntLE(stream(1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUIntBE(stream(1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntLE(stream(1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntBE(stream(1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntME(stream(1, 2, 3)));
    }

   

    @Test
    public void testReadLongLE() throws Exception {
        assertEquals(0x0807060504030201L,
                EndianUtils.readLongLE(stream(1, 2, 3, 4, 5, 6, 7, 8)));
        assertEquals(-1L,
                EndianUtils.readLongLE(stream(255, 255, 255, 255, 255, 255, 255, 255)));
       
        assertEquals(0x80000000L, EndianUtils.readLongLE(stream(0, 0, 0, 0x80, 0, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readLongLE(stream(0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Test
    public void testReadLongBE() throws Exception {
        assertEquals(0x0102030405060708L,
                EndianUtils.readLongBE(stream(1, 2, 3, 4, 5, 6, 7, 8)));
        assertEquals(-1L,
                EndianUtils.readLongBE(stream(255, 255, 255, 255, 255, 255, 255, 255)));
        assertEquals(0x80000000L, EndianUtils.readLongBE(stream(0, 0, 0, 0, 0x80, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readLongBE(stream(0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Test
    public void testLongUnderrun() {
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readLongLE(stream(1, 2, 3, 4, 5, 6, 7)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readLongBE(stream(1, 2, 3, 4, 5, 6, 7)));
    }

    //readUE7

    @Test
    public void testReadUE7() throws Exception {
        assertEquals(5L, EndianUtils.readUE7(stream(0x05)));
        assertEquals(127L, EndianUtils.readUE7(stream(0x7F)));
        assertEquals(128L, EndianUtils.readUE7(stream(0x81, 0x00)));
        assertEquals(129L, EndianUtils.readUE7(stream(0x81, 0x01)));
    }

    @Test
    public void testReadUE7Underrun() {
        assertThrows(IOException.class, () -> EndianUtils.readUE7(stream()));
        assertThrows(IOException.class, () -> EndianUtils.readUE7(stream(0x81)));
    }

    // lectures d'un tableau d'octet

    @Test
    public void testGetShort() {
        assertEquals(513, EndianUtils.getShortLE(bytes(1, 2)));
        assertEquals(-1, EndianUtils.getShortLE(bytes(0xFF, 0xFF)));
        assertEquals(513, EndianUtils.getShortLE(bytes(0, 1, 2), 1));
        assertEquals(258, EndianUtils.getShortBE(bytes(1, 2)));
        assertEquals(-2, EndianUtils.getShortBE(bytes(0xFF, 0xFE)));
        assertEquals(258, EndianUtils.getShortBE(bytes(0, 1, 2), 1));
    }

    @Test
    public void testGetUShort() {
        assertEquals(513, EndianUtils.getUShortLE(bytes(1, 2)));
        assertEquals(65535, EndianUtils.getUShortLE(bytes(0xFF, 0xFF)));
        assertEquals(513, EndianUtils.getUShortLE(bytes(0, 1, 2), 1));
        assertEquals(258, EndianUtils.getUShortBE(bytes(1, 2)));
        assertEquals(65535, EndianUtils.getUShortBE(bytes(0xFF, 0xFF)));
        assertEquals(258, EndianUtils.getUShortBE(bytes(0, 1, 2), 1));
    }

    @Test
    public void testGetInt() {
        assertEquals(67305985, EndianUtils.getIntLE(bytes(1, 2, 3, 4)));
        assertEquals(-1, EndianUtils.getIntLE(bytes(0xFF, 0xFF, 0xFF, 0xFF)));
        assertEquals(67305985, EndianUtils.getIntLE(bytes(0xAA, 1, 2, 3, 4), 1));
        assertEquals(16909060, EndianUtils.getIntBE(bytes(1, 2, 3, 4)));
        assertEquals(-1, EndianUtils.getIntBE(bytes(0xFF, 0xFF, 0xFF, 0xFF)));
        assertEquals(16909060, EndianUtils.getIntBE(bytes(0xAA, 1, 2, 3, 4), 1));
    }

    @Test
    public void testGetUInt() {
        assertEquals(67305985L, EndianUtils.getUIntLE(bytes(1, 2, 3, 4)));
        assertEquals(67305985L, EndianUtils.getUIntLE(bytes(0xAA, 1, 2, 3, 4), 1));
        assertEquals(16909060L, EndianUtils.getUIntBE(bytes(1, 2, 3, 4)));
        assertEquals(16909060L, EndianUtils.getUIntBE(bytes(0xAA, 1, 2, 3, 4), 1));
        assertEquals(Integer.MIN_VALUE, EndianUtils.getIntLE(bytes(0, 0, 0, 0x80)));
        assertEquals(2147483648L, EndianUtils.getUIntLE(bytes(0, 0, 0, 0x80)));
        assertEquals(2147483648L, EndianUtils.getUIntBE(bytes(0x80, 0, 0, 0)));
        assertEquals(4294967295L, EndianUtils.getUIntLE(bytes(0xFF, 0xFF, 0xFF, 0xFF)));
    }

    @Test
    public void testGetLongLE() {
        assertEquals(0x0807060504030201L, EndianUtils.getLongLE(bytes(1, 2, 3, 4, 5, 6, 7, 8), 0));
        assertEquals(0x0807060504030201L,
                EndianUtils.getLongLE(bytes(0xAA, 1, 2, 3, 4, 5, 6, 7, 8), 1));
        assertEquals(-1L, EndianUtils.getLongLE(
                bytes(255, 255, 255, 255, 255, 255, 255, 255), 0));
        assertEquals(Long.MIN_VALUE, EndianUtils.getLongLE(bytes(0, 0, 0, 0, 0, 0, 0, 0x80), 0));
    }

    @Test
    public void testUbyteToIntAndGetUByte() {
        assertEquals(0, EndianUtils.ubyteToInt((byte) 0));
        assertEquals(127, EndianUtils.ubyteToInt((byte) 0x7F));
        assertEquals(128, EndianUtils.ubyteToInt((byte) 0x80));
        assertEquals(255, EndianUtils.ubyteToInt((byte) 0xFF));
        assertEquals((short) 255, EndianUtils.getUByte(bytes(0, 0xFF), 1));
        assertEquals((short) 128, EndianUtils.getUByte(bytes(0x80), 0));
        assertEquals((short) 5, EndianUtils.getUByte(bytes(0, 5), 1));
    }
}
