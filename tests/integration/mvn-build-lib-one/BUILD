load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")


filegroup(
    name = "sources",
    srcs = glob(["**/*"]),
    visibility = ["//visibility:public"]
)

create_mvn_buildpack(
    name = "app=._buildpack",
    pom_file = "pom.xml",
    visibility = ["//visibility:public"]
)

run_mvn_buildpack(
    name = "lib",
    log_level = "INFO",
    srcs = [":sources"],
    buildpack = ":app=._buildpack",
    outputs = ["myapi-single-1.0.0-SNAPSHOT.jar"],
    visibility = ["//visibility:public"]
)