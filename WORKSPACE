repository_name = "wix_incubator_bazelizer"

workspace(name = repository_name)

load("//third_party:rules_repository.bzl", "install")
install()


#
# E2E tests
#

load("//private/ruls/maven:repository_registry.bzl", "maven_repository_registry")
maven_repository_registry(
    name = "maven_e2e",
    modules = [
        "//tests/e2e/mvn-parent-pom:declared_pom",
        "//tests/e2e/mvn-build-lib-one:declared_pom",
        "//tests/e2e/mvn-build-lib-with-profile:declared_pom",
    ]
)