repository_name = "wix_incubator_bazelizer"

workspace(name = repository_name)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

load("//:tooling.bzl", register_tooling = "register")

register_tooling()

load("//:third_party_deps.bzl", dependencies = "dependencies")

dependencies()


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
        "//tests/e2e/mvn-lib-subparent/mvn-lib-module-non-compiled:maven",
        "//tests/e2e/mvn-lib:maven",
        "//tests/e2e/mvn-lib-parent:maven",
    ],
)
