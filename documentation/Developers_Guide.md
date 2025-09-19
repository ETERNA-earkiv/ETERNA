# Developers guide

This is a quick and dirty guide on how to start coding on ETERNA.

## Get the source code

You can easily get the source code by cloning the project into your machine (just need git installed):

```bash
$ git clone https://github.com/ETERNA-earkiv/ETERNA.git
```

If you plan to contribute to ETERNA, you will need to first fork the repository into your own GitHub account and then clone it into your machine. To learn how to do it, please check this [GitHub article](https://help.github.com/articles/fork-a-repo).


<!-- WARNING: changing this title will break links -->
## How to build and run

ETERNA uses [Apache Maven](http://maven.apache.org/) build system. Being a multi-module Maven project, in the root **pom.xml** is declared all the important information to all modules in ETERNA, such as:

* Modules to be included in the default build cycle
* Maven repositories to be used
* Dependency management (version numbers are declared here and inherited by the sub-modules)
* Plugin management (version numbers are declared here and inherited by the sub-modules)
* Profiles available (There are a lot of usable profiles. One that only includes the core projects (**core**), other that includes user interface projects (**wui**), other that build ETERNA wui docker image (**wui,roda-wui-docker**), and some other ones that, for example, can include external plugins projects that can be integrated in ETERNA (**all**)).

### Dependencies

The pre-requisites to build ETERNA are:

* Git client
* Apache Maven 3.8+
* GitHub account, [configure Maven to use your Github account](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token).
* Oracle Java 17


### Compilation

To compile, go to the ETERNA sources folder and execute the command:

```bash
$ mvn clean package
```

Use the following command to skip the Unit Tests (faster).

```bash
$ mvn clean package -Dmaven.test.skip=true
```

After a successful compile, ETERNA web application will be available at `roda-ui/roda-wui/target/roda-wui-VERSION.war`. To deploy it, just put it inside your favourite servlet container (e.g. Apache Tomcat) and that is it.

More advanced instruction available on the [Developer notes](https://github.com/ETERNA-earkiv/ETERNA/blob/main/DEV_NOTES.md) page.

## How to set up the development environment

### Required software

Besides the software needed to build ETERNA, we recommend the following:

* IntelliJ IDEA ([Download page](https://www.jetbrains.com/idea/download/))

**NOTE:** This is not a restrictive list of software to be used for developing ETERNA (as other software, like IDEs, can be used instead of the one suggested.)

### How to import the code in IntelliJ IDEA

1. Start IntelliJ IDEA
2. Select "File > Open". Then, browse to ETERNA source code directory on your filesystem and select "Open"
3. Install "Adapter for Eclipse Code Formatter" plugin in "File > Settings > Plugins"
4. Set the "Eclipse Code Formatter" configuration to use `code-style/eclipse-formatter.xml` configuration. Also, set the import order to be `java;javax;org;com;`.
5. Select any Java file and do the following actions (these settings will be remembered, there is no need to select these options every time). On the menu: `Code > Reformat File...`, set Scope: Only VCS changed text; Optimize imports (checked); Code cleanup (checked); Rearrange code (unchecked).
6. Go to "File > Settings...", "Editor > Code Style > Java", select tab "Imports", set "Class count to use import with '*':" 9999, set "Names count to use static import with '*': 9999


## Code structure

ETERNA is structured as follows:

### /

* **pom.xml** - root Maven Project Object Model
* **code-style** - checkstyle & Eclipse code formatter files
* **roda-common/** - this module contains common components used by other modules/projects
  * **roda-common-data** - this module contains all ETERNA related model objects used in all other modules/projects
  * **roda-common-utils** - this module contains base utilities to be used by other modules/projects

### /roda-core/

  * **roda-core** - this module contains model, index and storage services, with special attention to the following packages:
    * **common** - this package contains roda-core related utilities
    * **storage** - this package contains both a storage abstraction (inspired on OpenStack Swift) and some implementations (ATM a filesystem & Fedora 4 based implementation)
    * **model** - this package contains all logic around ETERNA objects (e.g. CRUD operations, etc.), built on top of ETERNA storage abstraction
    * **index** - this package contains all indexing logic for ETERNA model objects, working together with ETERNA model through Observable pattern
    * **migration** - this package contains all migration logic (e.g. every time a change in a model object occurs a migration might be needed)
  * **roda-core-tests** - this module contains tests and tests helpers for roda-core module. Besides that, this module can be added as dependency for other project that have, for example, plugins and ones wants to test them more easily

### /roda-ui/

* **roda-wui**- this module contains the Web User Interface (WUI) web application and the web-services REST. Basically the components to allow programmatic interaction with ETERNA.

### /roda-common/

* **roda-common-data** - this module contains all ETERNA related model objects used in all other modules/projects
* **roda-common-utils** - this module contains base utilities to be used by other modules/projects


## Contribute

### Source code

1. [Fork the ETERNA GitHub project](https://help.github.com/articles/fork-a-repo)
2. Change the code and push into the forked project
3. [Submit a pull request](https://help.github.com/articles/using-pull-requests)

To increase the changes of you code being accepted and merged into ETERNA source here's a checklist of things to go over before submitting a contribution. For example:

* Has unit tests (that covers at least 80% of the code)
* Has documentation (at least 80% of public API)

### License and Intellectual Property

All contributions to ETERNA are licensed on LGPL v3, which includes an explicit grant of patent rights, meaning that the developers who created or contributed to the code relinquish their patent rights with regard to any subsequent reuse of the software.

### External plugins

To create new plugins and use them to ETERNA it is necessary to:

1. Create a new plugin project, see https://github.com/keeps/roda-plugin-template/
2. Build the plugin and deploy the resulting zip (expanded) on **config/plugins/PLUGIN_NAME/**

## REST API

ETERNA can be controlled via a REST API. This is great to develop external services or integrate other applications  with the repository.

### Developing 3rd party integrations

If you are interested in developing an integration with ETERNA via the REST API, please contact the product team for further information by leaving a question or a comment on https://github.com/ETERNA-earkiv/ETERNA/issues.
