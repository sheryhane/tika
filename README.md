# Tâche 3 : Semgrep dans la CI d’Apache Tika
## 1. Équipe et classes choisies
- Étudiant(e)(s) : Amhaouch Maha 
- Étudiant(e)(s) : Souha Wahbi 
- dépôt : https://github.com/sheryhane/tika (branche de la tache )
- Classe modifiée : ProcessUtils.java (tika-core/src/main/java/org/apache/tika/utils/ProcessUtils.java) 
Toutes les vulnérabilités ont été introduites dans la même classe, sous forme de méthodes ‘public static’ à la toute fin (section 4)

## 2. Justification du choix des classes et des méthodes 
On a décidé de regrouper les 7 vulnérabilités dans la même classe ProcessUtils pour trois raison: 
- elle fait partie de tika-core, le module dont tika au complet dépend donc une faille dans ce module va toucher tout le projet.
- -Elle sert à lancer des programmes externes (execute, checkCommand) donc le code qu’on y trouve est sensible.
- -Elle ne contient que des méthodes statiques donc chaque faille est une méthode ajoutée à la fin, facile à repérer, à expliquer et à enlever.
  
Le code ajouté au complet compile et passe toutes les vérifications du projet. On a choisi chaque méthode de façon à représenter un type de vulnérabilité différent : Le choix du type, la raison et le résultat Semgrep sont détaillés à la section 4.

## 3. Worklflow GitHub Actions
Etapes:

On a ajouté semgrep au début du workflow .github/workflows/main-jdk17-build.yml.
 Build (needs : semgrep) et e2e-tests dépendent de lui ce qui fait que Maven est seulement déclenché dans le cas ou Semgrep réussit et ne trouve aucune nouvelle faille :
Or, si Semgrep trouve une nouvelle faille, tout s’arrête.

Partie importante :
- name: Semgrep - échoue si une nouvelle vulnérabilité est introduite 
    env: 
BASE_SHA: ${{ github.event.pull_request.base.sha || github.event.before }} 
    run: | 
if [ -z "$BASE_SHA" ] || [ "$BASE_SHA" = "0000000000000000000000000000000000000000" ]; then 
    BASE_SHA=$(git rev-parse HEAD~1) 
fi 
semgrep scan --config p/java --config p/security-audit --error - -baseline-commit "$BASE_SHA" 
- name: Semgrep - comparatif avec le code avant les vulnérabilités
     if: always() 
     run: | 
         semgrep scan --config p/java --config p/security-audit -- baseline-commit bc3d0aed83c45b17cfa9b254737b4ae059c916c0

 
Justification:
+ baseline (– baseline-commit): fait la différence entre une nouvelle faille et une faille déjà présente.  Tika avait initialement 19 alertes Semgrep donc sans le baseline la CI aurait été toujours rouge. Semgrep signale les failles ajoutées après avoir analysé et comparé le commit de référence et le commit courant.
+ le commit de référence (BASE_SHA) : quand on fait un pull request, on va prendre la base de la PR (github.event.pull_request.base.sha) et quand on fait un push on va prendre le dernier committ déjà poussé (github.event.before). Au premier push d’une branche, git n’en a pas (il retourne que des 0) donc on prend le commit parent (HEAD-1)
+ fetch-depth : 0 : Semgrep necessite l’historique git au complet pour être en mesure de comparer avec un ancien commit.
+ --error : Lorsqu’une faille est détéctée, il renvoie le code 1 ce qui empêche le build et rend le job rouge.
+  p/java et p/security-audit: p/java seul(60règles) n’est pas en mesure de détecter l’injection SQL. Les deux ensembles donnent 259 règles gratuites.
+ BASE_SHA passé par env : écrire${{…}} directement dans le script permet une injection de commande.
+L’étape de comparaison : permet de comparer le code au commit (avant la première faille) et affiche toutes les failles d’un coup.
Vérification :
On a poussé les failles une à la fois sur la branche ‘tache3’ et on vérifie les résultat à chaque fois :

| Commit poussé | Résultat de la CI|
|………|……..|
|Faille 1, MD5  | Echec : MD5 détecté |
|Faille 3, SQL avec p/java seul | Echec : mais le SQL n’est pas détecté : il manquait des règles|
|Correction du SQL avec p/security-audit| Echec : injection SQL détectée |
| Faille 4, désérialisation | Echec : désérialisation détectée |
| Faille 5, 6 et 7 | Echec : nom de serveur, socket et XXE détectés |
| Ajout de l’étape de comparaison | Succès : aucune nouvelle faille. La comparaison affiche les 8 alertes des 7 failles|
Le dernier run est très important vu qu’il démontre que la CI ne bloque pas un commit sans danger(modification du workflow) mais qu’elle bloque tous ce qui ajoute une faille.
## 4. Les 7 vulnérabilités
| # | Méthode | Faille | Ligne | Règle Semgrep | CWE | OWASP 2025 |
|---|---------|--------|-------|---------------|-----|------------|
| 1 | hashText | Hachage MD5 | 329 | use-of-md5 | CWE-328 | A04 Cryptographie |
| 2 | encryptAES | Chiffrement en mode ECB | 348 | ecb-cipher, use-of-aes-ecb | CWE-327 | A04 Cryptographie |
| 3 | findDocument | Injection SQL | 368 | formatted-sql-string | CWE-89 | A05 Injection |
| 4 | deserializeObject | Désérialisation | 399 | object-deserialization | CWE-502 | A08 Intégrité des données |
| 5 | acceotHosts | Nom de serveur non vérifié | 426 | insecure-hostname-verifier | CWE-297 | A07 Authentification |
| 6 | connect | Connexion non chiffrée | 444 | unencrypted-socket | CWE-319 | A04 Cryptographie |
| 7 | createSaxParser | XML mal configuré (XXE) | 451 | saxparserfactory-disallow-doctype-decl-missing | CWE-611 | A02 Mauvaise configuration |

