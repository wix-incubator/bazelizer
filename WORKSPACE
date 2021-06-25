repository_name = "wix_incubator_bazelizer"

workspace(name = repository_name)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")


RULES_JVM_EXTERNAL_TAG = "3.1"

RULES_JVM_EXTERNAL_SHA = "e246373de2353f3d34d35814947aa8b7d0dd1a58c2f7a6c41cfeaff3007c2d14"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("//:third_party.bzl", "dependencies")

dependencies()

load("@bazelizer_deps//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

load("@bazelizer_deps//:compat.bzl", "compat_repositories")

compat_repositories()

load("//:maven_binary_tool.bzl", install_maven_tool = "install")

install_maven_tool()

#
# E2E tests
#
load("//maven:defs.bzl", "maven_repository_registry")

maven_repository_registry(
    name = "maven_e2e_v3",
    modules = [

        "//tests/e2e/mvn-lib-subparent:maven",
        "//tests/e2e/mvn-lib-subparent/mvn-lib-module-a:maven",
        "//tests/e2e/mvn-lib-subparent/mvn-lib-module-b:maven",
        "//tests/e2e/mvn-lib:maven",
        "//tests/e2e/mvn-lib-parent:maven",
    ],
)
