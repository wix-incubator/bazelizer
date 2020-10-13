load("//:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")
load("//private/ruls:buildpack.bzl", "create_mvn_buildpack_v2")


filegroup(
    name = "sources",
    srcs = glob(["**/*"]),
    visibility = ["//visibility:public"]
)


create_mvn_buildpack(
    name = "app=._buildpack",
    pom_file_init_src = "pom.xml",
    visibility = ["//visibility:public"]
)

create_mvn_buildpack_v2(
    name = "app=._buildpack2",
    pom_file = "pom.xml",
    visibility = ["//visibility:public"]
)

run_mvn_buildpack(
    name = "jaxb2api",
    log_level = "INFO",
    srcs = [":sources"],
    buildpack = ":app=._buildpack2",
    outputs = ["myapi-single-1.0.0-SNAPSHOT.jar"],
    visibility = ["//visibility:public"]
)