#load(
#"//private/ruls/buildpack:buildpack.bzl",
#_create_mvn_buildpack = "create_mvn_buildpack",
#_run_mvn_buildpack = "run_mvn_buildpack"
#)

load(
"//private/ruls/buildpack:new_buildpack.bzl",
_create_mvn_buildpack = "new_create_mvn_buildpack",
_run_mvn_buildpack = "new_run_mvn_buildpack"
)


load(
"//private/ruls/maven:repository.bzl",
_declare_pom = "declare_pom"
)

create_mvn_buildpack = _create_mvn_buildpack
run_mvn_buildpack = _run_mvn_buildpack
declare_pom = _declare_pom