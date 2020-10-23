load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")


#
# preparing
#

_buildpack = "//tests/e2e/mvn-build-lib:app=._buildpack"

sh_library(
    name = "MavenImageDummy",
    srcs = ["dummy.sh"],
    data = [_buildpack],
)

sh_test(
    name = "MavenImageTest",
    srcs = ["test_files_exist.sh"],
    args = ["tests/e2e/mvn-build-lib/repository_for_app=._buildpack_img.tar", "tests/integration/mvn-build-lib/pom.xml"],
    data = [_buildpack],
    deps = [":MavenImageDummy"]
)

sh_test(
    name = "MavenImageTarTest",
    srcs = ["test_tar_repo.sh"],
    args = ["tests/e2e/mvn-build-lib/repository_for_app=._buildpack_img.tar"],
    data = [_buildpack],
    deps = [":MavenImageDummy"]
)

#
# running
#

_run = "//tests/e2e/mvn-build-lib:jaxb2api"

sh_library(
    name = "MavenRunDummy",
    srcs = ["dummy.sh"],
    data = [_run],
)

sh_test(
    name = "MavenRunTest",
    srcs = ["test_jar.sh"],
    args = ["tests/e2e/mvn-build-lib/myapi-1.0.0-SNAPSHOT.jar", "com/mavenizer/examples/jaxbapi/API.class"],
    data = [_run],
    deps = [":MavenRunDummy"]
)


sh_library(
    name = "mvn-build-lib-with-profile__maven",
    srcs = ["dummy.sh"],
    data = ["//tests/e2e/mvn-build-lib-with-profile"],
)

sh_test(
    name = "mvn-build-lib-with-profile__has_har_test",
    srcs = ["test_jar.sh"],
    args = ["tests/e2e/mvn-build-lib-with-profile/libmvn-build-lib-with-profile.jar"],
    data = [":mvn-build-lib-with-profile__maven"],
    deps = [":mvn-build-lib-with-profile__maven"]
)