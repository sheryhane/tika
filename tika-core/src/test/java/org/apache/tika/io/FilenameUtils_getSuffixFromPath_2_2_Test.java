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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FilenameUtils_getSuffixFromPath_2_2_Test {

    @Test
    public void testGetSuffixFromPath() throws Exception {
        // Test case 1: Normal file path with valid suffix
        String path1 = "example.txt";
        String expected1 = ".txt";
        String actual1 = FilenameUtils.getSuffixFromPath(path1);
        assertEquals(expected1, actual1);
        // Test case 2: File path with invalid suffix
        String path2 = "example.doc";
        String expected2 = ".doc";
        String actual2 = FilenameUtils.getSuffixFromPath(path2);
        assertEquals(expected2, actual2);
        // Test case 3: File path with no suffix
        String path3 = "example";
        String expected3 = "";
        String actual3 = FilenameUtils.getSuffixFromPath(path3);
        assertEquals(expected3, actual3);
        // Test case 4: File path with empty string
        String path4 = "";
        String expected4 = "";
        String actual4 = FilenameUtils.getSuffixFromPath(path4);
        assertEquals(expected4, actual4);
        // Test case 5: File path with null
        String path5 = null;
        String expected5 = "";
        String actual5 = FilenameUtils.getSuffixFromPath(path5);
        assertEquals(expected5, actual5);
    }
}
