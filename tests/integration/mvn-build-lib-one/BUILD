load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")

alias(
    name = "mvn-build-lib-one",
    actual = ":lib",
)

filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)

create_mvn_buildpack(
    name = "buildpack",
    pom_file = "pom.xml",
    visibility = ["//visibility:public"]
)

run_mvn_buildpack(
    name = "lib",
    log_level = "OFF",
    srcs = [":sources"],
    buildpack = ":buildpack",
    visibility = ["//visibility:public"]
)