# Plom: Predictive Language for Outreach on Mobiles

As smartphones become cheaper, people in poorer households are often able to afford a smartphone but not a computer. Although owning a smartphone does provide good access to technology, that access is also limited. To take full advantage of the computer capabilities of a smartphone, you need to be able to program it, you need to be able to write new software. If you aren't able to program your own software, you will always be reliant on others to make the software you need. But it's very difficult to program on a smartphone. The small screen and limited input capabilities of smartphones make it impractical to do any programming with them.

Plom is a project to create a programming language specifically for mobile phones. Plom programs are written using a special IDE that makes it easier to type in program code on a cellphone. Plom uses these approaches to make programming easier on a cellphone:

- it predicts the variables and keywords that are being entered to reduce the amount of input needed
- it automatically formats code so as to reduce the amount of formatting and special symbols that need to be typed
- it represents programs at a higher level than just text in order to make it easier to manipulate program code


## Interface

Although Plom is still being developed, here is an overview of what the interface for entering Plom code will look like.

![Mockup of Plom user interface](docs/imgs/uiOverview.svg)


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

