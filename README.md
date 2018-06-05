# Watchdog

<p align="center"><img src ="logo.png" alt="Watchdog logo" /></p>

Inspired by [watchdog timers](https://en.wikipedia.org/wiki/Watchdog_timer) in embedded systems, the Watchdog project is a Java toolkit that allows you to monitor your worker threads and potentially take corrective action when problems arise. When used correctly, the Watchdog project adds a layer of resiliency to your enterprise application that protects against runaway threads caused by software bugs and bad inputs.

* Watch for and break out of blocked code (e.g. tight loops)
* Watch for and break out of blocked I/O
* Monitor code execution: object instantiations, method entry, and branching

## Table of Contents

 * [Quick-start Guide](#quick-start-guide)
   * [Maven Instructions](#maven-instructions)
   * [Ant Instructions](#ant-instructions)
   * [Gradle Instructions](#gradle-instructions)
   * [Java Agent Instructions](#java-agent-instructions)
   * [Code Example](#code-example)
 * [Usage Guide](#configuration-guide)
   * [Instrumentation](#instrumentation)
   * [Monitoring](#monitoring)
   * [Watching](#watching)
   * [Common Pitfalls and Best Practices](#common-pitfalls-and-best-practices)
 * [Configuration Guide](#configuration-guide)
   * [Marker Type](#marker-type)
 * [FAQ](#faq)
   * [How much overhead am I adding?](#how-much-overhead-am-i-adding)
   * [What restrictions are there?](#what-restrictions-are-there)
   * [Can I use this with an IDE?](#can-i-use-this-with-an-ide)
   * [What alternatives are available?](#what-alternatives-are-available)
 * [Change Log](#change-log)

## Quick-start Guide

The Watchdog project relies on bytecode instrumentation to monitor your code. Maven, Ant, and Gradle plugins are provided to instrument your code. In addition to these plugins, a Java Agent is provided to instrument your code at runtime. Your code can target any version of Java from 9 onward.

### Maven Instructions

In your POM...

First, add the "user" module as a dependency.
```xml
<dependency>
    <groupId>com.offbynull.watchdog</groupId>
    <artifactId>user</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then, add the Maven plugin so that your classes get instrumented when you build.
```xml
<plugin>
    <groupId>com.offbynull.watchdog</groupId>
    <artifactId>maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <!-- Instruments main classes at process-classes phase -->        
        <execution>
            <id>watchdog-instrument-id</id>
            <goals>
                <goal>instrument</goal>
            </goals>
        </execution>
        <!-- Instruments test classes at process-test-classes phase -->
        <execution>
            <id>test-watchdog-instrument-id</id>
            <goals>
                <goal>test-instrument</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- Add config items as XML tags here. -->
    </configuration>
</plugin>
```

### Ant Instructions

In your build script...

First, define the Ant Task. It's available for download from [Maven Central](https://repo1.maven.org/maven2/com/offbynull/watchdog/ant-plugin/1.0.0/ant-plugin-1.0.0-shaded.jar).
```xml
<taskdef name="InstrumentTask" classname="com.offbynull.watchdog.antplugin.InstrumentTask">
    <classpath>
        <pathelement location="ant-task-1.0.0-shaded.jar"/>
    </classpath>
</taskdef>
```

Then, bind it to the target of your choice.
```xml
<target name="-post-compile">
    <!-- The classpath attribute is a semicolon delimited list of the classpath required by your code. -->
    <!-- Add config items as XML attributes in the tag below. -->
    <InstrumentTask classpath="" sourceDirectory="build" targetDirectory="build"/>
</target>
```

You'll also need to include the "user" module's JAR in your classpath as a part of your build. It's also available for download from [Maven Central](https://repo1.maven.org/maven2/com/offbynull/watchdog/user/1.0.0/user-1.0.0.jar).

### Gradle Instructions

In your build script...

First, instruct Gradle to pull the watchdog plugin from Maven central...

```groovy
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath group: 'com.offbynull.watchdog',  name: 'gradle-plugin',  version: '1.0.0'
    }
}
```

Then, apply the watchdog plugin and add the "user" module as a dependency...

```groovy
apply plugin: "java"
apply plugin: "watchdog"

watchdog {
    // Add config items as properties here (e.g. key = value).
}

repositories {
    mavenCentral()
}

dependencies {
    compile group: 'com.offbynull.watchdog', name: 'user', version: '1.0.0'
}
```

### Java Agent Instructions

The Watchdog Java Agent allows you to instrument your code at runtime instead of build-time. That means that the bytecode instrumentation required to make your watchdog work happens when your application runs instead of when your application gets compiled.

To use the Java Agent, download it from [Maven Central](https://repo1.maven.org/maven2/com/offbynull/watchdog/java-agent/1.0.0/java-agent-1.0.0-shaded.jar) and apply it when you run your Java program...

```shell
java -javaagent:java-agent-1.0.0-shaded.jar myapp.jar

# Add config items via Java Agent argument. Java Agent arguments are specified
# as a comma delimited list appended to the path of the -javaagent tag. For
# example...
#
# -javaagent:java-agent-1.0.0-shaded.jar=key1=val1,key2=val2
```

The Watchdog Java Agent won't instrument classes that have already been instrumented, so it should be safe to use it with classes that may have already gone through instrumentation (as long as those classes have been instrumented by the same version of the instrumenter).

### Code Example

First, declare classes and methods to watch...

```java
@Watch // can be applied to class or individual methods
public class ClassA {
    public void infiniteLoop() {
        for (int i = 0; i < 1; i+=0) {
        }
    }
}
```

Then, launch it and watch...

```java
// Launch code.  If doesn't finish in 2.5 seconds, break out.
WatchdogLauncher.watch(2500L, (Watchdog wd) -> {
    new ClassA().infiniteLoop();
});
```

After 2.5 seconds, you should get a ```CodeInterruptedException```...

```
Exception in thread "main" com.offbynull.watchdog.user.CodeInterruptedException
	at com.offbynull.watchdog.user.KillDurationListener.hitCheck(KillDurationListener.java:77)
	at com.offbynull.watchdog.user.KillDurationListener.onBranch(KillDurationListener.java:67)
	at com.offbynull.watchdog.user.Watchdog.onBranch(Watchdog.java:63)
	at test.ClassA.infiniteLoop(ClassA.java:8)
	at test.Main.lambda$main$0(Main.java:10)
	at com.offbynull.watchdog.user.WatchdogLauncher.lambda$watch$0(WatchdogLauncher.java:78)
	at com.offbynull.watchdog.user.WatchdogLauncher.monitor(WatchdogLauncher.java:53)
	at com.offbynull.watchdog.user.WatchdogLauncher.watch(WatchdogLauncher.java:126)
	at com.offbynull.watchdog.user.WatchdogLauncher.watch(WatchdogLauncher.java:106)
	at com.offbynull.watchdog.user.WatchdogLauncher.watch(WatchdogLauncher.java:81)
	at test.Main.main(Main.java:9)
```

## Usage Guide

### Instrumentation

### Monitoring

### Watching

### Common Pitfalls and Best Practices

## Configuration Guide

You can configure instrumentation by supplying key/value arguments to the instrumenter. Arguments are passed in differently depending on how you're performing instrumentation. If you're using...

 * Maven, provide configurations as XML tags inside the ```<configuration>``` tag ([Example](#maven-instructions))
 * Ant, provide configurations as XML attributes on the ```<InstrumentationTask>``` tag ([Example](#ant-instructions))
 * Gradle, provide configurations inside the ```watchdog``` block ([Example](#gradle-instructions))
 * Java Agent, provide configurations by appending ```=KEY1=VAL1,KEY2=VAL2,...``` to the ```-javaagent``` argument ([Example](#java-agent-instructions))

The following subsections provide information on the various configuration options.

### Marker Type

Marker type adds extra logic to track and output what the instrumenter added to your methods. This provides core information for debugging problems with the instrumenter -- it provides little to no value for you as a user.

 * Name: ```markerType```.
 * Value: { ```NONE``` | ```CONST``` | ```STDOUT``` }.
 * Default: ```NONE```.

## FAQ

#### How much overhead am I adding?

Instrumentation adds code to your classes, so your class files will become larger and that extra code will take time to execute. For most applications, especially enterprise applications that are I/O-heavy, the cost is negligible.

#### What restrictions are there?

The main restrictions have to do with JNI and reflections.

* Instrumentation does not apply to native methods (JNI).
* Objects instantiated via reflections won't be reported.

#### Can I use this with an IDE?

If your IDE delegates to one of the supported build systems (Maven/Gradle/Ant), you can use this with your IDE. In some cases, your IDE may try to optimize by prematurely compiling classes internally, skipping any instrumentation that should be taking place as a part of your build. You'll have to turn this feature off.

For example, if you're using Maven through Netbeans, you must turn off the "Compile On Save" feature that's enabled by default. Otherwise, as soon as you make a change to your code and save, Netbeans will compile your Java file without instrumentation. IntelliJ and Eclipse probably have similar options available.

#### What alternatives are available?

There don't seem to be any competing products available. If you know of any, please let me know and I'll update this section.

## Change Log

### [1.0.0] - Unreleased
- Initial release.
