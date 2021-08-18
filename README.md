# bazelizer
[![mavenizer actions Status](https://github.com/wix-incubator/mavenizer/workflows/CI/badge.svg)](https://github.com/wix-incubator/mavenizer/actions)

<img src="assets/black.png" width="250">

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


![Alt text](assets/ci.png?raw=true | width=350)


## Usage

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

# in your BUILD files

load("@wix_incubator_bazelizer//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")


```


## Design

This tools assume that pom.xml (e.g. build configurations) will be updated **rarely**. 
So, tool will fetche all dependencies, not related to bazel targets, once and cached by bazel mechanics as 
maven repository tar archive. All builds rely on this repository 'image' with ability to inject bazel deps dynamical during build.

## Rules

### maven_repository_registry
 
This is repository rule that generate and register all declared "maven" targets. This allow to fetch all deps from maven world only once and cache them.
                                                           

Usage
```
# ./WORKSPACE

maven_repository_registry_v2(
    name = "maven_repo",
    modules = [
        "//tests/e2e/mvn-lib-a:module",
        "//tests/e2e/mvn-lib-parent:module",
        "//tests/e2e/mvn-build-lib-one:module",
        "//tests/e2e/mvn-lib-b:module",
        "//tests/e2e/mvn-lib-G:module",
        "//tests/e2e/mvn-lib-G/mvn-lib-G-a:module",
        "//tests/e2e/mvn-lib-G/mvn-lib-G-b:module",
    ]
)
```
  
| attr name  | description  |
|---|---|
| name  | Name; required. A unique name for this target.  |
| modules  | list of labels; Labels of declared maven targets via `declare_module`    |


**NOTE 1** This rule execute dry run of a build by empty project directory + given pom. 
This is done to eagerly fetch all plugin's and there dependencies and reuse for any build. In this case pom file have to be ready to be executed with empty workspace directory.
Any change in pom file will trigger rebuilding a tarball. 
Also rule fetchs all deps via 

**NOTE 2** Rule fetchs all also by `de.qaware.maven:go-offline-maven-plugin:resolve-dependencies` plugin. 
This allows overcome a problem that some plugin can dynamically fetch additional deps only at runtime. Pls read more about it [here](https://github.com/qaware/go-offline-maven-plugin).

### maven_project

Represent a maven target that represented as pom.xml file and optional reference to parent target. 

Usage

```
maven_project(
    name = "module",
    pom_file = ":pom.xml",
    parent = "//tests/e2e/mvn-lib-parent:module"
)
```

| attr name  | description  |
|---|---|
| name  | Name; required. A unique name for this target.  |
| pom_file  | Actual pom file.     |
| parent  | Label; Reference to parent module;    |

Special flags are supported:

| attr name  | description  |
|---                             | ---                                    |
| --deps-drop-all                | Delete all dependencies in pom.xml before execution. This can be useful if same dependencies will come from a bazel target;  |
| --deps-drop-exclude  <groupId:artifactId> | Exlude some dependency to be deleted by wildcard like `com.google.*:*` expressions;    |
| --mvn-active-profiles  profile1,profile2  | List of active profiles for maven;  |

### 

Usage

```
load("@maven_repo//:execute_build.bzl", "execute_build")

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"])
)

execute_build(
    name = "mvn-lib-G-b",
    pom_def = ":module",
    deps = [ "@com_sun_xml_bind_jaxb_impl" ],
    srcs = [":sources"],
    visibility = ["//visibility:public"]
)
```

| attr name  | description  |
|---|---|
| name  | Name; required. A unique name for this target.  |
| pom_def  | Label for module declaration;     |
| deps  | Deps; Supported any dep with JavaInfo provider; Support **only direct deps**;    |
| srcs  | Files; Link maven workspace into bazel sandbox;    |


###### Important notes

This tool **not support** transitive dependencies. Only direct compile dependencies as [full_compile_jars](https://docs.bazel.build/versions/master/skylark/lib/JavaInfo.html#full_compile_jars) will be used.    

###### pom.xml 

Pom transformed during each build. This is done to support hermetic builds and allows to inject java targets from bazel into it.
List of transformations:
1. Drop all dependencies declared in pom, except they are **marked** no to be deleted;
2. Injected synthetic dependency declaration for each bazel dep; 

For this case `com.sun.xml.bind` will be dropped. This is done to give possibility centralize deps and have onlu one source of truth about dep versions.
In this example we will inject  `@com_sun_xml_bind_jaxb_impl` instead. 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:bz="https://github.com/wix-incubator/bazelizer">
    <modelVersion>4.0.0</modelVersion>
    <groupId>groupId</groupId>
    <artifactId>artifactId</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <dependencies>
        <dependency bz:drop="never">
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </dependency>
        <dependency>
            <groupId>com.sun.xml.bind</groupId>
            <artifactId>jaxb-impl</artifactId>
        </dependency>
    </dependencies>

</project>
```


## Flags

```
 --deps-drop-all                          Delete all dependencies that declared in pom file
                                          before tool execution;
 --deps-drop-exclude=<coors>              Dependencies that satisfy an expression won't be
                                          deleted. Expected a pattern in format '<groupId>:<artifactId>'. 
                                          Also accept wildcard expressions. Examples: 
                                             'com.google.*:*', '*:guava', 'com.google.guava:failureaccess';
 --mvn-active-profiles=<p>                Maven active profiles
 --mvn-extra-args=<p>                     Maven extra commands
 --mvn-override-artifact-id=<artifactId>  Change artifact id for maven project
```