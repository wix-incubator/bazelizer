java_library(
    name = "bazelizer",
    srcs = glob(["*.java"]),
    javacopts = [
        "-source", "8", "-target", "8",
    ],
    deps = [
        "//tests/e2e/bzl-lib/src/com/mavenizer/examples/util",
        "//tests/e2e/mvn-lib-codegen:xsd"
    ],
    visibility = ["//visibility:public"],
)