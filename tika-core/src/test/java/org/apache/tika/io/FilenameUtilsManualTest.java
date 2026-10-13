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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

/**
 * Tests manuels écrits pour tuer les mutants PIT survivants de {@link FilenameUtils}.
 */
public class FilenameUtilsManualTest {

    @TempDir
    Path tmp;

    // Pour le nom : remplit la clé de métadonnée de rang i, dans l'ordre de priorité de
    // getEmbeddedName (utilisé par getSanitizedEmbeddedFileName).
    private static void setNameKey(Metadata m, int i, String value) {
        switch (i) {
            case 0:
                m.set(TikaCoreProperties.RESOURCE_NAME_KEY, value);
                break;
            case 1:
                m.set(TikaCoreProperties.INTERNAL_PATH, value);
                break;
            case 2:
                m.set(TikaCoreProperties.EMBEDDED_RELATIONSHIP_ID, value);
                break;
            case 3:
                m.set(TikaCoreProperties.EMBEDDED_RESOURCE_PATH, value);
                break;
            default:
                m.set(TikaCoreProperties.ORIGINAL_RESOURCE_NAME, value);
                break;
        }
    }

    // Pour le chemin : même chose avec l'ordre de priorité de getEmbeddedPath
    // (utilisé par getSanitizedEmbeddedFilePath).
    private static void setPathKey(Metadata m, int i, String value) {
        switch (i) {
            case 0:
                m.set(TikaCoreProperties.EMBEDDED_RESOURCE_PATH, value);
                break;
            case 1:
                m.set(TikaCoreProperties.INTERNAL_PATH, value);
                break;
            case 2:
                m.set(TikaCoreProperties.RESOURCE_NAME_KEY, value);
                break;
            case 3:
                m.set(TikaCoreProperties.EMBEDDED_RELATIONSHIP_ID, value);
                break;
            default:
                m.set(TikaCoreProperties.ORIGINAL_RESOURCE_NAME, value);
                break;
        }
    }

    private static Metadata withResourceName(String name) {
        Metadata m = new Metadata();
        m.set(TikaCoreProperties.RESOURCE_NAME_KEY, name);
        return m;
    }

    private static Metadata withEmbeddedPath(String path) {
        Metadata m = new Metadata();
        m.set(TikaCoreProperties.EMBEDDED_RESOURCE_PATH, path);
        return m;
    }

    // ------------------------------------------------------------ getSuffixFromPath

    /**
     * Vérifie la taille maximale d'une extension, condition {@code n.length() - i < 6}.
     */
    @Test
    public void suffixLengthLimitIsFiveCharactersIncludingTheDot() {
        assertEquals(".maha", FilenameUtils.getSuffixFromPath("file.maha")); // 5 caractères avec le point : accepté
        assertEquals("", FilenameUtils.getSuffixFromPath("file.wahbi")); // 6 caractères avec le point : refusé
    }

    // ------------------------------------------------------- getSanitizedEmbeddedFileName

    /**
     * Un nom dont le dernier "/" est suivi d'espaces n'est pas utilisable : on attend {@code null}.
     */
    @Test
    public void fileNameThatIsBlankAfterTheSlashIsRejected() {
        assertNull(FilenameUtils.getSanitizedEmbeddedFileName(withResourceName("dir/ "), ".bin", 100)); // partie nom vide
    }

    /**
     * Un nom réduit à son extension n'a pas de partie nom : on attend {@code null}.
     */
    @Test
    public void fileNameThatIsOnlyAnExtensionIsRejected() {
        assertNull(FilenameUtils.getSanitizedEmbeddedFileName(withResourceName(".txt"), ".bin", 100)); // extension seule
    }

    /**
     * Frontière de la troncature, condition {@code namePart.length() > maxLength}.
     * Un mutant {@code >=} tronquerait aussi le premier cas.
     */
    @Test
    public void fileNameIsTruncatedOnlyWhenStrictlyLongerThanMaxLength() {
        // abcdefghij fait 10 caractères (<= 10) : le nom reste intact
        assertEquals("abcdefghij.txt", FilenameUtils
                .getSanitizedEmbeddedFileName(withResourceName("abcdefghij.txt"), ".bin", 10));

        // abcdefghijk fait 11 caractères (> 10) : on garde 3 lettres, puis "..." et ".txt"
        assertEquals("abc....txt", FilenameUtils
                .getSanitizedEmbeddedFileName(withResourceName("abcdefghijk.txt"), ".bin", 10));
    }

