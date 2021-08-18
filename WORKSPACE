repository_name = "wix_incubator_bazelizer"

workspace(name = repository_name)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

load("//third_party:third_party.bzl", "dependencies")

dependencies()

#load("@rules_jvm_external//:defs.bzl", "maven_install")
#
#maven_install(
#        artifacts = [
#            "org.sonatype.aether:aether-api:1.7",
#            "org.sonatype.aether:aether-spi:1.7",
#            "org.sonatype.aether:aether-util:1.7",
#            "org.sonatype.aether:aether-connector-file:1.7",
#            "org.sonatype.aether:aether-connector-wagon:1.7",
#            "org.sonatype.aether:aether-impl:1.7",
#        ],
#        fetch_sources = True,
#        repositories = [
#            "https://repo1.maven.org/maven2",
#        ],
#)

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
        "//tests/e2e/mvn-lib-codegen:maven",
        "//tests/e2e/mvn-lib:maven",
        "//tests/e2e/mvn-lib-parent:maven",
    ],
)
