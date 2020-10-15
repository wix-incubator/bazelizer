load("//maven:defs.bzl", "create_mvn_buildpack", "run_mvn_buildpack")


#
# preparing
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
    args = ["tests/integration/mvn-build-lib/app=._buildpack_img.tar", "tests/integration/mvn-build-lib/pom.xml"],
    data = [_buildpack],
    deps = [":MavenImageDummy"]
)

sh_test(
    name = "MavenImageTarTest",
    srcs = ["test_tar_repo.sh"],
    args = ["tests/integration/mvn-build-lib/app=._buildpack_img.tar"],
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

#genrule(
#    name = "xxx",
#    outs = ["t.data"],
#    cmd = "ls $(locations @maven_bin//:bin) > \"$@\"",
#    srcs = ["@maven_bin//:bin"],
#)

#genrule(
#    name = "xxxy",
#    outs = ["xxxy.data"],
#    cmd = "./external/maven_bin/bin/mvn > \"$@\"",
#    srcs = ["@maven_bin//:bin"],
#)