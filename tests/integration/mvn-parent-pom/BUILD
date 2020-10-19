load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")

exports_files(["pom.xml"])

create_mvn_buildpack(
    name = "parent",
    pom_file = "pom.xml",
    visibility = ["//visibility:public"],
    log_level = "INFO",
)