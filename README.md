# Watchdog

<p align="center"><img src ="logo.png" alt="Watchdog logo" /></p>

Inspired by [watchdog timers](https://en.wikipedia.org/wiki/Watchdog_timer) in embedded systems, the Watchdog project is a Java toolkit for
helping guard your code against runaway loops and stalled I/O. Why use Watchdog? When used correctly, it adds a layer of resiliency to your
application that protects against software bugs and bad inputs.

## Table of Contents

 * [Quick-start Guide](#quick-start-guide)
   * [Maven Instructions](#maven-instructions)
   * [Ant Instructions](#ant-instructions)
   * [Gradle Instructions](#gradle-instructions)
   * [Java Agent Instructions](#java-agent-instructions)
   * [Code Example](#code-example)
 * [Usage Guide](#usage-guide)
   * [Watching Code](#watching-code)
   * [Watching I/O](#watching-io)
   * [Uninterruptible Sections](#uninterruptible-sections)
   * [Launching](#launching)
 * [Configuration Guide](#configuration-guide)
   * [Marker Type](#marker-type)
 * [FAQ](#faq)
   * [How much overhead am I adding?](#how-much-overhead-am-i-adding)
   * [Can I use this with an IDE?](#can-i-use-this-with-an-ide)
   * [What alternatives are available?](#what-alternatives-are-available)
 * [Change Log](#change-log)

## Quick-start Guide

The Watchdog project relies on bytecode instrumentation to monitor your code. Maven, Ant, and Gradle plugins are provided to instrument your
code. In addition to these plugins, a Java Agent is provided to instrument your code at runtime. Your code can target any version of Java
from 9 to Java 10.

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

First, define the Ant Task. It's available for download from
[Maven Central](https://repo1.maven.org/maven2/com/offbynull/watchdog/ant-plugin/1.0.0/ant-plugin-1.0.0-shaded.jar).

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

You'll also need to include the "user" module's JAR in your classpath as a part of your build. It's also available for download from
[Maven Central](https://repo1.maven.org/maven2/com/offbynull/watchdog/user/1.0.0/user-1.0.0.jar).

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

The Watchdog Java Agent allows you to instrument your code at runtime instead of build-time. That means that the bytecode instrumentation
required to make your watchdog work happens when your application runs instead of when your application gets compiled.

To use the Java Agent, download it from [Maven Central](https://repo1.maven.org/maven2/com/offbynull/watchdog/java-agent/1.0.0/java-agent-1.0.0-shaded.jar) and apply it when you run your Java program...

```shell
java -javaagent:java-agent-1.0.0-shaded.jar myapp.jar

# Add config items via Java Agent argument. Java Agent arguments are specified
# as a comma delimited list appended to the path of the -javaagent tag. For
# example...
#
# -javaagent:java-agent-1.0.0-shaded.jar=key1=val1,key2=val2
```

The Watchdog Java Agent won't instrument classes that have already been instrumented, so it should be safe to use it with classes that may
have already gone through instrumentation (as long as those classes have been instrumented by the same version of the instrumenter).

### Code Example

First, declare classes and methods to watch...

```java
@Watch // Apply annotation to class or individual methods
public class Main {
    public static void infiniteLoop() {
        while (true) { }
    }
}
```

Then, launch and watch...

```java
// Launch code.  If doesn't finish in 2.5 seconds, break out.
WatchdogLauncher.watch(2500L, (Watchdog wd) -> {
    Main.infiniteLoop();
});
```

After 2.5 seconds, you should get a ```WatchdogTimeoutException```...

```
Exception in thread "main" com.offbynull.watchdog.user.WatchdogTimeoutException:
	at com.offbynull.watchdog.user.WatchdogLauncher.watch(WatchdogLauncher.java:93)
	at com.offbynull.watchdog.user.WatchdogLauncher.watch(WatchdogLauncher.java:54)
	at test.Launcher.main(Launcher.java:8)
Caused by: com.offbynull.watchdog.user.CodeInterruptedException
	at com.offbynull.watchdog.user.Watchdog.hitCheck(Watchdog.java:141)
	at com.offbynull.watchdog.user.Watchdog.onBranch(Watchdog.java:113)
	at test.Main.infiniteLoop(Main.java:8)
	at test.Launcher.lambda$main$0(Launcher.java:8)
	at com.offbynull.watchdog.user.WatchdogLauncher.lambda$watch$0(WatchdogLauncher.java:51)
	at com.offbynull.watchdog.user.WatchdogLauncher.watch(WatchdogLauncher.java:85)
```

## Usage Guide

The Watchdog project is essentially a combination bytecode instrumenter and library that helps guard your code against a specific class of
software bugs: runaway threads. If you haven't already done so, refer to the [Quick-start Guide](#quick-start-guide) for setup instructions
for your environment.

It's important to note that Watchdog isn't a foolproof drop-in solution...

1. It only guards code you write, not code in third-party libraries that you may be using.
1. It's intended to be used in conjunction with good coding practices such as input validation.

Please read the following subsections carefully as they detail important concepts, usage patterns, and gotchas.

### Watching Code

Watchdog relies on bytecode instrumentation to break out of runaway code such as infinite loops. There are 2 ways to mark your methods for
instrumentation: annotations and method parameters.

The ```@Watch``` annotation is the simplest way to mark methods for instrumentation. Apply the annotation to a class to mark all methods
within the class for instrumentation. Or, apply the annotation to individual methods to only mark those methods for instrumentation.

```java
@Watch
public class AnnotatedClass {
    public static void infiniteLoop() {
        while (true) { }
    }

    public static void block(long wait) throws InterruptedException {
        Thread.sleep(wait);
    }
}

public class AnnotatedMethods {
    @Watch
    public static void infiniteLoop() {
        while (true) { }
    }

    @Watch
    public static void block(long wait) throws InterruptedException {
        Thread.sleep(wait);
    }
}
```

The other way to mark methods for instrumentation is to change the parameter list of the method to take in a ```Watchdog``` as the first
parameter. The argument passed in for that parameter can then be passed down the invocation chain.

```java
public class ParameterMethods {
    public static void infiniteLoop(Watchdog watchdog) {
        while (true) {
            block(watchdog, 100L);
        }
    }

    public static void block(Watchdog watchdog, long wait) throws InterruptedException {
        Thread.sleep(wait);
    }
}
```

Whenever possible, you should opt for marking using a ```Watchdog``` parameter over a ```@Watch``` annotation. While both have their
limitations, the instrumentation added for annotation marking is slower than parameter marking. For full coverage, mix annotation marking
with parameter marking in the same class. For example...

```java
@Watch
public class Mix {

    public void test() {
        for (int i = 0; i < 10; i++) {
            List<Integer> list = newInstance(Watchdog.PLACEHOLDER);
            System.out.println(list);
        }
    }

    public List<Integer> create(Watchdog watchdog) {
    	Random rand = new Random();
    	
    	LinkedList<Integer> list = new LinkedList<>();
        IntStream.generate(() -> rand.nextInt()).forEach(x -> list.add(x));
        
        Collections.sort(list, (x,y) -> Integer.compare(x, y));

        return list;
    }
}
```

In the example above, both the methods and the lambdas within will be instrumented. The ```create()``` method takes in a ```Watchdog```
parameter, while the ```test()``` method and lambdas uses the ```@Watch``` annotation supplied on the class. Notice how ```test()``` is
calling ```create()```, but since it doesn't have direct access to a ```Watchdog``` object to pass down the invocation chain, it uses
```Watchdog.PLACEHOLDER```. If an annotated method ever needs access to the ```Watchdog``` object, ```Watchdog.PLACEHOLDER``` can be used.

One important thing to be aware of with lambdas is that, at this time, directly passing in methods doesn't work. For example...

```java
IntStream.range(0,10).forEach(list::add);        // NO check applied for each call to List.add()
IntStream.range(0,10).forEach(x -> list.add(x)); // check applied for each call to List.add()
```

### Watching I/O

Instrumented methods can also take into account blocking I/O. The usage pattern for this is simple: use ```Watchdog.watchBlocking()``` to
watch a I/O resource and ```Watchdog.unwatchBlocking()``` to unwatch it. If the watchdog timer elapses, the resource gets closed based on
the logic you supply. For example...

```java
BlockingInterrupter fisInterrupter = null;
try (FileInputStream fis = new FileInputStream("in.txt")) {
    fisInterrupter = t -> fis.close();
    watchdog.watchBlocking(fisInterrupter);

    String fileData = IOUtils.toString(fis);
    System.out.println(fileData);
} finally {
    if (fisInterrupter != null) {
        watchdog.unwatchBlocking(fisInterrupter);
    }
}
```

Alternatively, since the example above is using try-with-resources and ```Closeable```s, it can be simplified by using
```Watchdog.wrapBlocking()```...

```java
try (FileInputStream fis = new FileInputStream("in.txt");
     Closeable cfis = watchdog.wrapBlocking(fis);) {
    String fileData = IOUtils.toString(fis);
    System.out.println(fileData);
}
```

### Uninterruptible Sections

Instrumented methods can have regions of code that run uninterrupted by the watchdog. These regions are called uninterruptible sections. If
the watchdog timer elapses while in an uninterruptible section, execution will continue until the uninterruptible section has been exited,
at which point the code will abruptly exit.

The usage pattern for this is simple: use ```Watchdog.enterUninterruptibleSection()``` to start an uninterruptible section and
```Watchdog.exitUninterruptibleSection()``` to leave it. For example...

```java
watchdog.enterUninterruptibleSection();
try {
    for (Resource res : resources) {
        res.shutdown();
    }
} finally {
    watchdog.exitUninterruptibleSection();
}
```

Alternatively, the example above can be simplified by using ```Watchdog.wrapUninterruptibleSection()```...

```java
watchdog.wrapUninterruptibleSection(() -> {
    for (Resource res : resources) {
        res.shutdown();
    }    
});
```

Outside of performing crucial tasks such as cleanup operations typically found in catch/finally blocks, uninterruptible sections should be
used sparingly.

### Launching

Instrumented code must be launched through the ```WatchdogLauncher``` class. For example...

```java
// Launch code.  If doesn't finish in 2.5 seconds throws a WatchdogTimeoutException.
Result res = WatchdogLauncher.watch(2500L, (Watchdog wd) -> {
    MainClass main = new MainClass(wd);
    Result mainRes = main.execute(wd);
    return mainRes;
});
```

If the watchdog timer elapses before your instrumented code finishes, you'll receive a ```WatchdogTimeoutException```. Internally, however,
your instrumented code will throw a ```CodeInterruptedException```. You should never catch/discard a ```CodeInterruptedException```
exception.

If you run instrumented code directly or attempt to launch code from code that's already been launched, you'll encounter an
```IllegalStateException```.

## Configuration Guide

You can configure instrumentation by supplying key/value arguments to the instrumenter. Arguments are passed in differently depending on how
you're performing instrumentation. If you're using...

 * Maven, provide configurations as XML tags inside the ```<configuration>``` tag ([Example](#maven-instructions))
 * Ant, provide configurations as XML attributes on the ```<InstrumentationTask>``` tag ([Example](#ant-instructions))
 * Gradle, provide configurations inside the ```watchdog``` block ([Example](#gradle-instructions))
 * Java Agent, provide configurations by appending ```=KEY1=VAL1,KEY2=VAL2,...``` to the ```-javaagent``` argument ([Example](#java-agent-instructions))

The following subsections provide information on the various configuration options.

### Marker Type

Marker type adds extra logic to track and output what the instrumenter added to your methods. This provides core information for debugging
problems with the instrumenter -- it provides little to no value for you as a user.

 * Name: ```markerType```.
 * Value: { ```NONE``` | ```CONST``` | ```STDOUT``` }.
 * Default: ```NONE```.

## FAQ

#### How much overhead am I adding?

Instrumentation adds code to your classes, so your class files will become larger and that extra code will take time to execute. For most
applications, especially enterprise applications that are I/O-heavy, the cost is negligible.

#### Can I use this with an IDE?

If your IDE delegates to one of the supported build systems (Maven/Gradle/Ant), you can use this with your IDE. In some cases, your IDE may
try to optimize by prematurely compiling classes internally, skipping any instrumentation that should be taking place as a part of your
build. You'll have to turn this feature off.

For example, if you're using Maven through Netbeans, you must turn off the "Compile On Save" feature that's enabled by default. Otherwise,
as soon as you make a change to your code and save, Netbeans will compile your Java file without instrumentation. IntelliJ and Eclipse
probably have similar options available.

#### What alternatives are available?

There don't seem to be any competing products available. If you know of any, please let me know and I'll update this section.

## Change Log

### [1.0.0] - Unreleased
- Initial release.
