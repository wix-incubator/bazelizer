load("//private/ruls:buildpack.bzl", "create_mvn_buildpack", "run_mvn_buildpack")



filegroup(
    name = "pom",
    srcs = ["example_pom.xml"],
    visibility = ["//visibility:public"]
)


#
# preparing
#

create_mvn_buildpack(
    name = "MavenImage",
    pom_file_init_src = "example_pom.xml"
)

sh_library(
    name = "MavenImageDummy",
    srcs = ["dummy.sh"],
    data = [":MavenImage"],
)

sh_test(
    name = "MavenImageTest",
    srcs = ["test_files_exist.sh"],
    args = ["tests/MavenImage.tar", "tests/example_pom.xml"],
    data = [":MavenImage"],
    deps = [":MavenImageDummy"]
)

sh_test(
    name = "MavenImageTarTest",
    srcs = ["test_tar_repo.sh"],
    args = ["tests/MavenImage.tar"],
    data = [":MavenImage"],
    deps = [":MavenImageDummy"]
)


#
# running
#



run_mvn_buildpack(
    name = "MavenRun",
    deps = [
        "@maven//:info_picocli_picocli",
        "//tests/projectB"
    ], # test lib
    srcs = ["//tests/projectA:sources"],
    buildpack = ":MavenImage",
    outputs = ["projectA.jar"]
)


sh_library(
    name = "MavenRunDummy",
    srcs = ["dummy.sh"],
    data = [":MavenRun"],
)

sh_test(
    name = "MavenRunTest",
    srcs = ["test_files_exist.sh"],
    args = ["tests/MavenRun.tar"],
    data = [":MavenRun"],
    deps = [":MavenRunDummy"]
)