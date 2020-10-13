load("//:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")


#
# preparing
#

#create_mvn_buildpack(
#    name = "MavenImage",
#    pom_file_init_src = "example_pom.xml"
#)
#

_buildpack = "//tests/integration/mvn-build-lib:app=._buildpack"

sh_library(
    name = "MavenImageDummy",
    srcs = ["dummy.sh"],
    data = [_buildpack],
)

sh_test(
    name = "MavenImageTest",
    srcs = ["test_files_exist.sh"],
    args = ["tests/integration/mvn-build-lib/app=._buildpack.tar", "tests/integration/mvn-build-lib/pom.xml"],
    data = [_buildpack],
    deps = [":MavenImageDummy"]
)

sh_test(
    name = "MavenImageTarTest",
    srcs = ["test_tar_repo.sh"],
    args = ["tests/integration/mvn-build-lib/app=._buildpack.tar"],
    data = [_buildpack],
    deps = [":MavenImageDummy"]
)

#
# running
#

_run = "//tests/integration/mvn-build-lib:jaxb2api"

sh_library(
    name = "MavenRunDummy",
    srcs = ["dummy.sh"],
    data = [_run],
)

sh_test(
    name = "MavenRunTest",
    srcs = ["test_jar.sh"],
    args = ["tests/integration/mvn-build-lib/myapi-1.0.0-SNAPSHOT.jar", "com/mavenizer/examples/jaxbapi/API.class"],
    data = [_run],
    deps = [":MavenRunDummy"]
)