load("//:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")


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


run_mvn_buildpack(
    name = "jaxb2api",
    deps = [
        "//tests/integration/lib/src/com/mavenizer/examples/subliby"
    ],
    srcs = [":sources"],
    buildpack = ":app=._buildpack",
    outputs = ["myapi-1.0.0-SNAPSHOT.jar"],
    visibility = ["//visibility:public"]
)