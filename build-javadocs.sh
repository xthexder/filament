#!/bin/sh
javadoc -d ./javadocs-tmp -sourcepath "./src" -classpath "./lib/asm-4.2.jar:./lib/asm-tree-4.2.jar" -link "http://asm.ow2.org/asm40/javadoc/user/" -link "http://docs.oracle.com/javase/7/docs/api/" -public -quiet org.frustra.filament
git fetch origin gh-pages:gh-pages
git checkout gh-pages
rm -rf javadocs
mv javadocs-tmp javadocs
git commit -a -m "Update javadocs" && git push
git checkout master
