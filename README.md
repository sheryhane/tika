# Tâche 2 : génération de tests avec ChatUniTest et analyse de mutation (Apache Tika, `tika-core`)

Ce document rend compte de la tâche 2 sur le module `tika-core` d'Apache Tika. Nous avons choisi deux classes déjà testées mais incomplètement couvertes, mesuré la qualité de leurs tests avec PIT, essayé de les compléter avec un modèle de langage local piloté par ChatUniTest, puis écrit à la main les tests qui manquaient pour tuer les mutants restants. Chaque section correspond à un critère du barème : choix des classes (section 2), installation de ChatUniTest (section 3), génération et critique des tests (section 4), analyse de mutation (section 5), tests écrits à la main (section 6) et exécution dans la GitHub Action (section 7).

## 1. Équipe et classes choisies

- Étudiant(e)(s) : Amhaouch Maha
- Module : `tika-core` (fork du dépôt Apache Tika, branche `tache2`)
- Classes testées : `org.apache.tika.io.FilenameUtils` et `org.apache.tika.utils.CharsetUtils`
- Tests originaux associés : `FilenameUtilsTest` et `CharsetUtilsTest`

- Étudiant(e)(s) : Souha Wahbi 
- Module : `tika-core` (fork du dépôt Apache Tika, branche `tache2`)
- Classes testées : `org.apache.tika.io.EndianUtils`
- Tests originaux associés : `EndianUtilsTest`
  

## 2. Justification du choix des classes et des méthodes

Le choix s'appuie sur une analyse de base faite avec PIT (rapports dans `docs/pit-baseline/`), exécutée avec uniquement les deux tests d'origine de ces classes, de sorte que les tests d'autres classes qui les exercent indirectement ne faussent pas la mesure. Aucune des deux classes n'est couverte à 100 %, et les deux contiennent des mutants que les tests d'origine ne détectent pas, ce qui satisfait le critère de sélection de l'énoncé.

| Classe | Couverture de lignes (PIT) | Score de mutation | Test strength | Mutants non tués |
|---|---|---|---|---|
| `FilenameUtils` | 88 % (162/184) | **69 %** (79/115) | 80 % | 36 (16 sans couverture, 20 survivants) |
| `CharsetUtils` | 83 % (67/81) | **80 %** (20/25) | 100 % | 5 (tous sans couverture) |
| `EndianUtils` | 26 % (31/121) | **18 %** (38/206) | 73 % | 168 (154 sans couverture, 14 survivants) |
| **Total** | 67 % (260/386) | **40 %** (137/346) | 80 % | 209 (175 sans couverture, 34 survivants) |

La couverture mesurée par JaCoCo (rapport `tika-core/target/site/jacoco/index.html`) donne une image voisine :

| Classe | Lignes avec tests d'origine | Lignes avec tous les tests | Branches avec tests d'origine | Branches avec tous les tests |
|---|---|---|---|---|
| `FilenameUtils` | 88 % | **98 %** | 76 % | **95 %** |
| `CharsetUtils` | 81 % | **90 %** | 78 % | **97 %** |

