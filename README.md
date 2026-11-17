# Tâche 3 : Semgrep dans la CI d'Apache Tika

## 1. Équipe et classe choisie

- Étudiante : Maha Amhaouch
- Étudiante : Souha Wahbi
- Dépôt : https://github.com/sheryhane/tika (branche `tache3`)
- Classe modifiée : `ProcessUtils.java` (`tika-core/src/main/java/org/apache/tika/utils/ProcessUtils.java`)

Toutes les vulnérabilités ont été introduites dans la même classe, sous forme de méthodes `public static` ajoutées à la fin (voir la section 4).

## 2. Justification du choix de la classe

On a regroupé les 7 vulnérabilités dans la classe `ProcessUtils` pour trois raisons :

- elle fait partie de **tika-core**, le module dont tout Tika dépend : une faille à cet endroit touche tout le projet ;
- elle sert à **lancer des programmes externes** (`execute`, `checkCommand`), donc on y trouve naturellement du code sensible ;
- elle ne contient que des **méthodes statiques** : chaque faille est une méthode ajoutée à la fin, facile à repérer, à expliquer et à enlever.

Tout le code ajouté compile et passe les vérifications du projet. Chaque méthode représente un type de vulnérabilité différent. Le type, la raison et le résultat Semgrep sont détaillés à la section 4.

## 3. Workflow GitHub Actions

### Étapes

On a ajouté un job `semgrep` au début du workflow `.github/workflows/main-jdk17-build.yml`. Le job `build` en dépend (`needs: semgrep`), et `e2e-tests` dépend de `build` : Maven ne se lance que si Semgrep ne trouve aucune nouvelle faille. Si Semgrep en trouve une, tout s'arrête.

Partie importante :

```yaml
- name: Semgrep - échoue si une nouvelle vulnérabilité est introduite
  env:
    BASE_SHA: ${{ github.event.pull_request.base.sha || github.event.before }}
  run: |
    if [ -z "$BASE_SHA" ] || [ "$BASE_SHA" = "0000000000000000000000000000000000000000" ]; then
      BASE_SHA=$(git rev-parse HEAD~1)
    fi
    semgrep scan --config p/java --config p/security-audit --error --baseline-commit "$BASE_SHA"

- name: Semgrep - comparatif avec le code avant les vulnérabilités
  if: always()
  run: |
    semgrep scan --config p/java --config p/security-audit --baseline-commit bc3d0aed83c45b17cfa9b254737b4ae059c916c0
```

### Justification

- **Baseline (`--baseline-commit`)** : Tika avait déjà 19 alertes. Sans baseline, la CI serait toujours rouge. Avec elle, Semgrep ne signale que les failles **nouvelles** par rapport à un commit de référence.
- **Commit de référence (`BASE_SHA`)** : la base de la PR pour une pull request, le dernier commit poussé pour un push. Au premier push d'une branche, il n'y en a pas : on prend le commit parent (`HEAD~1`).
- **`fetch-depth: 0`** : Semgrep a besoin de tout l'historique pour comparer avec un ancien commit.
- **`--error`** : une faille détectée rend le job rouge et bloque le build.
- **`p/java` + `p/security-audit`** : `p/java` seul ratait l'injection SQL ; les deux ensemble donnent 259 règles gratuites.
- **`BASE_SHA` dans `env:`** : écrire `${{ ... }}` directement dans le script permettrait une injection de commande.
- **Étape de comparaison** : compare le code au commit d'avant la première faille et affiche toutes nos failles d'un coup, sans bloquer le build.

### Vérification

On a poussé les failles une à la fois sur la branche `tache3` et vérifié le résultat à chaque fois :

