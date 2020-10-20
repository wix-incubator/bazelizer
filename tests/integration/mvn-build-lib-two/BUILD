load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")


filegroup(
    name = "sources",
    srcs = glob(["src/**/*"]),
    visibility = ["//visibility:public"]
)

create_mvn_buildpack(
    name = "buildpack",
    pom_file = "pom.xml",
    visibility = ["//visibility:public"],
    log_level = 'INFO',
    mvn_flags = [ "--active-profiles=my_profile"]
)

run_mvn_buildpack(
    name = "mvn-build-lib-two",
    log_level = 'INFO',
    srcs = [":sources"],
    buildpack = ":buildpack",
    visibility = ["//visibility:public"],
)