    /**
     * Parmi les cinq métadonnées, la première qui n'est pas blanche est celle qui est retournée.
     */
    @Test
    public void fileNameUsesTheFirstNonBlankKeyInPriorityOrder() {
        for (int i = 0; i < 5; i++) {
            Metadata m = new Metadata();
            for (int j = 0; j < 5; j++) {
                setNameKey(m, j, j < i ? " " : "k" + j + ".txt"); // les clés avant le rang i sont blanches
            }
            assertEquals("k" + i + ".txt",
                    FilenameUtils.getSanitizedEmbeddedFileName(m, ".bin", 100), "rang " + i);
        }
    }

    // ------------------------------------------------------- getSanitizedEmbeddedFilePath

    /**
     * Tout chemin sans nom de fichier utilisable donne {@code null}.
     */
    @Test
    public void degeneratePathsAreRejected() {
        assertNull(FilenameUtils.getSanitizedEmbeddedFilePath(withEmbeddedPath(".."), ".bin", 100)); // ni nom ni extension
        assertNull(FilenameUtils.getSanitizedEmbeddedFilePath(withEmbeddedPath("dir/.py"), ".bin", 100)); // nom de fichier manquant
        assertNull(FilenameUtils.getSanitizedEmbeddedFilePath(withEmbeddedPath("A:"), ".bin", 100)); // lettre de lecteur seule : borne 'A'
        assertNull(FilenameUtils.getSanitizedEmbeddedFilePath(withEmbeddedPath("Z:"), ".bin", 100)); // lettre de lecteur seule : borne 'Z'
    }

    /**
     * Un chemin réduit à une extension est renvoyé tel quel (branche {@code extension.equals(path)}).
     */
    @Test
    public void pathThatIsOnlyAnExtensionIsReturnedAsIs() {
        assertEquals(".java",
                FilenameUtils.getSanitizedEmbeddedFilePath(withEmbeddedPath(".java"), ".bin", 100)); // contrairement au nom, pas de null
    }

    /**
     * Frontière {@code retPath.length() > maxLength} pour "dir/file.txt" (12 caractères).
     */
    @Test
    public void pathKeepsDirectoryOnlyWhenItFitsInMaxLength() {
        assertEquals("dir/file.txt", FilenameUtils
                .getSanitizedEmbeddedFilePath(withEmbeddedPath("dir/file.txt"), ".bin", 12)); // tient pile : intact
        assertEquals("file.txt", FilenameUtils
                .getSanitizedEmbeddedFilePath(withEmbeddedPath("dir/file.txt"), ".bin", 11)); // trop long : le dossier est abandonné
    }

    /**
     * Frontière {@code namePart.length() > maxLength} quand le chemin doit être raccourci.
     */
    @Test
    public void longPathIsTruncatedToTheNamePartOnlyWhenNeeded() {
        assertEquals("mimawabc.txt", FilenameUtils
                .getSanitizedEmbeddedFilePath(withEmbeddedPath("d/mimawabc.txt"), ".bin", 8)); // partie nom de 8 caractères : pas de troncature
        assertEquals("abc....txt", FilenameUtils
                .getSanitizedEmbeddedFilePath(withEmbeddedPath("d/abcdefghijk.txt"), ".bin", 10)); // partie nom de 11 caractères : tronquée
    }

    /**
     * Même contrôle pour le chemin : la première métadonnée non blanche l'emporte.
     */
    @Test
    public void pathUsesTheFirstNonBlankKeyInPriorityOrder() {
        for (int i = 0; i < 5; i++) {
            Metadata m = new Metadata();
            for (int j = 0; j < 5; j++) {
                setPathKey(m, j, j < i ? " " : "d/f" + j + ".txt"); // les clés avant le rang i sont blanches
            }
            assertEquals("d/f" + i + ".txt",
                    FilenameUtils.getSanitizedEmbeddedFilePath(m, ".bin", 100), "rang " + i);
        }
    }

