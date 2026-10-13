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

public class EndianUtilsGeneratedTest {

    @Test
    public void testReadShortLE() throws Exception {
        // bytes 01 02 in little-endian = 0x0201 = 513
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{0x01, 0x02});
        assertEquals(513, EndianUtils.readShortLE(in));
    }

    @Test
    public void testReadShortLEUnderrun() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{0x01});
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readShortLE(in));
    }

    @Test
    public void testReadUShortLE() throws Exception {
        // bytes 01 02 in little-endian = 513
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{0x01, 0x02});
        assertEquals(513, EndianUtils.readUShortLE(in));
    }

    @Test
    public void testReadUShortLEUnsigned() throws Exception {
        // bytes FF FF: unsigned = 65535 (a signed read would give -1)
        ByteArrayInputStream in =
                new ByteArrayInputStream(new byte[]{(byte) 0xFF, (byte) 0xFF});
        assertEquals(65535, EndianUtils.readUShortLE(in));
    }
}
