#!/bin/sh
javadoc -d ./javadocs -sourcepath "./src" -classpath "./lib/asm-4.2.jar:./lib/asm-tree-4.2.jar" -link "http://asm.ow2.org/asm40/javadoc/user/" -link "http://docs.oracle.com/javase/7/docs/api/"org.frustra.filament
