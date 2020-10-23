load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")

load("//private/ruls/maven:repository.bzl", "declare_pom")

exports_files(["pom.xml"])

create_mvn_buildpack(
    name = "parent",
    pom_file = "pom.xml",
    visibility = ["//visibility:public"],
    # log_level = "INFO",
)


#
# API 2.0
#

declare_pom(
    name = "declared_pom",
    pom_file = ":pom.xml",
    visibility = ["//visibility:public"]
)
