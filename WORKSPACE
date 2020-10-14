repository_name = "bazelizer"

workspace(name = repository_name)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "3.3"
RULES_JVM_EXTERNAL_SHA = "d85951a92c0908c80bd8551002d66cb23c3434409c814179c0ff026b53544dab"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("//third_party:defs.bzl", "dependencies")
dependencies()

load("//third_party:pinn.bzl", "dependencies")
dependencies()


http_archive(
   name = "maven_bin",
   url = "https://www2.apache.paket.ua/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz",
   build_file = "//private/ruls/maven:BUILD.maven",
   sha256 = "26ad91d751b3a9a53087aefa743f4e16a17741d3915b219cf74112bf87a438c5",
   strip_prefix = "apache-maven-3.6.3"
)