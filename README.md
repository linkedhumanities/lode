#### Installation
* load and install "Play Framework" (http://www.playframework.com).
* clone the project and verify that the project is adjusted to your play framework version (./lode/project/build.properties).
* compile (play compile) and start (play run) the project.

TODO: Add documentation for Tomcat, OpenRDF Sesame Workbench, etc.

# Databases
There are 3 databases involved in LODE: the H2 Settings Database, the MySQL wikiStat database, and the OpenRDF triples database. Note that in all URLs below `localhost` will need to be replaced with the server name (e.g., `linkedhumanities.cogs.indiana.edu`)

## H2 Database
This is initialized in the LODE framework.

The H2 Database stores the configuration settings from the `Config` tab of the web interface or the `app/Settings/Settings.java` file in the code.
 
A user-friendly browser can be intiialized with `play` then running `h2-browser` at the command line. [http://localhost:8082/](http://localhost:8082/). For remote access add a file `~/.h2.server.properties` with the contents `webAllowOthers=true`.

## MySQL Database
The MySQL Database stores the output of the wikiPrep algorithm. It can be accessed using PHPMyAdmin: [http://localhost/phpmyadmin/](http://localhost/phpmyadmin/)

## OpenRDF Database
The OpenRDF database stores the triples and the linkages created by LODE. It can be accessed at [http://localhost:8080/openrdf-workbench/](http://localhost:8080/openrdf-workbench/).
