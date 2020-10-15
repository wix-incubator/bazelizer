load(
"//private/ruls/buildpack:buildpack.bzl",
_create_mvn_buildpack = "create_mvn_buildpack",
_run_mvn_buildpack = "run_mvn_buildpack"
)


create_mvn_buildpack = _create_mvn_buildpack
run_mvn_buildpack = _run_mvn_buildpack