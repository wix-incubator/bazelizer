# bazelizer

[![mavenizer actions Status](https://github.com/wix-incubator/mavenizer/workflows/CI/badge.svg)](https://github.com/wix-incubator/mavenizer/actions)

## IN PROGRESS 

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


## Usage

```
RULES_MAVENOZER_TAG = ...
RULES_MAVENOZER_URL = ...
RULES_MAVENOZER_SHA = ...

http_archive(
    name = "bazelizer",
    strip_prefix = "mavenizer-%s" % RULES_MAVENOZER_TAG,
    sha256 = RULES_MAVENOZER_SHA,
    url = RULES_MAVENOZER_URL,
)

load("@bazelizer//:paths.bzl", "create_mvn_buildpack", "run_mvn_buildpack")

# usage ....
```


## Design

This tools assume that pom.xml (e.g. build configurations) will be updated **rarely**. In this case tool represent 2 ruls:

- create_mvn_buildpack
- run_mvn_buildpack


### create_mvn_buildpack
 
This rule generates tarball as a snapshot of m2 repository that resolve everything this project is dependent on (dependencies, plugins, reports) in preparation for going offline. 
Created tarball + initial pom = a `buildpack`. Image that can be reused for all consequent builds. 
                                                           

Usage
```
create_mvn_buildpack(
    name = "MyMavenImage",
    pom_file_init_src = "pom.xml"
)
```
  
| attr name  | description  |
|---|---|
| name  | Name; required. A unique name for this target.  |
| pom_file_init_src  | File; Reference to an skeleton pom.     |


**Important to know**: this rule execute dry run of a build by empty project directory + given pom.
This is done to eagerly fetch all plugin's and there dependencies and reuse for any build. In this case pom file have to be ready to be executed with empty workspace directory.
Any change in pom file will trigger rebuilding a tarball.



### run_mvn_buildpack

Represent subsequent maven build. Executed in offline mode, as expects that all needed for maven was fetched via `create_mvn_buildpack`. Can consume java based bazel deps.

Usage
```
run_mvn_buildpack(
    name = "MyMavenBuild",
    deps = [
        "//examples/targetA",
        "//examples/targetB"
    ], # test lib
    srcs = ["//examples/maven_project:sources"],
    buildpack = ":MyMavenImage",
    group_id = "my",
    artifact_id = "lib",
    outputs = ["lib-1.0.0-SNAPSHOT.jar"]
)
```


| attr name  | description  |
|---|---|
| name  | Name; required. A unique name for this target.  |
| deps  | Labels; Dependencies for maven build. Support only direct dependencies with JavaInfo provider.     |
| srcs  | filegroup that represent all workspace of maven project as is     |
| buildpack  | Reference to a buildpack that was created via  create_mvn_buildpack rule    |
| group_id  | (Optional) group id that passed into build. Available as placeholder (see below)   |
| artifact_id  | (Optional) artifact id that passed into build. Available as placeholder (see below)   |
| outputs  | List of files. Each file is relative to the maven target _folder_. Same files will be registered as an output of the target.   |


###### Important notes

1. This tool **not support** transitive dependencies. Only direct compile dependencies as [full_compile_jars](https://docs.bazel.build/versions/master/skylark/lib/JavaInfo.html#full_compile_jars) will be used.    

###### pom.xml 

Pom file supports  [mustache](https://mustache.github.io/) templating. Minimal example of pom with supported placeholders.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>{{ groupId }}</groupId>
    <artifactId>{{ artifactId }}</artifactId>
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

By default tool will not print default maven output to minimaze rules output. If it neede log level can be changed by 
```
run_mvn_buildpack(
    ...
    log_level="INFO"
)
```

Supported levels:

- **INFO** - print maven default output + info messages by a tool;
- **DEBUG** - print maven default output + debug messages by a tool (for example generate pom);
- **TRACE** - print maven debug output + debug messages by a tool;

###### Support of custom template properties:

TBD


## Features
