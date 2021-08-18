load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file", "http_jar")


RULES_JVM_EXTERNAL_TAG = "3.1"
RULES_JVM_EXTERNAL_SHA = "e246373de2353f3d34d35814947aa8b7d0dd1a58c2f7a6c41cfeaff3007c2d14"
RULES_JVM_NAME = "rules_jvm_external"

MAVEN_BINARY_NAME = "wix_incubator_bazelizer_maven_binary_tool"
_MAVEN_BINARY_VERSION = "3.6.3"
_MAVEN_BINARY_SHA256 = "26ad91d751b3a9a53087aefa743f4e16a17741d3915b219cf74112bf87a438c5"

_MAVEN_BUILD_FILE = """
filegroup(
    name = "{mvn_files_target}",
    visibility = ["//visibility:public"],
    srcs = glob(["bin/**", "boot/**", "conf/**", "lib/**"]),
)
"""

COURSIER_JAR_BINARY_NAME = "wix_incubator_bazelizer_coursier"
_COURSIER_CLI_VERSION = "v2.0.16"
_COURSIER_CLI_HTTP_FILE_NAME = ("coursier_cli_" + _COURSIER_CLI_VERSION).replace(".", "_").replace("-", "_")
_COURSIER_CLI_GITHUB_ASSET_URL = "https://github.com/coursier/coursier/releases/download/{COURSIER_CLI_VERSION}/coursier.jar".format(COURSIER_CLI_VERSION = _COURSIER_CLI_VERSION)
_COURSIER_CLI_SHA256 = "076de041cbebc0a1272b84f1e69f6da5df4961847850b95cb3dfa3f776145225"

_COURSIER_BUILD_FILE = """
filegroup(
    name = "{id}",
    visibility = ["//visibility:public"],
    srcs = glob(["*.jar"]),
)
"""

# Run 'bazel run //:mirror_coursier' to upload a copy of the jar to the Bazel mirror.
#_COURSIER_CLI_BAZEL_MIRROR_URL = "https://mirror.bazel.build/coursier_cli/" + _COURSIER_CLI_HTTP_FILE_NAME + ".jar"
#_COURSIER_CLI_SHA256 = "076de041cbebc0a1272b84f1e69f6da5df4961847850b95cb3dfa3f776145225"

def register():
    if native.existing_rule(RULES_JVM_NAME) == None:
        http_archive(
            name = RULES_JVM_NAME,
            sha256 = RULES_JVM_EXTERNAL_SHA,
            strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
            url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
        )

    http_archive(
       name = MAVEN_BINARY_NAME,
       url = "https://www2.apache.paket.ua/maven/maven-3/" + _MAVEN_BINARY_VERSION + "/binaries/apache-maven-" + _MAVEN_BINARY_VERSION + "-bin.tar.gz",
       build_file_content = _MAVEN_BUILD_FILE.format(
               mvn_files_target=MAVEN_BINARY_NAME
       ),
       sha256 = _MAVEN_BINARY_SHA256,
       strip_prefix = "apache-maven-" + _MAVEN_BINARY_VERSION
    )

    http_jar(
       name = COURSIER_JAR_BINARY_NAME,
       url = _COURSIER_CLI_GITHUB_ASSET_URL,
       sha256 = _COURSIER_CLI_SHA256
    )

