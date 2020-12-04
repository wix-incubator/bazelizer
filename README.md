# bazelizer

[![mavenizer actions Status](https://github.com/wix-incubator/mavenizer/workflows/CI/badge.svg)](https://github.com/wix-incubator/mavenizer/actions)

## !! WORK IN PROGRESS  !!

#### TL;DR
This is your last chance if you really need some maven plugin inside your Bazel build and you cannot rewrite or adapt it to Bazel.
 
#### Long Version
 
There is a wide variety of maven plugins already written. Moreover, some plugins have no ports to other build systems. So, if 

- you use Bazel as main build tool, your CI use bazel and all 99% teams and all infra are using Bazel
- and there is a maven plugin that you need;

Then, you need *bazelizer*. 
Someone can call it a **dirty hack**, but we know the truths ;)

This tool represents overall maven project as one Bazel's target.
In this whey you can isolate all your maven stuff as one unit (_if desired, put everything that is not specific to the maven into Bazel_) and integrate it into  Bazel environment.
Use bazel deps, depends on bazel target and event doing it efficiently. 

***

##### Take a look on example project [here](tests/integration/README.md)


![Alt text](assets/ci.png?raw=true "Title")


# Usage

Registering the tool

```
# in your WORKSPACE

RULES_TAG = "0.2.3"
RULES_TAG_SHA = "e1ff5910ac034aed69e682e9e5cfb52450c27396a85edac8cdec79f50679ad6b"
NAME = "wix_incubator_bazelizer"

http_archive(
    name = NAME,
    url = "https://github.com/wix-incubator/bazelizer/archive/%s.zip" % RULES_TAG,
    type = "zip",
    strip_prefix = "bazelizer-%s" % RULES_TAG,
    sha256 = RULES_TAG_SHA,
)

load("@wix_incubator_bazelizer//third_party:rules_repository.bzl", "install")
install()
```

## declare_pom
Declare a maven module that you want to lift into bazel

```
# in BUILD file at the root of maven module

load("@wix_incubator_bazelizer//maven:defs.bzl", "declare_pom")

declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml",
    visibility = ["//visibility:public"]
)
```

Where:

| attr name  | description                                                        |
|    ---     | ---                                                                |
| name       | string; A unique name for this target.                             |
| pom_file   | File; Reference to an maven pom file.                              |
| parent     | Label; (optional) Reference to the _declare_pom_ target as maven parent pom. Support supported only via `<parent>...<relativePath>../path/to/parent</relativePath> </parent>` block.     |


### maven_repository_registry
 
Repository rule, that register all pom modules and doing centralized dependencies fetch for it. 
This repository generates an executor file that can be used for launching maven builds for particular modules. 

 ```
# in WORKSPACE file

load("@wix_incubator_bazelizer//maven:defs.bzl", "maven_repository_registry")

maven_repository_registry(
    name = "bazelized_maven",
    modules = [
        "//tests/e2e/mvn-parent-pom:declared_pom",
        "//tests/e2e/mvn-build-lib:declared_pom",
        "//tests/e2e/mvn-build-lib-one:declared_pom",
        "//tests/e2e/mvn-build-lib-with-profile:declared_pom",
    ]
)
```

Where:

| attr name  | description                                                                |
|    ---     | ---                                                                        |
| name       | string; A unique name for the **repository**                                     |
| modules    | List labels; Lavels of all maven modules that should be lifted into bazel  |



 
This rule generates tarball as a snapshot of m2 repository that resolve everything this project is dependent on (dependencies, plugins, reports) in preparation for going offline. 
Created tarball + initial pom = a `buildpack`. Image that can be reused for all consequent builds. 
                                                           


## execute_build

Rule created by `maven_repository_registry` repository rule. Responsible for launching particular maven modules.


```
load("@bazelized_maven//:execute_build.bzl", "execute_build")


# Example of fetching everething from maven workspace
filegroup(
    name = "sources",
    srcs = glob(["**/*"], exclude=["target/**/*"]),
    visibility = ["//visibility:public"]
)

execute_build(
    name = "my-maven-lib",
    pom_def = ":declared_pom", # see: declare_pom rule
    srcs = [":sources"],
    visibility = ["//visibility:public"],
    deps = [
         # my bazel deps
         "//tests/e2e/mvn-build-lib-one",
        "//tests/e2e/lib/src/com/mavenizer/examples/subliby",
    ]
)
```
  
Where:

| attr name  | description                                                                                                         |
|    ---     | ---                                                                                                                 |
| name       | string; A unique name for the target                                                                                |
| pom_def    | Label; declared maven module definition (see  `declare_pom` rule)                                                   |
| srcs       | List files; Files that will be lined into bazel sandbox for execution. Have to contains at least `src` dir.         |
| deps       | List labels; Bazel dependencies with JavaInfo provider.                                                             |
| outputs    | List string; (optional) Files inside target dedicatory that should be additionally registered as output for target. |

###### Notes:
1. Tool support only direct dependencies. All transitive dependencies are **excluded**. 
2. Default rule outputs is the jar file that current maven build produce. So, `outputs` can be usefull some additional result binaries  needed. 
3. Another maven module can be a dependency for current module. In this case it is the same as module was published into maven repository, and this is **cool**.


## pom.xml 

TBD

Pom file support special set of XML transformations to be able to use it both in bazel and in maven transparently. By default 
tool:
- tries to cleanup all dependencies in `<dependencies>` and append rule deps from bazel 
(installed into tmp local repository for each execution run)
- inject sandbox relative path to parent pom, if it provided

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    

    <!-- Here we enabling this transformations -->

    <!-- xembly:on -->

    <modelVersion>4.0.0</modelVersion>
    <groupId>yyy</groupId>
    <artifactId>xxxx</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>javax.xml.bind</groupId>
            <artifactId>jaxb-api</artifactId>
        </dependency>
    </dependencies>

</project>
```

Supported flags:

| flag      | description |
| ----      | ---- |
| xembly:on | Enabling support of transformations. By default all  |
| xembly:no-drop-deps | Disable cleanup of deps |



##### Logging

By default tool will not print default maven output to minimaze rules output. If it neede log level can be changed by 
```



execute_build(
    ...
    log_level="INFO"
)
```

Supported levels:

- **INFO** - print maven default output + info messages by a tool;
- **DEBUG** - print maven default output + debug messages by a tool (for example generate pom);
- **TRACE** - print maven debug output + debug messages by a tool;
