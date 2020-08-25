# mavenizer

#### TL;DR
This is your last chace if you really need a maven plugin and you cannot rewrite or adapt it to Bazel.
 
#### Long Version
 
There is a wide variety of plugins already written. Moreover, some plugins are supported by third-party developers. 
At the junction of these conditions, pain occurs:

- you use Bazel as main build tool, your CI use bazel and all 99% teams and all infra is using Bazel
- there is the maven plugin that can solve satisfy you;

And here *mavenizer* takes the stage. This tool represents overall maven project as one Bazel's target.
This allows you to extract all maven related stuff into some isolated unit and integrated it within all Bazel environment.
Use bazel deps, depends on bazel target and event doing it efficiently.

![Alt text](assets/ci.png?raw=true "Title")


## Usage

TBD


#### create_mvn_buildpack
  
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


**Important to know**: this rule execute dry run of a build by empty project directory + given pom. This is done to eagerly fetch all plugin's and there dependencies. 


#### run_mvn_buildpack

  
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
| group_id  | Optional group id that passed into build   |
| artifact_id  | Optional artifact id that passed into build   |
| outputs  | List of files. Each file is relative to the maven target _folder_. Same files will be registered as an output of the target.   |

## HowTo

1. This tool not support transitive dependecies. 
So, any Bazel's dependency that was passed into `run_mvn_buildpack` will be flattened under common group id and uniq artifact id. Each artifact id is sha256 hash string from bazel fully qualified target's name.

2. Tool assume that pom file will be modified rarely. So, `create_mvn_buildpack` generate a tarball of a maven repository with all dependencies required for a build. This tarball will cached by bazel. In this case all
maven inner dependencies + plagin's dependencies will be reused between projects. Main reason is to resolve everything this project is dependent on (dependencies, plugins, reports) in preparation for going offline

3. `run_mvn_buildpack` in its turn expect to be executed often, and trigger full maven build (**in offline mode**) with:
- skeleton pom from `create_mvn_buildpack`
- maven repository tarball from `create_mvn_buildpack`
- passed sources + passed deps