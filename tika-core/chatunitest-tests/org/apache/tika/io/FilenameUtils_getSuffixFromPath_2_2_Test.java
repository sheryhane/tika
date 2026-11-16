package org.apache.tika.io;

import org.apache.tika.io.FilenameUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tika.extractor.EmbeddedDocumentUtil;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.utils.StringUtils;

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
        String expected2 = "";
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