Les 7 failles engendrent 8 alertes (la faille deux est détectée par deux règles en même temps) : Les failles 1, 2 et 6 touchent la cryptographie mais pas au même endroit : 1 touche le calcul d’une empreinte, 2 touche la façon de chiffrer et 6 touche le transport des données donc ce sont bien trois erreurs différentes.
1. Hachage MD5 (hashText). La méthode sert à emprunter du texte et engendre un type de résume ( est censé être unique) avec MessageDigest.getInstance("MD5"). Or, vu que MD5 est cassé depuis longtemps cela cause un problème : On sait comme faire deux textes différents qui ont la même empreinte donc on ne peut plus s’en servir pour vérifier qu’un fichier n’a pas été modifié. Il faut donc utiliser SHA-256. Cette faille est simple et facile à implémenter donc idéale comme première faille ( c’est aussi un des exemples qu’on a vu en cours avec Bandit)
2. Chiffrement en mode ECB (encryptAES). La méthode chiffre un texte avec AES mais en mode ECB : Cipher.getInstance("AES/ECB/PKCS5Padding"). Or, en mode ECB, le texte va être découpé en blocs et chaque bloc va être chiffré tout seul avec une même clé. Donc on obtient deux blocs identiques vont donner deux blocs chiffrés de façon identique. Donc si quelqu’un regarde les données chiffrées : il va voir des répétitions et peut même déplacer des blocs sans connaître la clé. Pour y remédier il va falloir utiliser le mode GCM. Donc cette faille est plus subtile que le MD5 vu que bien que le parce que le mode soit mauvais, l’algorithme est bon. 
 3. Injection SQL (findDocument) : La méthode cherche un document par son nom en collant directement le nom reçu dans la requête : "SELECT * FROM documents WHERE name = '" + name + ". La façon idéale de faire est d’utiliser un PreparedStatement où le nom est traité comme une donnée et non comme du code SQL. 
4. Désérialisation (deserializeObject). La méthode reconstruit un objet Java avec new ObjectInputStream(input) à partir d’octet qu’elle reçoit. Le problème qui se présente est que ce sont les octets reçus qui décident quels objets Java sont recréés. Du code peut également s’exécuter pendant cette reconstruction. Un attaquant peut également en profiter si ces octets viennent de l’extérieur. C’est une des failles Java les plus graves. Pour y remédier on peut utiliser un format simple comme JSON pour les données qui viennent de l’extérieur. 
5. Nom de serveur non vérifié (acceotHosts) : En HTTPS le programme doit vérifier que le certificat appartient bien au serveur qu’il essaie de joindre. La méthode verify répond toujours return true car elle accepte n’importe quel serveur. Un attaquant qui possède un certificat valide pour son site peut alors se faire passer pour un autre. Ce genre de code est souvent copié d’Internet pour détourner des erreurs de certificat. Il faut garder la vérification par défaut de Java. Tika a déjà un équivalent dans HttpClientFactory.java. 
6. Connexion non chiffrée (connect). La méthode ouvre une connexion réseau avec new Socket(host, port). Tout ce qui passe par cette connexion circule en clair comme en HTTP de façon ç ce que n’importe qui sur le même réseau peut lire les données. Tika traite souvent des documents sensibles (contrats, courriels, etc) donc Il faut une connexion chiffrée (SSLSocket), l’équivalent du HTTPS. 
7. XML mal configuré, ou XXE (createSaxParser) : La méthode crée un lecteur de fichiers XML avec les réglages par défaut : SAXParserFactory.newInstance().  Ce lecteur accepte qu’un fichier XML lui demande d’aller lire d’autres fichiers. Un fichier piégé peut faire lire au serveur des fichiers qu’il n’est pas censé lire. C’est grave pour Tika vu que son métier est de lire des documents venant d’inconnus et plusieurs formats contiennent du XML. Il faut donc la désactiver.

## 5. Comparaison avant/ après
On a lancé Semgrep sur tout le dépôt,  avec les mêmes règles que la CI. Une fois sur le commit juste avant la première faille et une autre fois sur la branche tache3 avec les 7 failles
||Avant|Après|Différence|
|---|--------------|--------|---------------------| 
|tika au complet|19|27|+8|
|ProcessUtils.java|0|8|+8|	

Les 8 nouvelles alertes correspondent exactement aux 7 failles (rappel : la 2 compte deux fois). Les 19 alertes qui apparaissaient avant existaient déjà et ne font pas échouer la CI grâce au baseline.
## Humour : rickroll (pas drôle btw)
On a créer une action qu’on peut réutiliser (.github/actions/rickroll) qu’on appelle à la fin de build et e2e-tests si l’étape Maven a échoué :
-name : Rickroll si un test echoue 
 if : failure () && steps.maven.outcome == ‘failure’ 
uses: ./.github/actions/rickroll
 On n’a pas réutiliser random-rickroll vu qu’elle ne réagit qu’à l’ouverture d’une issue et pas à test qui échoue. Ensuite, pour vérifier que ça marche effectivement on a jouté exprès un test qui échoue. Après une correction de ce test, le run du commit a bien affiché l’annotation « rickrolled » avec le lien de la chanson, puis on l’a retiré.
On a aussi découvert une autre limite : Lors de notre première exécution le rickroll a été déclenché quand une verification d’en-tête a échoué donc on a déduit que l’étape maven ne lance pas que les tests mais qu’elle vérifie aussi d’autres choses (notamment les entêtes de licenses).

