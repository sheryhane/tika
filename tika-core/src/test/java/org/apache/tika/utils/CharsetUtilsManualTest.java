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
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests manuels (tâche 2) pour les mutants PIT survivants de {@link CharsetUtils}, dans les
 * branches ICU4J de {@code isSupported} et de {@code forName}.
 * <p>
 * ICU4J n'est pas sur le classpath de tika-core : les champs statiques privés
 * {@code isSupportedICU} et {@code getCharsetICU} (de type {@link java.lang.reflect.Method})
 * valent {@code null}, donc ces branches ne s'exécutent jamais avec les tests existants.
 * {@code Method} est une classe {@code final} que Mockito ne peut pas simuler (c'est l'erreur
 * des tests générés par ChatUniTest). On injecte donc par réflexion de vraies méthodes statiques
 * de la classe factice {@link FakeIcu}, puis on restaure les valeurs d'origine après chaque test
 * pour éviter les effets de bord entre les tests.
 */
public class CharsetUtilsManualTest {

    /** Faux ICU : méthodes statiques publiques que CharsetUtils appelle par réflexion. */
    public static class FakeIcu {
        public static final String NAME = "fake-icu-charset";

        public static Boolean isSupported(String name) {
            return NAME.equals(name);
        }

        public static Charset forNameIcu(String name) {
            return NAME.equals(name) ? StandardCharsets.UTF_16 : null;
        }

        public static Boolean throwingIsSupported(String name) {
            throw new IllegalStateException("ICU en panne (simulé)");
        }

        public static Charset throwingForName(String name) {
            throw new IllegalStateException("ICU en panne (simulé)");
        }
    }

    private Object savedIsSupported;
    private Object savedGetCharset;

    @BeforeEach
    public void saveIcuFields() throws Exception {
        savedIsSupported = read("isSupportedICU");
        savedGetCharset = read("getCharsetICU");
    }

    @AfterEach
    public void restoreIcuFields() throws Exception {
        write("isSupportedICU", savedIsSupported);
        write("getCharsetICU", savedGetCharset);
    }

    private static Object read(String field) throws Exception {
        Field f = CharsetUtils.class.getDeclaredField(field);
        f.setAccessible(true);
        return f.get(null);
    }

    private static void write(String field, Object value) throws Exception {
        Field f = CharsetUtils.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }

    /**
     * isSupported() consulte ICU avant le JDK (mutants des lignes 107 et 108).
     */
    @Test
    public void isSupportedConsultsIcuFirst() throws Exception {
        write("isSupportedICU",
                FakeIcu.class.getMethod("isSupported", String.class));
        assertTrue(CharsetUtils.isSupported(FakeIcu.NAME)); // inconnu du JDK, mais le faux ICU répond true
        assertTrue(CharsetUtils.isSupported("UTF-8")); // connu du JDK
        assertFalse(CharsetUtils.isSupported("unknown-charset-xyz")); // connu de personne
    }

    /**
     * Si ICU lève une exception, isSupported() la capture et renvoie false (mutant de la ligne 118).
     */
    @Test
    public void isSupportedReturnsFalseWhenIcuThrows() throws Exception {
        write("isSupportedICU",
                FakeIcu.class.getMethod("throwingIsSupported", String.class));
        assertFalse(CharsetUtils.isSupported("UTF-8")); // nom pourtant valide : l'exception arrive avant le test du JDK
    }

    /**
     * forName() renvoie le charset fourni par ICU quand il existe (mutants des lignes 186 et 187).
     */
    @Test
    public void forNameReturnsTheCharsetFoundByIcu() throws Exception {
        write("getCharsetICU",
                FakeIcu.class.getMethod("forNameIcu", String.class));
        assertSame(StandardCharsets.UTF_16, CharsetUtils.forName(FakeIcu.NAME)); // nom absent du JDK : seul ICU peut répondre
    }

    /**
     * Si ICU renvoie null, forName() retombe sur Charset.forName du JDK (mutant de la ligne 186).
     */
    @Test
    public void forNameFallsBackToJdkWhenIcuReturnsNull() throws Exception {
        write("getCharsetICU",
                FakeIcu.class.getMethod("forNameIcu", String.class));
        assertSame(Charset.forName("UTF-32"), CharsetUtils.forName("UTF-32")); // UTF-32 : connu du JDK, ICU répond null
    }

    /**
     * Si ICU lève une exception, forName() l'ignore et retombe sur le JDK (clause catch).
     */
    @Test
    public void forNameFallsBackToJdkWhenIcuThrows() throws Exception {
        write("getCharsetICU",
                FakeIcu.class.getMethod("throwingForName", String.class));
        assertSame(Charset.forName("UTF-32"), CharsetUtils.forName("UTF-32")); // l'InvocationTargetException est ignorée
    }
}
