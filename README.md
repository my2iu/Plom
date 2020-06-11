# Plom
Predictive Language for Outreach on Mobiles


## Build Instructions

Plom is currently programmed in Java and compiled to JavaScript using GWT. The Maven build tool is used to compile the Java. To build Plom, simply go into the `plom-core` directory and run the Maven command

```
mvn package
```

Maven will then build the project. The generated JavaScript will be found in the `target/plom-core-...` directory. If you want Maven to start web server so that you can immediately run the code in a browser, you can give the command

```
mvn gwt:devmode
```

A window will open with instructions on where to point your browser to run the code.

If you change the grammar of the language, you will have to rebuild the generated parser code. Do that by going into the `plom-astgen` directory and running

``` 
mvn compile exec:java -Dexec.mainClass=org.programmingbasics.plom.astgen.PlomAstGen
```