    // ------------------------------------------------------------------ resolveWithin

    /**
     * Un fichier qui existe dans le dossier est renvoyé sous forme normalisée.
     */
    @Test
    public void resolveWithinReturnsAnExistingFileInsideTheDirectory() throws IOException {
        Path dir = Files.createDirectory(tmp.resolve("inside"));
        Files.createFile(dir.resolve("a.txt")); // le fichier existe : le contrôle des liens symboliques s'exécute
        assertEquals(dir.normalize().resolve("a.txt"), FilenameUtils.resolveWithin(dir, "a.txt"));
    }

    /**
     * Un nom inexistant est accepté, et les ".." intérieurs sont normalisés avant le contrôle.
     */
    @Test
    public void resolveWithinAcceptsNonExistingAndNormalizedNames() throws IOException {
        Path dir = Files.createDirectory(tmp.resolve("inside"));
        assertEquals(dir.normalize().resolve("new/sub.txt"),
                FilenameUtils.resolveWithin(dir, "new/sub.txt")); // n'existe pas encore
        assertEquals(dir.normalize().resolve("a.txt"),
                FilenameUtils.resolveWithin(dir, "x/../a.txt")); // revient à "a.txt" après normalisation
    }

    /**
     * Un nom qui sort du dossier avec ".." est refusé par une {@code IOException}
     * dont le message contient le nom fautif et "outside of".
     */
    @Test
    public void resolveWithinRejectsParentTraversal() throws IOException {
        Path dir = Files.createDirectory(tmp.resolve("b"));

        Files.createDirectory(tmp.resolve("bc")); // dossier voisin dont le nom commence comme "b"

        IOException e = assertThrows(IOException.class, () -> FilenameUtils.resolveWithin(dir, "../x"));

        assertTrue(e.getMessage().contains("../x") && e.getMessage().contains("outside of"));
        assertThrows(IOException.class, () -> FilenameUtils.resolveWithin(dir, "../bc")); // piège pour une comparaison de chaînes
    }

    /**
     * Un lien symbolique qui pointe hors du dossier est refusé (contrôle du chemin réel).
     */
    @Test
    public void resolveWithinRejectsSymlinkPointingOutside() throws IOException {
        Path dir = Files.createDirectory(tmp.resolve("inside"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Files.createFile(outside.resolve("secret.txt"));
        try {
            Files.createSymbolicLink(dir.resolve("link"), outside);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            assumeTrue(false, "liens symboliques non supportés"); // test sauté si le système refuse les liens
        }

        IOException e1 = assertThrows(IOException.class,
                () -> FilenameUtils.resolveWithin(dir, "link/secret.txt")); // chemin logique dedans, chemin réel dehors
        assertTrue(e1.getMessage().contains("link/secret.txt"));
        assertThrows(IOException.class, () -> FilenameUtils.resolveWithin(dir, "link"));
    }

    // ---------------------------------------------------------------- calculateExtension

    /**
     * Les quatre retours de calculateExtension : valeur par défaut, type inconnu, type mal formé, type connu.
     */
    @Test
    public void calculateExtensionDefaultsAndLookups() {
        Metadata none = new Metadata();
        assertEquals(".dflt", FilenameUtils.calculateExtension(none, ".dflt")); // pas de type : valeur par défaut

        Metadata unknown = new Metadata();
        unknown.set(HttpHeaders.CONTENT_TYPE, "application/x-tache2-unknown");
        assertEquals(".bin", FilenameUtils.calculateExtension(unknown, ".dflt")); // bien formé mais inconnu

        Metadata malformed = new Metadata();
        malformed.set(HttpHeaders.CONTENT_TYPE, "not-a-media-type");
        assertEquals(".bin", FilenameUtils.calculateExtension(malformed, ".dflt")); // mal formé : MimeTypeException

        Metadata png = new Metadata();
        png.set(HttpHeaders.CONTENT_TYPE, "image/png");
        assertEquals(".png", FilenameUtils.calculateExtension(png, ".dflt")); // type connu : son extension
    }
}
