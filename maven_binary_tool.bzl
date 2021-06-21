load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

_maven_binary_version = "3.6.3"
_maven_binary_sha256 = "26ad91d751b3a9a53087aefa743f4e16a17741d3915b219cf74112bf87a438c5"

MAVEN_BINARY_NAME = "wix_incubator_bazelizer_maven_binary_tool"
MAVEN_BINARY_NAME_CLI = "wix_incubator_bazelizer_maven_binary_tool"

_MAVEN_BUILD_FILE = """
filegroup(
    name = "{mvn_files_target}",
    visibility = ["//visibility:public"],
    srcs = glob(["bin/**", "boot/**", "conf/**", "lib/**"]),
)
"""

def install():
    http_archive(
       name = MAVEN_BINARY_NAME,
       url = "https://www2.apache.paket.ua/maven/maven-3/" + _maven_binary_version + "/binaries/apache-maven-" + _maven_binary_version + "-bin.tar.gz",
       build_file_content = _MAVEN_BUILD_FILE.format(
               mvn_files_target=MAVEN_BINARY_NAME
       ),
       sha256 = _maven_binary_sha256,
       strip_prefix = "apache-maven-" + _maven_binary_version
    )