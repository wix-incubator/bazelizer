load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")

exports_files(["pom.xml"])

filegroup(
    name = "sources",
    srcs = glob(["**/*"]),
    visibility = ["//visibility:public"]
)


create_mvn_buildpack(
    name = "app=._buildpack",
    pom_file = "pom.xml",
    pom_parent = "//tests/e2e/mvn-parent-pom:parent",
    visibility = ["//visibility:public"],
    # log_level = "INFO"
)

run_mvn_buildpack(
    name = "jaxb2api",
    deps = [
        "//tests/e2e/mvn-build-lib-one:lib",
        "//tests/e2e/lib/src/com/mavenizer/examples/subliby",
    ],
    log_level = "OFF",
    srcs = [":sources"],
    buildpack = ":app=._buildpack",
    outputs = ["myapi-1.0.0-SNAPSHOT.jar"],
    visibility = ["//visibility:public"]
)