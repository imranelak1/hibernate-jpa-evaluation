# Évaluation — Stock, projets et état civil

Solution du lab **159** du [cours Hibernate & JPA](https://mliaedu.toubkalit.com/student/courses/28). Les **trois applications** demandées sont dans ce dépôt indépendant, avec une architecture DAO/service et une démonstration commune. Java 17, Hibernate 5.6.15.Final, JPA 2.2, H2 1.4.200, MySQL Connector 8.0.33 et JUnit 4.13.2.

Les noms de packages et d'attributs suivent les modèles demandés ; `Integer`, `LocalDate`, `float` pour le prix produit et `double` pour le prix tâche sont utilisés. [docs/CONCEPTION.md](docs/CONCEPTION.md) présente nos schémas de relations et la structure de l'implémentation.

## Exécution sans serveur externe

```bash
mvn clean test
mvn exec:java
```

`application.properties` configure H2. `ma.projet.util.HibernateUtil` enregistre les 12 entités et partage une seule factory. Chaque opération ferme son contexte ; les écritures sont transactionnelles et annulées en cas d'exception. Les associations `@MapsId` utilisent des références rattachées au contexte courant.

## Exécution sur MySQL réel

```bash
docker compose up -d --wait
mvn -Dtest.db=mysql test
mvn -Ddb=mysql exec:java
docker compose down
```

Le fichier `compose.yaml` démarre uniquement un conteneur de laboratoire MySQL **8.4.0**, sur `127.0.0.1:3318`, avec la base `hibernate_evaluation`. Les identifiants `lab/labpass` sont réservés à cette base locale jetable. `application-mysql.properties` utilise le dialecte `MySQL8Dialect`. La démo génère les tables avec `hbm2ddl.auto=create` ; les tests utilisent `create-drop`. Pour une autre base de laboratoire, définir `DB_URL`, `DB_USER`, `DB_PASSWORD`.

Les deux bases ont été réellement exécutées : **14 tests passent sur H2 et les mêmes 14 tests passent sur MySQL**, sans échec, erreur ou test ignoré. Les preuves MySQL sont [les tests](evidence/mysql-test-output.txt), [la démonstration](evidence/mysql-demo-output.txt), [le schéma réellement généré](evidence/mysql-schema.sql) et [les tables/compteurs SQL](evidence/mysql-verification.txt). Le conteneur créé pour la vérification a ensuite été supprimé.

Le journal MySQL conserve les avertissements Hibernate de suppression de contraintes/tables absentes lors de la préparation `create-drop` d'une base neuve. Les créations suivantes et les tests réussissent ; le schéma final et les compteurs ont aussi été vérifiés directement par SQL. Les avertissements de nettoyage ne correspondent pas à des erreurs JUnit.

## Exercice 1 — Gestion de stock

Entités `ma.projet.classes.Categorie`, `Produit`, `Commande`, `LigneCommandeProduit` et clé composée `LigneCommandeId`. Le diagramme nomme la ligne `LigneCommandeProduit` ; le service demandé s'appelle `LigneCommandeService`. Les services `ProduitService`, `CategorieService`, `CommandeService`, `LigneCommandeService` implémentent l'interface générique `ma.projet.dao.IDao` via `AbstractService`.

- Produits par catégorie ; produits commandés entre deux dates inclusives, sans doublons.
- Lignes d'une commande avec référence, prix et quantité ; détail accessible après fermeture du contexte.
- Requête nommée de prix **strictement supérieur à 100 DH**.
- CRUD persistant des lignes à clé composée ; contrôle des quantités et des doublons dans une commande.

La commande du 14 mars 2013 contient `ES12 / 120 DH / 7`, `ZR85 / 100 DH / 14`, `EE85 / 200 DH / 5`, soit **3 240 DH**. La requête nommée retourne ES12 et EE85. La commande d'avril permet de vérifier les bornes de dates. Les calculs du montant convertissent les valeurs de prix en `BigDecimal` pour éviter une accumulation en flottant.

## Exercice 2 — Gestion de projets

Entités `ma.projet.classes.Projet`, `Tache`, `Employe`, `EmployeTache` et clé composée `EmployeTacheId`. Services `ProjetService`, `TacheService`, `EmployeService`, `EmployeTacheService`.

- Tâches réalisées d'un employé et projets dont il est chef.
- Tâches planifiées du projet et tâches réalisées avec les dates réelles de l'association.
- Requête nommée de prix **strictement supérieur à 1 000 DH** ; tâches réalisées entièrement entre deux dates inclusives.
- Coût global planifié et réalisé, comptage des jours réellement passés ; dates incohérentes rejetées.

La démo affiche les dates réelles d'Analyse, Conception et Développement. Recette reste planifiée et inachevée. Le coût planifié est **5 500 DH**, le coût réalisé **4 700 DH**, le temps écoulé **30 jours**. Le coût réalisé compte chaque tâche une seule fois même si plusieurs employés y sont affectés.

## Exercice 3 — État civil

Entités `ma.projet.beans.Personne` (abstraite, héritage `JOINED`), `Homme`, `Femme`, `Mariage`, clé composée `MariageId` incluant homme, femme et date de début pour autoriser un remariage du même couple. Services `HommeService`, `FemmeService`, `MariageService`.

- Création de **10 femmes et 5 hommes**, affichage des femmes et de la plus âgée : ALAMI Karima, née le 1 janvier 1960.
- Épouses d'un homme sur une période, avec prise en compte des mariages terminés et des mariages en cours (`dateFin=null`).
- **Requête SQL native nommée** retournant le nombre d'enfants déclarés pour une femme sur la période ; Salima retourne **4**.
- **Requête JPQL nommée** pour les femmes mariées au moins deux fois ; Karima retourne deux mariages.
- **Criteria API** avec sous-requête corrélée et `countDistinct` pour le nombre d'hommes ayant exactement quatre épouses distinctes sur la période ; résultat **1** entre 2000 et 2020.
- Historique détaillé de SAFI Said : **4 mariages en cours**, **1 mariage terminé** ; femmes, dates et nombres d'enfants affichés.

Les périodes matrimoniales sont inclusives et un mariage est retenu lorsqu'il chevauche l'intervalle demandé. « Quatre femmes entre deux dates » désigne ici quatre épouses distinctes sur les unions chevauchant la période ; le modèle ne précise pas une condition de simultanéité. Le modèle ne contient pas les dates de naissance des enfants : la requête native additionne les nombres déclarés des mariages retenus, elle ne compte pas des naissances sur la période. Ces conventions sont documentées et testées.

## Preuves et captures

[Sortie de démonstration H2](evidence/demo-output.txt) ; [sortie des 14 tests H2](evidence/test-output.txt). Les tests vérifient les trois cas, les bornes/seuils, les clés composées, l'héritage, les données manquantes et le rollback/fermeture des sessions.

Les captures montrent un rapport navigateur construit à partir des **sorties d'exécution réelles** et des rapports JUnit sauvegardés. Les trois exercices figurent dans la sortie de démonstration et les preuves MySQL.

![Résultats des trois exercices](screenshots/results.png)

![Tests de persistance](screenshots/tests.png)

![Vérification SQL et tests sur MySQL réel](screenshots/mysql.png)

Le tableau des scénarios utilise les XML Surefire de la dernière exécution sur MySQL. `evidence/test-output.txt` conserve la première exécution H2; `evidence/mysql-test-output.txt` conserve l'exécution MySQL correspondante.

## Références

[Guide officiel Hibernate 5.6 — modèle et requêtes](https://docs.hibernate.org/orm/5.6/userguide/html_single/) ; [Java EE — Criteria API](https://javaee.github.io/tutorial/persistence-criteria.html) ; [image MySQL officielle Docker](https://hub.docker.com/_/mysql).

## Régénérer le rapport et les captures

Les images sont des captures navigateur de résultats réellement exécutés, conservés dans `evidence/`. Les tests proviennent des XML Surefire, également archivés dans `evidence/junit/`. Les empreintes SHA-256 des sources figurent dans `evidence/checksums.json`.

Avec Python 3 et Node.js disponibles, depuis la racine du dépôt :

```sh
mvn clean verify > evidence/test-output.txt 2>&1
mvn exec:java > evidence/demo-output.txt 2>&1
python scripts/generate_report.py
npm install --no-save playwright
npx playwright install chromium
node scripts/capture-results.cjs
```

Le rapport se consulte aussi directement dans `evidence/report.html`. Les captures générées sont `screenshots/results.png` et `screenshots/tests.png`.
