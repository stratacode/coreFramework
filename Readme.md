## StrataCode coreFrameworks layer bundle

Contains layers that define the main framework integrations. This includes the layers used for running the Java to Javascript converter, the web framework, database integrations and more.

To run these layers, install the 'scc' command and put into your path. Put the coreFramework directory into a 'bundles' directory or specify a layer path that includes it. Choose from the subset of "runnable layers" in the examples, or in the getting started instructions.

### Note on scj format

Use of scj as an extension instead of java for Java files is only required for use of the StrataCode IntelliJ plugin. The scc command treats files with scj just like java files so if you are not using the IntelliJ plugin, you can use scc with regular Java projects. 

### Send us pull requests with enhancements or changes you require

For submitting pull requests that include .java files, please use the 'scj' extension so the IntelliJ plugin can be used for editing and running this module.
