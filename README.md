toolkit-analyzer
=================

Code is written in a combination of:
 - Java
 - Python

 ** Go to the Java package directory **

    cd analyzer

 ** Package me using Maven **

    mvn package

** Run me using Java **

    java -Xmx2g -jar ./target/toolkit-analyzer.jar BadDate Map ~/data latest

** Use my output **

All data output should be in the 'data' passed to the script. In the case above that is ~/data