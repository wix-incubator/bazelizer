repository_name = "wix_incubator_bazelizer"

workspace(name = repository_name)

load("//third_party:rules_repository.bzl", "install")
install()


#
# E2E tests
#

load("//maven:defs.bzl", "maven_repository_registry_v2")
maven_repository_registry_v2(
    name = "maven_e2e_v2",
    modules = [
        "//tests/e2e/mvn-lib-a:module",
        "//tests/e2e/mvn-lib-parent:module",
        "//tests/e2e/mvn-build-lib-one:module",
        "//tests/e2e/mvn-lib-b:module",
    ],
    use_global_cache = False
)