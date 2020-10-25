# bazelizer

[![mavenizer actions Status](https://github.com/wix-incubator/mavenizer/workflows/CI/badge.svg)](https://github.com/wix-incubator/mavenizer/actions)

## !! WORK IN PROGRESS  !!

#### TL;DR
This is your last chance if you really need some maven plugin inside your Bazel build and you cannot rewrite or adapt it to Bazel.
 
#### Long Version
 
There is a wide variety of the maven plugins already written. Moreover, some plugins have no ports to other build systems. So, if 

- you use Bazel as main build tool;
- and there is a maven plugin that you need;

Then, you need *bazelizer*. 
Someone can call it a **dirty hack**, but we know the truths ;)

This tool represents overall maven project as one Bazel's target.
In this whey you can isolate all your maven specific code into one hermetic unit within bazel build graph .
Use bazel deps, depends on bazel targets and event doing it efficiently. 

***

![Alt text](assets/ci.png?raw=true "Title")


## Getting started

> There is no compatibility table of bazel versions right now.



Add the following to your WORKSPACE file and update the githash if needed:
```python
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


## Usage

1. In your maven module's declare BUILD file:
```python
# in your BUILD files

load("@wix_incubator_bazelizer//maven:defs.bzl", "declare_pom")

declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml", # your pom file in a folder 
    visibility = ["//visibility:public"]
)

``` 

2. Register declared pom in you WORKSPACE via specific repository rule
```python
# in your WORKSPACE files
# 'wix_incubator_bazelizer' declaration and imports must be above 

load("@wix_incubator_bazelizer//maven:defs.bzl", "maven_repository_registry")

maven_repository_registry(
    name = "maven_buildpack", 
    modules = [
        "//path/to/my/module:declared_pom",
        # ... all the rest modules 
    ]
)
```

3. Update your maven module's BUILD:
```python
load("@wix_incubator_bazelizer//maven:defs.bzl", "declare_pom")

load("@maven_buildpack//:execute_build.bzl", "execute_build")

declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml", # your pom file in a folder 
    visibility = ["//visibility:public"]
)

execute_build(
    name = "mvn-build-lib",
    pom_def = ":declared_pom",
    srcs = [":sources"],
    visibility = ["//visibility:public"],
    deps = [
         "//some/target/dep",
        # ...
    ],
)
```


## Design

This tools assume that pom.xml (e.g. build configurations) will be updated **rarely**. In this case tool works by 2 phases:

1. You declare module
2. You register modules into one maven registry
3. You use build runner from declared registry

> See example above and e2e tests examples [here](tests/e2e/README.md)

This done for centralizing maven local cache's for all modules, and fetching them only once. 
All build executions are **stateless** and **hermetic**. 
Performing in offline mode, so can use only fetched dependencies + dynamic dependencies from bazel  


## Rules

### maven_repository_registry
 
> This is [repository_rule](https://docs.bazel.build/versions/master/skylark/repository_rules.html), so only allowed in the WORKSPACE file.
                                                           
Register all maven modules. It's goal to resolves all project dependencies, including plugins and make centralized repository that 
contains everything maven specific to support offline mode for all registered builds. Can handle resolve maven modules hierarchy. 

Usage
```
load("@wix_incubator_bazelizer//maven:defs.bzl", "maven_repository_registry")

maven_repository_registry(
    name = "maven_repo",
    modules = [
        "//tests/e2e/mvn-parent-pom:declared_pom",
        "//tests/e2e/mvn-build-lib:declared_pom",
        "//tests/e2e/mvn-build-lib-one:declared_pom",
        "//tests/e2e/mvn-build-lib-with-profile:declared_pom",
    ]
)
```
  
| attr name  | description  |
|---|---|
| modules  | Label list; required; All declared maven modules that supposed to be used  |


### declare_pom

This is a maven module declaration. It's goals is to describe maven module. 

Usage
```python
load("@wix_incubator_bazelizer//maven:defs.bzl", "declare_pom")

declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml",
    parent = "//tests/e2e/mvn-parent-pom:declared_pom",
    mvn_flags = [ "-P my_profile1,my_profile2" ]
)
```


| attr name  | description  |
|---|---|
| pom_file  | File; required. The pom file.  |
| parent  | Parent `declare_pom` module. Support of [inheritance](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#example-2) scenario.     |
| mvn_flags  | Additional flags for maven build     |

#### inheritance

POM projects support inheritance. This can be achieved via two scenarios. 
1. declaring all submodules in the parent pom
2. declare parent pom in each sub-module as `<relativePath>` element to our parent section.

**NOTE:** `bazelizer` supports only #2 scenario of usage

###### example 

Pom file is just plait maven project
```
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>xxxx</groupId>
    <artifactId>yyyy</artifactId>
    <version>1.0.0</version>

    {{#parent}}
    <parent>
        <groupId>xxxx</groupId>
        <artifactId>yyyy-parent</artifactId>
        <version>1.0.0</version>
        <relativePath>{{parent}}</relativePath>
    </parent>
    {{/parent}}
</project>
```

Then declaration will be
Usage
```python
# in yyyy-parent BUILD file
load("@wix_incubator_bazelizer//maven:defs.bzl", "declare_pom")

declare_pom(
    name = "yyyy_parent_pom",
    pom_file = ":pom.xml",
)

# in yyyy BUILD file
load("@wix_incubator_bazelizer//maven:defs.bzl", "declare_pom")

declare_pom(
    name = "yyyy_pom",
    pom_file = ":pom.xml",
    parent = "//your/yyyy-parent:yyyy_parent_pom"
)

```
##### flags

Sometimes you need specific maven profiles to be activated. This can be done via additional configurations for each pom declaration.

```python

declare_pom(
    name = "yyyy_parent_pom",
    pom_file = ":pom.xml",
    mvn_flags = [ "-P" ,"my_profile_x,my_profile_y" ]
)

```

**mvn_flags** is a command line arguments subset of mvn binary. Supported arguments:

| attr  | description  |
|---    |  ---         |
| -P PROFILE_1[,PROFILE_N]     | `,` separated list of profiles to activate  |
| --goal GOAL [--goal GOAL_N]  | mvn executable goals; Default is `clean install` |

###### pom.xml 

Pom file supports  [mustache](https://mustache.github.io/) templating. Minimal example of pom with supported placeholders.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>XXX</groupId>
    <artifactId>YYY</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <dependencies>
        {{#deps}}
        <dependency>
            <groupId>{{groupId}}</groupId>
            <artifactId>{{artifactId}}</artifactId>
            <version>{{version}}</version>
        </dependency>
        {{/deps}}
    </dependencies>

</project>
```

Where:
- groupId - `run_mvn_buildpack` attr or autogenerated string
- artifactId - `run_mvn_buildpack` attr or autogenerated string
- deps - bazel dependencies that will be installed during `run_mvn_buildpack` build. Each entity will have (groupId, artifactId, version) properties.



##### Logging

TBD

## Features
