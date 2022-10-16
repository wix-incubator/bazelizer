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
       url = "https://archive.apache.org/dist/maven/maven-3/" + _MAVEN_BINARY_VERSION + "/binaries/apache-maven-" + _MAVEN_BINARY_VERSION + "-bin.tar.gz",
       build_file_content = _MAVEN_BUILD_FILE.format(
               mvn_files_target=MAVEN_BINARY_NAME
       ),
       sha256 = _MAVEN_BINARY_SHA256,
       strip_prefix = "apache-maven-" + _MAVEN_BINARY_VERSION
    )