| Commit poussé | Résultat de la CI |
| --- | --- |
| [Faille 1, MD5](https://github.com/sheryhane/tika/actions/runs/43784664959) | Échec : MD5 détecté |
| [Faille 3, SQL, avec `p/java` seul](https://github.com/sheryhane/tika/actions/runs/43792282087) | Échec, mais le SQL n'est pas détecté : il manquait des règles |
| [Correction du SQL, avec `p/security-audit`](https://github.com/sheryhane/tika/actions/runs/43795871180) | Échec : injection SQL détectée |
| [Faille 4, désérialisation](https://github.com/sheryhane/tika/actions/runs/43801350991) | Échec : désérialisation détectée |
| [Failles 5, 6 et 7](https://github.com/sheryhane/tika/actions/runs/43835774948) | Échec : nom de serveur, socket et XXE détectés |
| Ajout de l'étape de comparaison | Succès : aucune nouvelle faille. La comparaison affiche les 8 alertes des 7 failles |

Le dernier run est important : il montre que la CI laisse passer un commit sans danger (une modification du workflow), mais bloque tous ceux qui ajoutent une faille.

## 4. Les 7 vulnérabilités

| # | Méthode | Faille | Ligne | Règle Semgrep | CWE | OWASP 2025 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `hashText` | Hachage MD5 | 329 | `use-of-md5` | CWE-328 | A04 Cryptographie |
| 2 | `encryptAES` | Chiffrement en mode ECB | 348 | `ecb-cipher`, `use-of-aes-ecb` | CWE-327 | A04 Cryptographie |
| 3 | `findDocument` | Injection SQL | 368 | `formatted-sql-string` | CWE-89 | A05 Injection |
| 4 | `deserializeObject` | Désérialisation | 399 | `object-deserialization` | CWE-502 | A08 Intégrité des données |
| 5 | `acceotHosts` | Nom de serveur non vérifié | 426 | `insecure-hostname-verifier` | CWE-297 | A07 Authentification |
| 6 | `connect` | Connexion non chiffrée | 444 | `unencrypted-socket` | CWE-319 | A04 Cryptographie |
| 7 | `createSaxParser` | XML mal configuré (XXE) | 451 | `saxparserfactory-disallow-doctype-decl-missing` | CWE-611 | A02 Mauvaise configuration |

Les 7 failles donnent 8 alertes, car la faille 2 est détectée par deux règles. Les failles 1, 2 et 6 touchent toutes la cryptographie, mais à trois endroits différents : le calcul d'une empreinte, la façon de chiffrer et le transport des données.

**1. Hachage MD5 (`hashText`).** La méthode calcule l'empreinte d'un texte avec `MessageDigest.getInstance("MD5")`. Or MD5 est cassé : on sait créer deux textes différents avec la même empreinte, donc il ne prouve plus qu'un fichier n'a pas été modifié. Il faut utiliser SHA-256. C'est la faille la plus simple, et un des exemples vus en cours avec Bandit.

**2. Mode ECB (`encryptAES`).** La méthode chiffre avec AES, mais en mode ECB : `Cipher.getInstance("AES/ECB/PKCS5Padding")`. ECB chiffre chaque bloc séparément, donc deux blocs identiques donnent le même résultat. Un attaquant voit des répétitions sans connaître la clé. Il faut utiliser le mode GCM. La faille est subtile : l'algorithme est bon, seul le mode est mauvais.

**3. Injection SQL (`findDocument`).** La méthode colle le nom reçu dans la requête : `"... WHERE name = '" + name + "'"`. Avec `' OR '1'='1`, la condition est toujours vraie et on récupère toute la table. Il faut un `PreparedStatement`, qui traite le nom comme une donnée et non comme du SQL.

**4. Désérialisation (`deserializeObject`).** La méthode recrée un objet Java à partir d'octets reçus, avec `new ObjectInputStream(input)`. Ce sont ces octets qui décident quels objets sont recréés, et du code peut s'exécuter pendant l'opération. S'ils viennent d'un attaquant, il en profite. Il vaut mieux utiliser un format simple comme JSON.

**5. Nom de serveur non vérifié (`acceotHosts`).** En HTTPS, le programme doit vérifier que le certificat appartient au bon serveur. Ici, `verify` répond toujours `true` : un attaquant peut se faire passer pour n'importe quel serveur. Il faut garder la vérification par défaut de Java.

**6. Connexion non chiffrée (`connect`).** La méthode ouvre une connexion avec `new Socket(host, port)`. Les données circulent en clair, comme en HTTP, et n'importe qui sur le réseau peut les lire. Tika traite souvent des documents sensibles : il faut une connexion chiffrée (`SSLSocket`).

**7. XXE (`createSaxParser`).** La méthode crée un lecteur XML avec les réglages par défaut : `SAXParserFactory.newInstance()`. Un fichier XML piégé peut alors lui faire lire des fichiers du serveur. C'est grave pour Tika, qui lit des documents d'inconnus, dont beaucoup contiennent du XML. Il faut désactiver cette option avec `setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)`.

## 5. Comparaison avant / après

On a lancé Semgrep sur tout le dépôt, avec les mêmes règles que la CI : une fois sur le commit juste avant la première faille (`bc3d0ae`), et une fois sur la branche `tache3` avec les 7 failles.

| | Avant | Après | Différence |
| --- | --- | --- | --- |
| Tika au complet | 19 | 27 | +8 |
| `ProcessUtils.java` | 0 | 8 | +8 |

Les 8 nouvelles alertes correspondent exactement aux 7 failles (la faille 2 compte deux fois). Les 19 alertes d'avant existaient déjà et ne font pas échouer la CI grâce à la baseline.

## 6. Ce que Semgrep n’a pas trouvé
On a essayé deux failles que la CI n’a pas été en mesure de détecter et on a décidé de les laisser dans le fichier parce qu’elle démontre les limites de Semgrep :
-**XXE avec ‘DocumentBuilderFactory** : elle est détectée avec les règles payantes (pro) mais invisible en CI sans compte. Donc le même outil ne donne pas le même résultat.
-**Injection de commande** : ‘host’ est concaténé dans une commande shell. Les règles d’injection gratuites ne se déclenchent que si elle voient l’origine de la donnée (par exemple : requête web). Or, ici elle n’est pas détectée vu que c’est un simple paramètre.
Donc semgrep detecte des erreurs mais il ne remplace ni les test ni une relecture humaine.


## 7. Humour : rickroll (pas drôle btw)

On a créé une action réutilisable (`.github/actions/rickroll`), appelée à la fin de `build` et `e2e-tests` si l'étape Maven a échoué :

```yaml
- name: Rickroll si un test echoue
  if: failure() && steps.maven.outcome == 'failure'
  uses: ./.github/actions/rickroll
```

On n'a pas réutilisé `random-rickroll`, parce qu'elle ne réagit qu'à l'ouverture d'une issue, pas à un test qui échoue.

Pour vérifier que ça marche, on a ajouté exprès un test qui échoue. Après une correction de ce test, le run du [commit `3ee170d`](https://github.com/sheryhane/tika/actions/runs/43675130197) a bien affiché l'annotation « Rickrolled » avec le lien de la chanson. On a ensuite retiré ce test (commit `bc3d0ae`).

On a aussi découvert une limite : lors d'une exécution, le rickroll s'est déclenché parce qu'une vérification d'en-tête de licence avait échoué. L'étape Maven ne lance donc pas que les tests, elle vérifie aussi d'autres choses, comme les en-têtes de licence.
