load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")


filegroup(
    name = "sources",
    srcs = glob(["**/*"]),
    visibility = ["//visibility:public"]
)


create_mvn_buildpack(
    name = "app=._buildpack",
    pom_file = "pom.xml",
    pom_parent = "//tests/integration/mvn-parent-pom:sources",
    visibility = ["//visibility:public"],
    log_level = "INFO"
)


run_mvn_buildpack(
    name = "jaxb2api",
    deps = [
        "//tests/integration/lib/src/com/mavenizer/examples/subliby"
    ],
    log_level = "INFO",
    srcs = [":sources"],
    buildpack = ":app=._buildpack",
    outputs = ["myapi-1.0.0-SNAPSHOT.jar"],
    visibility = ["//visibility:public"]
)