Chaque colonne provient d'une exécution séparée de `mvn -pl tika-core test` après suppression de `tika-core/target/jacoco.exec`, car JaCoCo ajoute par défaut les nouvelles exécutions au fichier existant et un seul fichier mélangerait les deux situations. Les colonnes « tests d'origine » correspondent à 14 tests exécutés, les colonnes « tous les tests » à 35 tests (les 14 d'origine, le test généré corrigé et les 20 tests manuels). De petits écarts avec PIT sont normaux (81 % contre 83 % pour `CharsetUtils`), car les deux outils ne comptent pas les lignes de la même façon, notamment pour les initialiseurs statiques que PIT ne mute pas.

Une troisième classe, `MediaType` (couverture 78 %, score de mutation 61 %, 33 mutants non tués), avait été analysée au départ. L'énoncé autorise entre une et trois classes ; nous l'avons écartée pour consacrer le temps de génération, long avec un modèle local, et d'analyse à deux classes que nous pouvons documenter en détail, et aucune génération n'a été tentée dessus.

### 2.1 Mutants non tués, classe par classe (analyse de base)

Les rapports HTML de PIT sont conservés dans `docs/pit-baseline/pit-reports/`. Le numéro est celui de la ligne dans le fichier source. Le statut **NO_COVERAGE** signifie qu'aucun test n'exécute la ligne, et **SURVIVED** que la ligne est exécutée mais qu'aucune assertion ne remarque la modification.

#### `FilenameUtils` : 36 mutants non tués (16 NO_COVERAGE, 20 SURVIVED)

| Méthode | Ligne(s) | Mutation | Statut |
|---|---|---|---|
| `getSuffixFromPath` | 134 | `n.length() - i < 6` devient `<= 6` | **SURVIVED** |
| `getSanitizedEmbeddedFileName` | 156 | `prefixLength > 0` devient `>= 0` ; condition niée | **SURVIVED** (2) |
| `getSanitizedEmbeddedFileName` | 174, 185 | `return null` remplacé par `""` | NO_COVERAGE (2) |
| `getSanitizedEmbeddedFileName` | 189 | `namePart.length() > maxLength` devient `>=` | **SURVIVED** |
| `getSanitizedEmbeddedFilePath` | 215 | `prefixLength > 0` devient `>= 0` | **SURVIVED** |
| `getSanitizedEmbeddedFilePath` | 232, 250, 259 | `return null` remplacé par `""` | NO_COVERAGE (3) |
| `getSanitizedEmbeddedFilePath` | 264, 265 | `retPath.length() > maxLength` et `namePart.length() > maxLength` deviennent `>=` | **SURVIVED** (2) |
| `getSanitizedEmbeddedFilePath` | 268 | `return namePart + extension` remplacé par `""` | NO_COVERAGE |
| `resolveWithin` | 305 | condition niée (vérification d'existence avant `toRealPath`) | **SURVIVED** (1) + NO_COVERAGE (1) |
| `resolveWithin` | 308 | condition `!realResolved.startsWith(realDir)` niée | NO_COVERAGE |
| `getPrefixLength` | 323 | `>= 'A'` devient `> 'A'` ; `<= 'Z'` devient `< 'Z'` | **SURVIVED** (2) |
| `getEmbeddedPath` | 344, 348, 352, 356 | condition `!isBlank(path)` niée | **SURVIVED** (4) |
| `getEmbeddedPath` | 349, 353, 357 | retour de la clé de repli remplacé par `""` | NO_COVERAGE (3) |
| `getEmbeddedPath` | 359 | retour de `ORIGINAL_RESOURCE_NAME` remplacé par `""` | **SURVIVED** |
| `getEmbeddedName` | 365, 369, 373, 377 | condition `!isBlank(path)` niée | **SURVIVED** (4) |
| `getEmbeddedName` | 370, 374, 378 | retour de la clé de repli remplacé par `""` | NO_COVERAGE (3) |
| `getEmbeddedName` | 380 | retour de `ORIGINAL_RESOURCE_NAME` remplacé par `""` | **SURVIVED** |
| `calculateExtension` | 402 | retour `".bin"` remplacé par `""` | NO_COVERAGE |
| `lookupExtension` | 416 | retour `null` remplacé par `""` | NO_COVERAGE |

#### `CharsetUtils` : 5 mutants non tués (tous NO_COVERAGE)

| Méthode | Ligne | Mutation | Cause |
|---|---|---|---|
| `isSupported` | 107 | condition `isSupportedICU != null && ...` niée | La branche ICU n'est jamais exécutée : ICU4J est absent du classpath de test, donc `isSupportedICU` vaut toujours `null` |
| `isSupported` | 108 | `return true` remplacé par `false` | Même cause (branche ICU) |
| `isSupported` | 118 | `return false` remplacé par `true` | Le `catch (Exception e)` n'est jamais atteint |
| `forName` | 186 | condition `cs != null` niée | La branche ICU de `forName` (`getCharsetICU != null`) n'est jamais exécutée |
| `forName` | 187 | `return cs` remplacé par `null` | Même cause |

Les 20 autres mutants de `CharsetUtils` sont tués, ce qui explique une test strength de 100 %. Les lignes rouges du bloc `static { ... }` n'ont pas de mutants, car PIT filtre les initialiseurs statiques, et la ligne 31 correspond au constructeur par défaut implicite.

#### `EndianUtils` : 168 mutants non tués (154 NO_COVERAGE, 14 SURVIVED)

`EndianUtilsTest` contient juste quatre tests ( `testReadUIntBE`, `testReadUE7`, `testReadUIntLE`et `testReadIntME`), et ilss n'appellent que quatre méthodes sur une trentaine. Les 14 mutants qui survivent se trouvent tous dans ces quatre méthodes :

| Méthode | Ligne(s) | Mutation | Statut |
|---|---|---|---|
| `readUIntLE` | 92 | `(ch1 \| ch2 \| ch3 \| ch4) < 0` devient `<= 0` | **SURVIVED** |
| `readUIntLE` | 92 | premier et deuxième `\|` remplacés par `&` | **SURVIVED** (2) |
| `readUIntBE` | 111 | `(ch1 \| ch2 \| ch3 \| ch4) < 0` devient `<= 0` | **SURVIVED** |
| `readUIntBE` | 111 | les trois `\|` remplacés par `&` | **SURVIVED** (3) |
| `readIntME` | 168 | `(ch1 \| ch2 \| ch3 \| ch4) < 0` devient `<= 0` | **SURVIVED** |
| `readIntME` | 168 | premier et deuxième `\|` remplacés par `&` | **SURVIVED** (2) |
| `readUE7` | 235 | `(i = stream.read()) >= 0` devient `> 0` ; `read++ < max` devient `<= max` | **SURVIVED** (2) |
| `readUE7` | 235 | `read++` remplacé par `read--` | **SURVIVED** |
| `readUE7` | 246 | `i < 0` devient `i <= 0` | **SURVIVED** |

#### `EndianUtils` : mutants sans couverture

Les 154 mutants sans couverture se trouvent dans les méthodes qu'aucun test n'appelle.

Les 38 autres mutants de `EndianUtils` sont tués, ce qui donne une test strength de 73 %. Les mutants mathématiques des lignes de retour sont tous tués, parce que les tests comparent des valeurs exactes calculées avec des octets non nuls et différents.

### 2.2 Constats

Trois causes expliquent presque tous les mutants non tués. D'abord, les chaînes de repli de `getEmbeddedPath` et de `getEmbeddedName` ne sont parcourues qu'avec une seule clé de métadonnées sur cinq, ce qui concentre 24 des 36 mutants de `FilenameUtils` dans ces deux méthodes. Ensuite, les cas limites manquent : les frontières entre `>` et `>=` sur `maxLength`, l'extension de 5 ou 6 caractères, les lettres de lecteur `A:` et `Z:`, ainsi que les branches d'erreur (`return null`, lien symbolique). Enfin, le code de `CharsetUtils` qui dépend de l'environnement (ICU4J) ne s'exécute jamais dans les tests, faute de bibliothèque et parce que les champs concernés sont privés et statiques.

Pour `EndianUtils`, on a une raison dominante : 154 des 168 mutants non tués sont sans couverture, parce que `EndianUtilsTest` n'appelle que 4 méthodes sur une trentaine. Les 14 survivants s'expliquent par trois défauts des tests existants:  aucune donnée ne se place sur une frontière - aucun octet lu ne vaut 0 - aucune lecture n'atteint la limite de 6 octets.

### 2.3 Méthodes choisies pour la génération

Nous avons confié à ChatUniTest deux méthodes dont les mutants vivants se prêtent à une comparaison avec nos tests écrits à la main. `CharsetUtils#forName` contient deux des cinq mutants sans couverture (lignes 186 et 187) et une logique de normalisation de noms riche en branches (`iso`, `cp`, `win`, `none`), mais elle dépend de champs statiques privés qui mettent un modèle de 7 milliards de paramètres en difficulté, ce qui en fait un cas instructif. `FilenameUtils#getSuffixFromPath` est au contraire courte, sans dépendance externe, et porte le mutant survivant de la ligne 134 (la limite de longueur de l'extension) : si un modèle local sait produire un bon test quelque part, c'est ici.
Egalement que deux méthodes de EndianUtils: `EndianUtils#readUIntBE` est exécutée par les tests d'origine mais porte quatre mutants survivants (ligne 111) : elle permet de vérifier si le modèle pense à tester un flux tronqué sur la bonne méthode. `EndianUtils#getIntLE` n'est  jamais appelée : elle est courte, statique, sans dépendance externe, et prend un simple tableau d'octets, ce qui en fait le cas le plus favorable pour un modèle local. 
## 3. Installation de l'outillage

### 3.1 JaCoCo et PIT (`tika-core/pom.xml`)

JaCoCo 0.8.15 produit le rapport de couverture (749 tests de `tika-core` exécutés, 0 échec, 2 ignorés). PIT est ajouté avec `pitest-maven` 1.17.4 et `pitest-junit5-plugin` 1.2.1 ; `targetClasses` est limité aux deux classes et `targetTests` aux classes de test dont le nom correspond à `CharsetUtils*Test` et `FilenameUtils*Test`, ce qui inclut les tests d'origine, le test généré et les tests manuels. Les formats HTML et XML sont activés pour pouvoir comparer les rapports par script (annexe A). La commande est :

```bash
mvn -pl tika-core test-compile org.pitest:pitest-maven:mutationCoverage -Djacoco.skip=true
```

Le premier lancement de PIT a échoué avec `Coverage generator Minion exited abnormally`. Les logs détaillés ont révélé le message `OutputDirectoryCreator not available; probably due to unaligned versions of the junit-platform-engine and junit-platform-launcher jars` : Tika utilise JUnit Platform 6.1.3 alors que `pitest-junit5-plugin` tirait la version 1.9.2. Après alignement du launcher sur 6.1.3, PIT s'exécute correctement en quelques secondes.

### 3.2 Ollama et modèle de langage local

Ollama est installé avec Homebrew et sert le modèle **Qwen2.5-Coder 7B** (`qwen2.5-coder:7b`, quantification Q4_K_M) sur l'API compatible OpenAI à l'adresse `http://localhost:11434/v1`.

### 3.3 ChatUniTest dans le pipeline Maven

Le plugin `io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1` est déclaré dans `tika-core/pom.xml` avec `url` = `http://localhost:11434/v1/chat/completions`, `maxRounds` = 3, `testNumber` = 3, `maxThreads` = 1, `temperature` = 0.2 et `maxPromptTokens` = 4096, des valeurs choisies pour obtenir des tests plus déterministes et une fenêtre de contexte adaptée à un modèle de cette taille. Cette version expose les goals `class`, `method` et `project`, mais pas de goal `help`, de sorte que le préfixe court `chatunitest:` n'est pas résolu et qu'il faut appeler le plugin par ses coordonnées complètes :

```bash
mvn -pl tika-core io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:method -DselectMethod=CharsetUtils#forName
```

La version 2.1.1 maintient une liste fermée de modèles : elle refuse `qwen2.5-coder:7b` (`No Model with name qwen2.5-coder:7b`) et n'accepte que `gpt-3.5-turbo`, `gpt-3.5-turbo-1106`, `gpt-4o`, `gpt-4o-mini`, `gpt-4o-mini-2024-07-18`, `code-llama` et `codeqwen:v1.5-chat`. Nous avons donc créé dans Ollama un alias portant le nom accepté `code-llama`, qui pointe en réalité vers Qwen2.5-Coder 7B, comme le montre `ollama show` :

```
$ ollama show code-llama:latest
  architecture   qwen2
  parameters     7.6B
  quantization   Q4_K_M
  System: "You are Qwen, created by Alibaba Cloud..."
```

Cet alias a le même identifiant que `qwen2.5-coder:7b` (`dae161e27b0e`) : le modèle réellement exécuté est bien Qwen2.5-Coder 7B, et l'étiquette `CODE_LLAMA` affichée par le plugin n'est qu'un nom.

Pour `EndianUtils`, On lance PIT sur une machine Windows, avec `pitest-maven` 1.19.5, on limite la cible à cette classe et à ses tests directement en ligne de commande :

```bat
mvn -pl tika-core test-compile org.pitest:pitest-maven:mutationCoverage -Djacoco.skip=true -DtargetClasses=org.apache.tika.io.EndianUtils -DtargetTests=org.apache.tika.io.EndianUtilsTest
```
Il faut les omettre les apostrophes sinon aucun test n'est trouvé.

## 4. Tests générés par ChatUniTest : résultats et critique

### 4.1 Où sont les tests et que sont-ils devenus

ChatUniTest écrit ses sorties dans `tika-core/chatunitest-tests/` et ses journaux (invites, réponses, messages d'erreur) dans `/tmp/chatunitest-info/tika-parent/tika-core/`, que nous avons copiés dans `docs/chatunitest-logs/`. Trois exécutions ont été faites : deux sur `CharsetUtils#forName` (avant puis après ajout de Mockito) et une sur `FilenameUtils#getSuffixFromPath`.

| Fichier | Compile sans intervention ? | S'exécute avec succès ? | Corrections manuelles |
|---|---|---|---|
| `CharsetUtils_forName_3_0_Test` | Run 1 : non (Mockito absent). Run 2 : oui | Non : échec en `setUp` | Aucune tentée : le défaut de fixture invalide tous les cas |
| `CharsetUtils_forName_3_1_Test` | Non, ni au run 1 ni au run 2 (3 rounds échoués) | Non | Aucune tentée |
| `CharsetUtils_forName_3_2_Test` | Run 1 : non. Run 2 : oui au round 2 | Non : échec en `setUp` | Aucune tentée |
| `FilenameUtils_getSuffixFromPath_2_2_Test` | Oui | Non : un oracle faux (génération 2 sur 3) | **2 manuelles** + 2 automatiques de style |

Sur quatre tests produits, aucun ne s'exécute avec succès sans intervention, et un seul a pu être intégré au projet après correction. Les deux autres générations de `getSuffixFromPath` (numéros 0 et 1) ont échoué aux trois rounds de réparation et n'ont rien produit.

### 4.2 Premier essai sur `CharsetUtils#forName` (sans Mockito)

Les messages `javac` révèlent d'abord des erreurs d'environnement : `package org.mockito does not exist` et `package org.mockito.junit.jupiter does not exist` dans les trois tests, puisque Mockito n'est pas une dépendance de `tika-core`, et, dans `_3_2`, les erreurs `cannot find symbol: class Mock` et `MockitoExtension` qui en découlent. Un avertissement signale par ailleurs que le processeur d'annotations de Tika déclare la version de source 17 alors que la machine utilise le JDK 22.

La lecture du code généré montre en outre neuf défauts : l'appel de `forName` par une instance de `CharsetUtils` alors que la méthode est statique ; l'appel de `getDeclaredMethod("getCharsetICU")` alors que `getCharsetICU` et `isSupportedICU` sont des champs de type `Method` et non des méthodes ; l'affectation du champ privé et final `COMMON_CHARSETS` ; l'affectation du champ privé `getCharsetICU` ; l'usage de `@Mock` sur un champ statique et le mock de `java.lang.reflect.Method`, classe finale que Mockito classique ne sait pas simuler ; l'usage dans `_3_2` d'un champ `commonCharsets` jamais déclaré ; un oracle faux, `assertEquals(Charset.forName("invalid"), charset)`, qui ne peut pas passer puisque `Charset.forName("invalid")` lève `UnsupportedCharsetException` ; l'hypothèse erronée que `forName("invalid")` lève `IllegalCharsetNameException` alors que ce nom est syntaxiquement légal ; et un oracle correct mais incomplet pour `forName(null)`, qui n'est pas accompagné des cas `"none"` et `"no"`.

### 4.3 Deuxième essai, après ajout de Mockito

Nous avons déclaré `mockito-core` et `mockito-junit-jupiter` en version 3.12.4, en portée `test`, dans `tika-core/pom.xml`, et le build reste valide (enforcer et Checkstyle passent). La génération a duré 8 min 56 s, contre 11 min 56 s pour la première. Les erreurs d'imports ont disparu, le test `_3_0` compile alors, `_3_2` compile au deuxième round et `_3_1` ne compile toujours pas, mais aucun des trois ne s'exécute avec succès et le dossier `tika-core/chatunitest-tests/` reste vide (`No test case left`).

L'unique erreur d'exécution relevée dans les journaux est `java.lang.NoSuchMethodException: org.apache.tika.utils.CharsetUtils.getCharsetICU()`, levée dans la méthode `setUp` par `getDeclaredMethod("getCharsetICU")`. Elle confirme à l'exécution le deuxième défaut de la section précédente. Comme `setUp` s'exécute avant chaque test, cette seule erreur fait échouer tous les cas de la classe (8 méthodes dans `_3_0`, 7 dans `_3_2`), y compris ceux dont l'oracle est correct, comme `forName(null)` qui doit lever `IllegalArgumentException` : une erreur de fixture rend donc inutilisables des assertions pertinentes. Les autres défauts n'ont pas pu être confirmés, car `setUp` échoue avant.

| | Run 1 (sans Mockito) | Run 2 (avec Mockito) |
|---|---|---|
| Tests qui compilent | 0 sur 3 | 2 sur 3 |
| Tests qui s'exécutent avec succès | 0 sur 3 | 0 sur 3 |
| Cause dominante | Mockito absent | `NoSuchMethodException` en `setUp` (erreur du modèle) |

Journaux : `docs/chatunitest-logs/run1-sans-mockito/` et `docs/chatunitest-logs/run2-avec-mockito/`.

### 4.4 Troisième essai : `FilenameUtils#getSuffixFromPath`

La commande était `mvn -pl tika-core io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:method -DselectMethod=FilenameUtils#getSuffixFromPath -DtestNumber=1`. La configuration affichée par le plugin indique pourtant `TestNumber >>> 3` : l'option `-DtestNumber=1` n'a pas été prise en compte, si bien que trois générations ont été tentées, en 4 min 5 s au total. Les générations 0 et 1 ont échoué à l'exécution aux trois rounds. La génération 2 s'est terminée par « compile and execute successfully round 0 » alors que JUnit comptait `1 tests failed` : pour le plugin, un test « réussit » dès qu'il compile et s'exécute, sans que l'oracle soit validé. Nous avons conservé ce test, `FilenameUtils_getSuffixFromPath_2_2_Test`, dont le texte brut est sauvegardé dans `docs/chatunitest-logs/run3-getSuffixFromPath/`.

Le test contient une seule méthode, `testGetSuffixFromPath`, qui enchaîne cinq cas, et une annotation `@ExtendWith(MockitoExtension.class)` inutile puisqu'aucun mock n'est déclaré.

| Cas | Entrée | Oracle de l'IA | Comportement réel | Verdict |
|---|---|---|---|---|
| 1 | `"example.txt"` | `".txt"` | `".txt"` | correct |
| 2 | `"example.doc"` | `""` (commentaire : « invalid suffix ») | `".doc"` | **faux** |
| 3 | `"example"` | `""` | `""` | correct |
| 4 | `""` | `""` | `""` | correct |
| 5 | `null` | `""` | `""` (`getName(null)` renvoie `""`) | correct |

Le modèle a jugé `.doc` invalide, alors que le code accepte toute extension alphanumérique de cinq caractères au plus, point compris, et que sa Javadoc le précise. L'assertion échoue donc avec `expected: <> but was: <.doc>`.

Quatre corrections ont été nécessaires pour intégrer ce test, dont deux seulement à la main :

| # | Correction | Nature | Faite par |
|---|---|---|---|
| 1 | Ajout de l'en-tête de licence exigé par la règle `RegexpHeader` | style | Spotless, automatiquement |
| 2 | Suppression de 12 imports inutilisés (règle `UnusedImports`) | style | Spotless, automatiquement |
| 3 | Remplacement de 4 imports `*` par des imports explicites (règle `AvoidStarImport`) | style | **à la main** |
| 4 | Oracle du cas 2 corrigé de `""` en `".doc"` | logique | **à la main** |

Les 17 erreurs Checkstyle initiales se répartissent en 1 (en-tête) + 12 (imports inutilisés) + 4 (imports `*`).

### 4.5 Critique des tests générés et comparaison avec les oracles écrits à la main

Les tests d'origine de Tika, écrits par des humains, ont des oracles précis et motivés. `FilenameUtilsTest.testEmbeddedFileNames` compare la chaîne exacte attendue pour une quarantaine d'entrées, parmi lesquelles des tentatives de remontée de dossier comme `"the quick../..brown fox.docx"` (attendu `"_brown fox.docx"`), des caractères réservés (`"the quick>brown fox.xlsx"` donne `"the quick%3Ebrown fox.xlsx"`) et une troncature à 50 caractères (`"abcdefghijklmnopqrstuvwxyz_abcdefghijklmno....docx"`). `testResolveWithin` ajoute des commentaires qui expliquent pourquoi chaque assertion existe, par exemple la distinction entre préfixe de chaîne et préfixe de chemin (`a/bc` n'est pas dans `a/b`). Dans `CharsetUtilsTest.testFunkyNames`, les corrections de noms mal orthographiés (`"iso-8851-1"` devient `"ISO-8859-1"`) sont des cas de régression issus de données réelles.

À côté, les oracles de l'IA se classent ainsi :

| Critère | Tests écrits par des humains (originaux et manuels) | Tests générés par Qwen2.5-Coder 7B |
|---|---|---|
| Pertinence | Chaque entrée vise une règle ou une frontière précise du code | Cas génériques et prévisibles (`example.txt`, chaîne vide, `null`) |
| Spécificité | Chaîne exacte attendue, dérivée de la lecture du code (par exemple 3 caractères conservés = `maxLength - len(".txt") - 3`) | Un oracle faux (`.doc` jugé invalide) montre une règle mal comprise |
| Vérifications triviales | Rares | Trois cas sur cinq (`example`, `""`, `null`) vérifient des comportements que `FilenameUtilsTest` exerce déjà, par exemple avec `sanitizeFilename("")` et `sanitizeFilename(null)` |
| Isolement des échecs | Une intention par méthode | Cinq cas dans une méthode : la première assertion fausse masque les suivantes |
| Frontières | Testées explicitement (`.maha` accepté, `.wahbi` refusé) | Aucune ; deux extensions de même longueur seulement |
| Fixture | Restauration des champs statiques après chaque test | `setUp` erroné qui invalide tous les cas (`CharsetUtils`) |

Un modèle de cette taille produit des tests syntaxiquement plausibles mais qui ne reflètent pas le comportement réel du code, et il échoue exactement là où les tests sont les plus utiles : sur le code qui dépend de champs privés, d'un état statique ou d'une frontière numérique. Le mécanisme de réparation de ChatUniTest, qui relance le modèle avec le message d'erreur, n'a pas corrigé l'erreur de `setUp` en trois rounds, ce qui suggère que le modèle ne distingue pas un champ d'une méthode dans le code qu'on lui fournit.

### 4.6 `EndianUtils` : tests générés par ChatUniTest

Résultat : aucun test généré ne compile. Le dossier `tika-core/chatunitest/` n'a jamais reçu de test. Les sorties brutes sont dans `docs/chatunitest-logs/endianutils/`. Le modèle tourne sur CPU, donc les exécutions ont été interrompues avant la fin de la classe.

Méthodes tentées : `readShortLE, `readShortBE`, `readUShortLE` et `readUShortBE`. Aucune ne compile, et les 3 tours de réparation automatique échouent.

Cause principale : `cannot find symbol: BufferUnderrunException`. La classe est imbriquée et doit s'écrire `EndianUtils.BufferUnderrunException`. Le tour de réparation a ajouté un mauvais import, donc la même erreur est restée.

Tests conservés après correction à la main :

- `EndianUtilsReadUShortLEChatTest` (2 tests, Mockito) : 4 corrections.
  1. `EndianUtils.BufferUnderrunException` : à la main.
  2. Imports précis : à la main.
  3. `throws Exception` : à la main.
  4. En-tête de licence et saut de ligne final : à la main. Fins de ligne LF : Spotless.
- `EndianUtilsGeneratedTest` (4 tests) : réécrit à la main d'après la tentative de `readShortLE`. Ce n'est pas une sortie brute.

Critique : les oracles du modèle sont corrects (513 pour `01 02`, `0x3412`) mais ne couvrent que 2 méthodes sur une vingtaine. Mocker `InputStream` est inutile : un `ByteArrayInputStream` suffit. Les tests écrits à la main ciblent les frontières (zéros, `FF`, bit 31 d'un `long`, flux court).

Apport mesuré : +6 mutants tués (38 à 44 sur 206). Une fois les tests manuels présents, `EndianUtilsReadUShortLEChatTest` n'ajoute plus rien.

## 5. Analyse de mutation

Les rapports HTML et XML sont conservés dans `docs/pit-baseline/pit-reports/` (tests d'origine), `docs/pit-avec-generes/` (tests d'origine plus test généré corrigé) et `docs/pit-final/` (tous les tests). Les trois étapes portent sur les mêmes 140 mutants des deux classes ; la troisième exécute tous les tests ensemble, y compris le test généré, qui n'apportait aucun mutant supplémentaire à l'étape 2 et laisse bien le score inchangé à 135 sur 140 une fois combiné aux tests manuels.

| Étape | Tests analysés par PIT | Mutants tués | Score | Test strength |
|---|---|---|---|---|
| 1. Tests d'origine | `CharsetUtilsTest`, `FilenameUtilsTest` | 99 sur 140 | **71 %** | 83 % |
| 2. Origine plus test généré corrigé | les deux précédents plus `FilenameUtils_getSuffixFromPath_2_2_Test` (132 tests exécutés) | 99 sur 140 | **71 %** | 83 % |
| 3. Tous les tests (origine, généré corrigé, manuels) | les cinq classes de tests : `CharsetUtilsTest`, `FilenameUtilsTest`, `FilenameUtils_getSuffixFromPath_2_2_Test`, `CharsetUtilsManualTest`, `FilenameUtilsManualTest` (189 tests exécutés) | 135 sur 140 | **96 %** | 98 % |

| Classe | Mutants | Étape 1 | Étape 2 | Étape 3 |
|---|---|---|---|---|
| `FilenameUtils` | 115 | 79 (69 %) | 79 (69 %) | **110 (96 %)** |
| `CharsetUtils` | 25 | 20 (80 %) | 20 (80 %) | **25 (100 %)** |

La couverture de lignes des deux classes mesurée par PIT passe de 229/265 (86 %) à 255/265 (96 %) entre les étapes 1 et 3.

### 5.1 Quels mutants les tests d'origine détectent, et pourquoi

Les tests d'origine tuent 99 mutants parce que leurs assertions comparent des chaînes exactes, ce qui rend visible toute modification d'une valeur de retour ou d'un calcul d'indice. La répartition par type de mutation, avant et après les tests manuels, est la suivante :

| Mutateur | Générés | Tués à l'étape 1 | Tués à l'étape 3 |
|---|---|---|---|
| Mathématique (`MathMutator`) | 11 | 11 | 11 |
| Retours primitifs (`PrimitiveReturns`) | 2 | 2 | 2 |
| Négation de conditions (`NegateConditionals`) | 66 | 52 | 65 |
| Retours de chaîne ou d'objet vide (`EmptyObjectReturnVals`) | 37 | 21 | 35 |
| Frontières (`ConditionalsBoundary`) | 13 | 5 | 11 |
| Retours `null` (`NullReturnVals`) | 5 | 4 | 5 |
| Retours booléens `true` (`BooleanTrueReturnVals`) | 4 | 3 | 4 |
| Retours booléens `false` (`BooleanFalseReturnVals`) | 2 | 1 | 2 |

Les mutants arithmétiques sont tous tués dès l'étape 1, notamment parce que `testEmbeddedFileNames` compare la chaîne tronquée complète, de sorte que toute modification du calcul de longueur change le résultat. Les mutants de frontière sont à l'inverse les plus mal détectés (5 sur 13), parce qu'aucun test d'origine ne place une entrée exactement sur la limite d'une comparaison.

### 5.2 Le test généré ne détecte aucun mutant supplémentaire

Le score est identique aux étapes 1 et 2 : 99 mutants tués sur 140, avec la même couverture de lignes (229/265). Les cas du test généré (`example.txt`, `example`, chaîne vide, `null`, puis `.doc` une fois l'oracle corrigé) étaient déjà exercés, directement ou par les fonctions de nettoyage, dans `FilenameUtilsTest`, et aucun ne se place sur la limite de longueur de l'extension, seul endroit où un mutant (ligne 134) restait vivant. Les trois tentatives sur `CharsetUtils#forName` n'ont rien apporté non plus, puisqu'aucun test n'a pu être exécuté. Les tests générés ne détectent donc pas tous les mutants : ils n'en détectent aucun de plus que les tests d'origine.

### 5.3 Mutants tués par les tests manuels

L'étape 3 fait gagner 36 mutants par rapport à l'étape 1. Le tableau suivant, extrait de la comparaison des fichiers `mutations.xml` par le script de l'annexe A, donne pour chaque groupe le test qui le tue ; la raison est la même partout : avec le mutant, l'assertion décrite à la section 6 échoue, alors que les tests d'origine n'exécutaient pas la ligne ou n'en vérifiaient pas la valeur.

| Classe | Ligne(s) | Mutateur | Test qui le tue |
|---|---|---|---|
| `CharsetUtils` | 107, 108 | NegateConditionals, BooleanFalseReturnVals | `isSupportedConsultsIcuFirst` |
| `CharsetUtils` | 118 | BooleanTrueReturnVals | `isSupportedReturnsFalseWhenIcuThrows` |
| `CharsetUtils` | 186 | NegateConditionals | `forNameFallsBackToJdkWhenIcuReturnsNull` |
| `CharsetUtils` | 187 | NullReturnVals | `forNameReturnsTheCharsetFoundByIcu` |
| `FilenameUtils` | 134 | ConditionalsBoundary | `suffixLengthLimitIsFiveCharactersIncludingTheDot` |
| `FilenameUtils` | 174 | EmptyObjectReturnVals | `fileNameThatIsBlankAfterTheSlashIsRejected` |
| `FilenameUtils` | 189 | ConditionalsBoundary | `fileNameIsTruncatedOnlyWhenStrictlyLongerThanMaxLength` |
| `FilenameUtils` | 232, 250, 323 | EmptyObjectReturnVals, ConditionalsBoundary | `degeneratePathsAreRejected` |
| `FilenameUtils` | 264, 268, 344 | ConditionalsBoundary, EmptyObjectReturnVals, NegateConditionals | `pathKeepsDirectoryOnlyWhenItFitsInMaxLength` |
| `FilenameUtils` | 265 | ConditionalsBoundary | `longPathIsTruncatedToTheNamePartOnlyWhenNeeded` |
| `FilenameUtils` | 305, 308 | NegateConditionals | `resolveWithinRejectsSymlinkPointingOutside` |
| `FilenameUtils` | 348 à 359 | NegateConditionals, EmptyObjectReturnVals | `pathUsesTheFirstNonBlankKeyInPriorityOrder` |
| `FilenameUtils` | 365 | NegateConditionals | `fileNameIsTruncatedOnlyWhenStrictlyLongerThanMaxLength` |
| `FilenameUtils` | 369 à 380 | NegateConditionals, EmptyObjectReturnVals | `fileNameUsesTheFirstNonBlankKeyInPriorityOrder` |
| `FilenameUtils` | 402, 416 | EmptyObjectReturnVals | `calculateExtensionDefaultsAndLookups` |

### 5.4 Les cinq mutants qui ne sont pas tués

Les cinq mutants restants sont tous dans `FilenameUtils` et aucun test ne peut les tuer, ce que nous avons vérifié de deux manières : par lecture du code, puis en appliquant chaque modification à la main sur le source et en relançant les tests, ce qui a reproduit le verdict de PIT dans chaque cas (les modifications équivalentes laissent tous les tests au vert, alors que la négation de la ligne 215 fait échouer un test, comme PIT l'indique).

| Ligne | Mutateur | Statut PIT | Explication |
|---|---|---|---|
| 156 | ConditionalsBoundary (`> 0` devient `>= 0`) | SURVIVED | Si `prefixLength` vaut 0, `path.substring(0)` renvoie la même chaîne : mutant **équivalent** |
| 156 | NegateConditionals (`> 0` devient `<= 0`) | SURVIVED | Dans `getSanitizedEmbeddedFileName`, ne pas retirer le préfixe (`C:\`) ne change pas le résultat : la ligne suivante remplace `:` et `\` par `/`, puis `getName` ne garde que le dernier segment. Le préfixe n'atteint jamais le résultat : équivalent en pratique |
| 185 | EmptyObjectReturnVals (`return null` devient `""`) | NO_COVERAGE | Second test `isBlank(namePart)`, placé après des remplacements qui ne font qu'ajouter des `_` : le premier test a déjà refusé les noms vides, donc la condition n'est jamais vraie. **Code mort** |
| 215 | ConditionalsBoundary | SURVIVED | Même raison que la ligne 156 : **équivalent**. La négation de cette ligne est au contraire tuée par `degeneratePathsAreRejected`, car dans `getSanitizedEmbeddedFilePath` le préfixe `A:` doit bien être retiré |
| 259 | EmptyObjectReturnVals | NO_COVERAGE | Même second test `isBlank(namePart)` dans `getSanitizedEmbeddedFilePath` : **code mort** |

Une anomalie du code est apparue au passage : la Javadoc de `getSuffixFromPath` annonce une extension de « 5 caractères ou moins », mais le point compte dans la limite, de sorte que le code accepte au plus quatre caractères après le point (`.maha` oui, `.wahbi` non). Le test `suffixLengthLimitIsFiveCharactersIncludingTheDot` fige ce comportement réel. Autre écart avec la version amont de Tika : dans notre fork, `calculateExtension` renvoie `".bin"` pour un type inconnu, alors que la version récente renvoie la valeur par défaut. Les tests sont écrits pour la version du fork.

### 5.5 `EndianUtils` : analyse de mutation

Les trois étapes portent sur les mêmes 206 mutants. Les rapports PIT sont dans `docs/pit-reports/`.

Étape 1, tests d'origine (`EndianUtilsTest`) : 38 mutants tués sur 206 (18 %), 154 sans couverture, lignes 31/121 (26 %), test strength 73 %.

Étape 2, origine plus tests issus de ChatUniTest corrigés (`EndianUtilsGeneratedTest`, `EndianUtilsReadUShortLEChatTest`) : 44 tués sur 206 (21 %), 147 sans couverture, lignes 37/121 (31 %), test strength 75 %.

Étape 3, tous les tests (plus `EndianUtilsManualTest`) : 180 tués sur 206 (87 %), 0 sans couverture, lignes 120/121 (99 %), test strength 87 %.

Les tests d'origine n'appellent que quatre méthodes (`readUE7`, `readUIntLE`, `readUIntBE`, `readIntME`). Les 154 mutants sans couverture sont dans toutes les autres. Aucun mutant de frontière n'est tué, faute de test avec un flux de zéros.

Les tests générés ajoutent 6 mutants tués (38 à 44) : ils n'appellent que `readShortLE` et `readUShortLE`. Une fois les tests manuels présents, `EndianUtilsReadUShortLEChatTest` n'ajoute plus rien, le score reste 180/206 avec ou sans lui. La contribution de `EndianUtilsGeneratedTest` n'a pas été mesurée séparément. Les tests générés ne détectent donc pas tous les mutants.

Le gain de l'étape 3 (38 à 180) vient des tests manuels : octets distincts pour l'ordre, `FF` pour le signe, zéros pour les comparaisons, offset non nul, flux trop court pour le `throw`, bit 31 d'un `long`.

Mutants non tués à l'étape 3 : 26 (3 Increments, 1 ConditionalsBoundary, 22 Math). À COMPLÉTER après lecture du rapport HTML : classer chaque survivant en équivalent ou tuable.

Résultats combinés des trois classes (somme des mesures PIT de chaque classe, pas une exécution commune) :
- Étape 1 : 137 tués sur 346 (40 %), lignes 260/386 (67 %).
- Étape 2 : 143 tués sur 346 (41 %), lignes 266/386 (69 %).
- Étape 3 : 315 tués sur 346 (91 %), lignes 375/386 (97 %).
## 6. Tests ajoutés manuellement

Ces 20 tests, écrits pour tuer les mutants survivants de la section 2, vivent dans `FilenameUtilsManualTest` (15 tests) et `CharsetUtilsManualTest` (5 tests). Chacun est décrit ci-dessous par son nom, son intention, la raison du choix des données et la manière dont le résultat attendu est déterminé. Les deux fichiers ont été rédigés avec l'aide de l'assistant Claude à partir de la liste des mutants survivants, puis relus, compilés et exécutés par nous ; les 20 tests passent avec `mvn -pl tika-core test` (BUILD SUCCESS, aucun échec).

### 6.1 `CharsetUtilsManualTest`

ICU4J n'est pas sur le classpath de `tika-core`, donc les champs statiques privés `isSupportedICU` et `getCharsetICU`, de type `java.lang.reflect.Method`, valent `null` et les branches ICU ne s'exécutent jamais. Comme `Method` est une classe finale, Mockito ne peut pas la simuler, ce qui explique l'échec des tests générés. La classe de test injecte donc par réflexion de vraies méthodes statiques d'une classe interne `FakeIcu` dans ces champs, et restaure les valeurs d'origine après chaque test (`@BeforeEach` et `@AfterEach`) pour ne pas perturber les autres tests.

**`isSupportedConsultsIcuFirst`** (mutants des lignes 107 et 108). Ce test vérifie que `isSupported` interroge ICU avant le JDK. Les données sont `"fake-icu-charset"`, un nom inconnu du JDK mais que le faux ICU déclare supporté, `"UTF-8"`, connu du JDK, et `"unknown-charset-xyz"`, connu de personne : ces trois valeurs séparent les trois chemins possibles (ICU seul, JDK seul, aucun). L'oracle attendu est `true`, `true`, `false`, déduit directement du code : `true` si ICU répond oui, sinon le résultat de `Charset.isSupported`. Si la condition ICU était niée ou si son `return true` devenait `false`, le premier `assertTrue` échouerait.

**`isSupportedReturnsFalseWhenIcuThrows`** (mutant de la ligne 118). Ce test vérifie que toute exception levée par ICU est capturée et se traduit par `false`. La donnée est `"UTF-8"`, un nom pourtant valide, associé à un faux ICU qui lève une `IllegalStateException` : si l'exception n'était pas captée, ou si le `catch` renvoyait `true`, le test le verrait, alors que `Charset.isSupported("UTF-8")` aurait répondu `true` sans exception. L'oracle est `false`, parce que l'exception interrompt l'appel avant le test du JDK.

**`forNameReturnsTheCharsetFoundByIcu`** (mutant de la ligne 187). Ce test vérifie que `forName` renvoie le charset fourni par ICU. La donnée est `"fake-icu-charset"`, absent de la table des charsets courants et du JDK, avec un faux ICU qui renvoie `UTF_16` : le seul moyen d'obtenir ce résultat est donc de passer par la branche ICU. L'oracle est `assertSame(UTF_16, ...)`, et un mutant qui renverrait `null` ou laisserait le JDK lever `UnsupportedCharsetException` serait détecté.

**`forNameFallsBackToJdkWhenIcuReturnsNull`** (mutant de la ligne 186). Ce test vérifie que, si ICU ne connaît pas le nom, `forName` retombe sur le JDK. La donnée est `"UTF-32"`, qui n'est pas dans la table courante mais que le JDK connaît, avec un faux ICU qui répond `null` pour ce nom. L'oracle est `Charset.forName("UTF-32")` : si la condition `cs != null` était niée, `forName` renverrait `null` à la place.

**`forNameFallsBackToJdkWhenIcuThrows`** (clause `catch ... //ignore`). Ce test vérifie qu'une panne d'ICU est ignorée et que le JDK prend le relais. La donnée est `"UTF-32"` avec un faux ICU qui lève une `IllegalStateException`, que la réflexion enveloppe dans une `InvocationTargetException`, précisément l'exception que le code ignore. L'oracle est le charset du JDK, sans exception.

### 6.2 `FilenameUtilsManualTest` : `getSuffixFromPath`

**`suffixLengthLimitIsFiveCharactersIncludingTheDot`** (mutant de la ligne 134). Ce test fixe la limite de longueur de l'extension, exprimée par la condition `n.length() - i < 6`. Les données sont `"file.maha"`, dont l'extension `.maha` compte exactement 5 caractères, point compris, et `"file.wahbi"`, dont l'extension `.wahbi` en compte 6 : elles encadrent la frontière. L'oracle est `".maha"` pour la première et `""` pour la seconde, en comptant le point comme le fait le code ; un mutant `<=` accepterait `".wahbi"`.

### 6.3 `FilenameUtilsManualTest` : `getSanitizedEmbeddedFileName`

**`fileNameThatIsBlankAfterTheSlashIsRejected`** (mutant de la ligne 174). Un nom qui ne contient que des espaces après le dernier `/` n'est pas exploitable. La donnée est `"dir/ "`, qui n'est pas vide dans son ensemble (il y a un dossier) mais dont la partie nom devient vide après nettoyage ; l'oracle est `null`.

**`fileNameThatIsOnlyAnExtensionIsRejected`** (branche `extension.equals(fName)`). Un nom réduit à une extension n'a pas de partie nom. La donnée est `".txt"`, dont l'extension calculée est identique au nom entier ; l'oracle est `null`.

**`fileNameIsTruncatedOnlyWhenStrictlyLongerThanMaxLength`** (mutants des lignes 189 et 365). Ce test fixe la frontière de troncature `namePart.length() > maxLength`. Les données sont `"abcdefghij.txt"`, dont la partie nom fait exactement 10 caractères, avec `maxLength` = 10, puis `"abcdefghijk.txt"`, avec 11 caractères. L'oracle est le nom intact dans le premier cas ; dans le second, il vaut `"abc....txt"` : le code conserve `maxLength - len(".txt") - 3 = 3` caractères, puis ajoute `...` et l'extension. Un mutant `>=` tronquerait aussi le premier cas.

**`fileNameUsesTheFirstNonBlankKeyInPriorityOrder`** (mutants des lignes 369 à 380). Tika cherche le nom dans cinq métadonnées, dans l'ordre `RESOURCE_NAME_KEY`, `INTERNAL_PATH`, `EMBEDDED_RELATIONSHIP_ID`, `EMBEDDED_RESOURCE_PATH`, `ORIGINAL_RESOURCE_NAME`, en ignorant celles qui sont vides. Le test boucle sur les rangs i de 0 à 4 : les clés de rang inférieur reçoivent `" "` (blanc, donc ignorée) et les autres une valeur distincte `"k<j>.txt"`. L'oracle est `"k<i>.txt"` : le premier rang non vide l'emporte. Cette boucle exerce chaque condition `isBlank` et chaque retour une fois, ce qu'aucun test d'origine ne faisait.

### 6.4 `FilenameUtilsManualTest` : `getSanitizedEmbeddedFilePath`

**`degeneratePathsAreRejected`** (mutants des lignes 232, 250 et 323). Ce test rassemble les entrées qui ne contiennent aucun nom de fichier exploitable : `".."`, `"dir/.py"` (partie nom vide), `"A:"` et `"Z:"` (lettre de lecteur seule). Les deux lettres de lecteur sont choisies parce qu'elles sont les bornes de la plage `'A'` à `'Z'` testée par `getPrefixLength` : elles tuent les mutants `> 'A'` et `< 'Z'`. L'oracle est `null` dans chaque cas.

**`pathThatIsOnlyAnExtensionIsReturnedAsIs`** (branche `extension.equals(path)`). La donnée est `".java"` ; l'oracle est `".java"`, car le code renvoie dans ce cas l'extension elle-même, contrairement à `getSanitizedEmbeddedFileName` qui renvoie `null`.

**`pathKeepsDirectoryOnlyWhenItFitsInMaxLength`** (mutants des lignes 264, 268 et 344). La donnée est `"dir/file.txt"`, longue de 12 caractères, avec `maxLength` = 12 puis 11. L'oracle est le chemin intact pour 12 ; pour 11, le code abandonne le dossier et renvoie `"file.txt"`, car le nom seul tient dans la limite.

**`longPathIsTruncatedToTheNamePartOnlyWhenNeeded`** (mutant de la ligne 265). Les données sont `"d/mimawabc.txt"` avec `maxLength` = 8, dont la partie nom fait exactement 8 caractères, et `"d/abcdefghijk.txt"` avec `maxLength` = 10, dont la partie nom en fait 11. L'oracle est `"mimawabc.txt"` (nom conservé, dossier abandonné) puis `"abc....txt"` (nom tronqué, selon le même calcul que ci-dessus).

**`pathUsesTheFirstNonBlankKeyInPriorityOrder`** (mutants des lignes 348 à 359). C'est le pendant du test sur le nom, avec l'ordre propre à `getEmbeddedPath` : `EMBEDDED_RESOURCE_PATH`, `INTERNAL_PATH`, `RESOURCE_NAME_KEY`, `EMBEDDED_RELATIONSHIP_ID`, `ORIGINAL_RESOURCE_NAME`. Les valeurs sont `"d/f<j>.txt"` et l'oracle `"d/f<i>.txt"` pour le premier rang non vide.

### 6.5 `FilenameUtilsManualTest` : `resolveWithin` et `calculateExtension`

**`resolveWithinReturnsAnExistingFileInsideTheDirectory`** (ligne 305). La donnée est un fichier `a.txt` réellement créé dans un dossier temporaire ; l'oracle est son chemin normalisé. Ce cas fait entrer le code dans le bloc de vérification des liens symboliques, qui n'est exécuté que pour les chemins existants.

**`resolveWithinAcceptsNonExistingAndNormalizedNames`** (ligne 305). Les données sont `"new/sub.txt"`, qui n'existe pas, et `"x/../a.txt"`, dont le segment `..` doit être normalisé avant la vérification de contenance. L'oracle est le chemin normalisé du dossier suivi du nom, sans exception.

**`resolveWithinRejectsParentTraversal`** (condition de la ligne 299). Les données sont `"../x"` et `"../bc"`, ce dernier désignant un dossier frère dont le nom commence par celui du dossier courant `b`, ce qui piège les implémentations qui comparent des chaînes plutôt que des éléments de chemin. L'oracle est une `IOException` dont le message contient le nom fautif et le texte « outside of ».

**`resolveWithinRejectsSymlinkPointingOutside`** (mutants des lignes 305 et 308). Le test crée un dossier `inside` contenant un lien symbolique `link` vers un dossier `outside` voisin qui contient `secret.txt`. Pour `"link/secret.txt"` et pour `"link"`, le chemin logique est dans `inside` mais le chemin réel est dehors : seul le contrôle de défense en profondeur (`toRealPath`) le détecte. L'oracle est une `IOException`. Le test est ignoré (`assumeTrue`) si le système de fichiers refuse de créer des liens symboliques.

**`calculateExtensionDefaultsAndLookups`** (mutants des lignes 402 et 416). Les données sont une métadonnée sans type de contenu (oracle : la valeur par défaut `".dflt"`), `"application/x-tache2-unknown"` (type bien formé mais inconnu, oracle `".bin"`), `"not-a-media-type"` (type mal formé qui déclenche `MimeTypeException`, oracle `".bin"`) et `"image/png"` (type connu, oracle `".png"`). Ces quatre cas parcourent tous les retours de `calculateExtension` et de `lookupExtension`.

### 6.6 `EndianUtilsManualTest` (22 tests)

`EndianUtilsManualTest` exerce toutes les méthodes publiques de `EndianUtils` que les tests d'origine n'appelaient pas, en choisissant pour chacune des octets distincts (ordre), `FF` (signe), des zéros (comparaisons), un offset non nul, un flux trop court (`BufferUnderrunException`) et le bit 31 d'un `long`, avec des valeurs attendues calculées à la main d'après le code (par exemple `0x02010403` pour `readIntME` sur `01 02 03 04`). Les 22 tests passent (`mvn -pl tika-core test -Dtest=EndianUtilsManualTest`) et font passer le score de mutation de 21 % à 87 %.

## 7. GitHub Action

Le workflow `.github/workflows/tache2.yml` s'exécute à chaque `push`, chaque `pull_request` et à la demande. Il récupère le dépôt avec `actions/checkout@v4`, installe Temurin 17 avec cache Maven (`actions/setup-java@v4`), puis lance `mvn -B -pl tika-core -am test -Dtest='CharsetUtils*Test,FilenameUtils*Test' -Dsurefire.failIfNoSpecifiedTests=false`. Ce motif couvre ensemble les tests d'origine, le test généré corrigé et les tests manuels, soit 35 tests : 14 tests d'origine, 1 test généré corrigé et 20 tests manuels. Le workflow lance ensuite PIT et publie le dossier `tika-core/target/pit-reports` comme artefact du run.

ChatUniTest n'est pas exécuté dans la GitHub Action, car le runner ne dispose ni d'Ollama ni du modèle : les tests générés sont donc commités dans le dépôt et c'est leur version corrigée qui est exécutée. Badge : `![CI](https://github.com/<UTILISATEUR>/tika/actions/workflows/tache2.yml/badge.svg)`. Lien vers un run réussi : **À COMPLÉTER**.

## 8. Difficultés rencontrées

Le premier obstacle a été l'incompatibilité entre JUnit Platform 6.1.3 et la version 1.9.2 tirée par le plugin JUnit 5 de PIT, qui faisait échouer le générateur de couverture (section 3.1). La liste fermée de modèles de ChatUniTest a obligé à passer par un alias Ollama (section 3.3), et l'absence de Mockito dans `tika-core` a fait échouer la compilation des premiers tests générés (section 4.2). Le JDK 22 de la machine alors que Tika vise Java 17 produit un avertissement du processeur d'annotations sans conséquence. Plus tard, PIT n'examinait que les deux tests d'origine (`2 tests examined`) tant que `targetTests` n'était pas élargi aux nouveaux noms de classes. Enfin, Checkstyle et Spotless, actifs à chaque build de Tika, ont rejeté le test généré (en-tête de licence, imports `*`, imports inutilisés), ce qui oblige à nettoyer tout fichier généré avant de pouvoir lancer le moindre test. L'option `-DtestNumber=1` n'a pas non plus été prise en compte par le plugin (section 4.4). Enfin, nos premières mesures JaCoCo donnaient des chiffres presque identiques avant et après l'ajout des tests, parce que `jacoco.exec` accumule les exécutions successives ; supprimer ce fichier avant chaque mesure a résolu le problème (section 2).

## 9. Liens (pour le rendu)

- Dépôt : **À COMPLÉTER**
- Ce README : **À COMPLÉTER**

## Annexe A. Script de comparaison des rapports PIT

Ce script liste les mutants tués dans le second rapport mais pas dans le premier, avec le test qui les tue. Il fusionne les mutants distincts d'une même ligne qui ont le même mutateur et la même description : les totaux officiels sont donc ceux de PIT (`index.html`).

```bash
python3 - <<'EOF2'
import xml.etree.ElementTree as ET
def load(d):
    r={}
    for m in ET.parse(d+'/mutations.xml').getroot():
        c=m.findtext('mutatedClass').split('.')[-1]
        if c in ('FilenameUtils','CharsetUtils'):
            r[(c,m.findtext('lineNumber'),m.findtext('mutator').split('.')[-1],m.findtext('description'))]=(m.get('status'),m.findtext('killingTest'))
    return r
a=load('docs/pit-baseline/pit-reports'); b=load('docs/pit-final')
for k,(s,t) in sorted(b.items(), key=lambda x:(x[0][0],int(x[0][1]))):
    if s=='KILLED' and a[k][0]!='KILLED': print(k[0],k[1],k[2],'<-',(t or '').split('method:')[-1])
EOF2
```

Souha : J'ai utilisé claude pour aider a générer et corriger du code et corriger mon rapport