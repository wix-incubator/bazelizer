load("//private/ruls/maven:repository.bzl", "declare_pom")
load("//private/ruls/maven:repository.bzl", "maven_repository")
load("//private/ruls/maven:runner.bzl", "run_mvn")


#declare_pom(
#    name = "the_mvn-parent-pom",
#    pom_file = "//tests/e2e/mvn-parent-pom:pom.xml"
#)
#
#
#declare_pom(
#    name = "the_mvn-build-lib-one",
#    pom_file = "//tests/e2e/mvn-build-lib-one:pom.xml",
#    parent = ":the_mvn-parent-pom"
#)


#maven_repository(
#    name = "repo",
#    modules = [
#        "the_mvn-parent-pom",
#        "the_mvn-build-lib-one"
#    ]
#)
#
#run_mvn(
#    repository = ":repo",
#    srcs = ["//tests/e2e/mvn-build-lib-one:sources"],
#    pom_def = ":the_mvn-build-lib-one",
#    name = "the_mvn-build-lib-one__build"
#)