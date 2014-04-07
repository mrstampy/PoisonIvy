# Poison Ivy - Java Library Dependency Resolver and Application Launcher

This library allows Java applications to be delivered without bundling Maven library dependencies with the deliverables. Instead the application is delivered with an [ivy.xml file](http://ant.apache.org/ivy/history/latest-milestone/ivyfile.html) which specifies the library dependencies.  On application startup, should libraries require resolution, the libraries are downloaded and the application can be restarted automatically.

The [PoisonIvy](https://github.com/mrstampy/PoisonIvy/blob/master/PoisonIvy/src/com/github/mrstampy/poisonivy/PoisonIvy.java) class can be used in one of three ways:

* A separate application
* An embedded class
* A main class superclass

## Parameters

- -h - Prints help message
- -ivy [ARG] - The ivy file for library dependency resolution (default: ./ivy.xml)
- -ivysettings [ARG] - The ivy settings file for library dependency resolution (default: built in settings)
- -libdir [ARG] - The directory to store the retrieved librarires (default: ./ivylib)
- -rp [ARG] - The ivy resolve pattern (default: [artifact]-[revision]&#040;-[classifier]&#041;.[ext])
- -f - Force clean library retrieval (default: false)
- -nc - Do not remove source and api documentation after library dependency retrieval (default: clean)
- -D [ARG] - Java -Dproperty=value command line properties
- -X [ARG] - Java -Xparm command line properties
- -mc [ARG] - The main class to execute
- -mj [ARG] - The application jar to execute

### Examples (command line)

-mj MyApplication.jar

-mj MyApplication.jar -X mx1000m -X ms500m

-mc com.my.MainClass -X mx1000m -X ms500m -D my.settings.file=/some/path/and/file

-ivy /path/to/ivy.xml -ivysettings /path/to/ivysettings.xml -libdir /path/to/ivylib -f -nc

### Examples (embedded class)

// forces update of libraries using the specified ivy.xml file
new PoisonIvy("-ivy", "myivy.xml", "-f").execute();

// forces update of libraries and (re)starts the Main application
new PoisonIvy("-f", "-mc", "com.my.app.Main").execute();

## Dependencies

Poison Ivy's dependencies (ivy, slf4j-api and commons-cli) must be included with the application deliverable.
