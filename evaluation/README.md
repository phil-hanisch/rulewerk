Evaluation
==========

This evaluation considers the grounding performance of the Rulewerk system extended by ASP features as well as the quality of the groundings produced by it.
The evaluation uses the instances and encodings for the problem classes _Crosswords_, _Stable-Marriage_, and _Visit-All_.
The first one is a custom problem class, while the others are from the [6th ASP competition](http://aspcomp2015.dibris.unige.it/}).
Moreover, this directory contains scripts for preparing, running, and evaluating the tests.

Compilation
-----------
There are already the two jar-files _rulewerk-asp.jar_ and _rulewerk-asp-fast.jar_, which can be used for the evaluation.
The latter system is obtained from the branch poc-native-answers, 
and it circumvents the existing Rulewerk interface for querying VLog to avoid retranslating query answers into java objects.
Both systems uses the file [ASP example](https://github.com/phil-hanisch/rulewerk/blob/poc/rulewerk-examples/src/main/java/org/semanticweb/rulewerk/examples/AspExample.java)
to provide a simple command-line interface.
You can recompile the files by running `mvn clean compile assembly:single`.

Run tests
---------
The script _run-test.zsh_ allows to automatically run the test for some or all of the problem classes mentioned above.
At the beginning of the script, there are parameters you can use to adapt the behaviour of the tests, e.g., by specifying how often each instances should be evaluated.
