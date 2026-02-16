# Utvecklarguide

Detta är en snabb och smutsig guide om hur man börjar koda på ETERNA.

## Hämta källkoden

Du kan enkelt hämta källkoden genom att klona projektet till din maskin (bara behöver git installerat):

```bash
$ git clone https://github.com/ETERNA-earkiv/ETERNA.git
```

Om du planerar att bidra till ETERNA behöver du först förgrena arkivet till ditt eget GitHub-konto och sedan klona det till din maskin. För att lära dig hur du gör det, vänligen kolla denna [GitHub-artikel](https://help.github.com/articles/fork-a-repo).


<!-- WARNING: changing this title will break links -->
## Hur man bygger och kör

ETERNA använder byggsystemet [Apache Maven](http://maven.apache.org/). Som ett flermoduls-Maven-projekt deklareras all viktig information för alla moduler i ETERNA i roten **pom.xml**, såsom:

* Moduler som ska inkluderas i standardbyggcykeln
* Maven-arkiv som ska användas
* Beroendehantering (versionsnummer deklareras här och ärvs av undermodulerna)
* Plugin-hantering (versionsnummer deklareras här och ärvs av undermodulerna)
* Tillgängliga profiler (Det finns många användbara profiler. En som endast inkluderar kärnprojekten (**core**), en annan som inkluderar användargränssnittsprojekt (**wui**), en annan som bygger ETERNA wui docker-bild (**wui,roda-wui-docker**), och några andra som till exempel kan inkludera externa plugin-projekt som kan integreras i ETERNA (**all**)).

### Beroenden

Förutsättningarna för att bygga ETERNA är:

* Git-klient
* Apache Maven 3.8+
* GitHub-konto, [konfigurera Maven att använda ditt Github-konto](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token).
* Oracle Java 21


### Kompilering

För att kompilera, gå till ETERNA-källmappen och kör kommandot:

```bash
$ mvn clean package
```

Använd följande kommando för att hoppa över enhetstesterna (snabbare).

```bash
$ mvn clean package -Dmaven.test.skip=true
```

Efter en lyckad kompilering kommer ETERNA webbapplikation att vara tillgänglig på `roda-ui/roda-wui/target/roda-wui-VERSION.war`. För att distribuera det, placera det bara i din favorit servlet-container (t.ex. Apache Tomcat) och det är klart.

Mer avancerade instruktioner finns tillgängliga på sidan [Developer notes](https://github.com/ETERNA-earkiv/ETERNA/blob/main/DEV_NOTES.md).

## Hur man sätter upp utvecklingsmiljön

### Nödvändig programvara

Förutom programvaran som behövs för att bygga ETERNA rekommenderar vi följande:

* IntelliJ IDEA ([Nedladdningssida](https://www.jetbrains.com/idea/download/))

**OBS:** Detta är inte en restriktiv lista över programvara som ska användas för att utveckla ETERNA (eftersom annan programvara, som IDE:er, kan användas istället för den föreslagna.)

### Hur man importerar koden i IntelliJ IDEA

1. Starta IntelliJ IDEA
2. Välj "File > Open". Bläddra sedan till ETERNA-källkodskatalogen på ditt filsystem och välj "Open"
3. Installera plugin "Adapter for Eclipse Code Formatter" i "File > Settings > Plugins"
4. Ställ in "Eclipse Code Formatter"-konfigurationen att använda `code-style/eclipse-formatter.xml`-konfigurationen. Ställ också in importordningen till `java;javax;org;com;`.
5. Välj valfri Java-fil och gör följande åtgärder (dessa inställningar kommer att komma ihåg, det finns inget behov av att välja dessa alternativ varje gång). På menyn: `Code > Reformat File...`, ställ in Scope: Only VCS changed text; Optimize imports (kryssad); Code cleanup (kryssad); Rearrange code (ej kryssad).
6. Gå till "File > Settings...", "Editor > Code Style > Java", välj fliken "Imports", ställ in "Class count to use import with '*':" 9999, ställ in "Names count to use static import with '*': 9999


## Kodstruktur

ETERNA är strukturerad enligt följande:

### /

* **pom.xml** - rot Maven Project Object Model
* **code-style** - checkstyle & Eclipse code formatter-filer
* **roda-common/** - denna modul innehåller gemensamma komponenter som används av andra moduler/projekt
  * **roda-common-data** - denna modul innehåller alla ETERNA-relaterade modelobjekt som används i alla andra moduler/projekt
  * **roda-common-utils** - denna modul innehåller basverktyg som ska användas av andra moduler/projekt

### /roda-core/

  * **roda-core** - denna modul innehåller modell-, index- och lagrings tjänster, med särskild uppmärksamhet på följande paket:
    * **common** - detta paket innehåller roda-core relaterade verktyg
    * **storage** - detta paket innehåller både en lagringsabstraktion (inspirerad av OpenStack Swift) och några implementationer (ATM en filsystem & Fedora 4 baserad implementation)
    * **model** - detta paket innehåller all logik runt ETERNA-objekt (t.ex. CRUD-operationer, etc.), byggt på toppen av ETERNA lagringsabstraktion
    * **index** - detta paket innehåller all indexeringslogik för ETERNA modellobjekt, arbetar tillsammans med ETERNA modell genom Observable-mönster
    * **migration** - detta paket innehåller all migrationslogik (t.ex. varje gång en ändring i ett modellobjekt inträffar kan en migration behövas)
  * **roda-core-tests** - denna modul innehåller tester och testhjälpmedel för roda-core-modulen. Förutom det kan denna modul läggas till som beroende för andra projekt som har, till exempel, plugins och man vill testa dem enklare

### /roda-ui/

* **roda-wui**- denna modul innehåller webbanvändargränssnittet (WUI) webbapplikation och webbtjänsterna REST. I grund och botten komponenterna för att tillåta programmatisk interaktion med ETERNA.

### /roda-common/

* **roda-common-data** - denna modul innehåller alla ETERNA-relaterade modelobjekt som används i alla andra moduler/projekt
* **roda-common-utils** - denna modul innehåller basverktyg som ska användas av andra moduler/projekt


## Bidra

### Källkod

1. [Förgrena ETERNA GitHub-projektet](https://help.github.com/articles/fork-a-repo)
2. Ändra koden och pusha till det förgrenade projektet
3. [Skicka en pull request](https://help.github.com/articles/using-pull-requests)

För att öka chanserna att din kod accepteras och mergas in i ETERNA-källkod här är en checklista över saker att gå igenom innan du skickar in ett bidrag. Till exempel:

* Har enhetstester (som täcker minst 80% av koden)
* Har dokumentation (minst 80% av publikt API)

### Licens och Immaterialrätt

Alla bidrag till ETERNA är licensierade under LGPL v3, vilket inkluderar ett explicit tillstånd av patenträttigheter, vilket innebär att utvecklarna som skapade eller bidrog till koden avstår från sina patenträttigheter med avseende på efterföljande återanvändning av programvaran.

### Externa plugins

För att skapa nya plugins och använda dem till ETERNA är det nödvändigt att:

1. Skapa ett nytt plugin-projekt, se https://github.com/keeps/roda-plugin-template/
2. Bygg pluginen och distribuera den resulterande zip-filen (expanderad) på **config/plugins/PLUGIN_NAME/**

## REST API

ETERNA kan kontrolleras via ett REST API. Detta är bra för att utveckla externa tjänster eller integrera andra applikationer med arkivet.

### Utveckla tredjepartsintegrationer

Om du är intresserad av att utveckla en integration med ETERNA via REST API:t, vänligen kontakta oss på earkiv@whitered.se.
