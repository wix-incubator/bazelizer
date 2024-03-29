load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("//third_party:third_party.bzl", third_party_deps_list = "deps")

def resources(name = "resources", runtime_deps = []):
    native.java_library(
        name = name,
        resources = native.glob(["**"], exclude = ["BUILD"]),
        resource_strip_prefix = "%s/" % native.package_name(),
        runtime_deps = runtime_deps,
        testonly = 0,
        visibility = ["//visibility:public"],
    )

def _package_visibility(pacakge_name):
    return ["//{p}:__pkg__".format(p = pacakge_name)]

def sources(visibility = None):
    if visibility == None:
        visibility = _package_visibility(native.package_name())
    native.filegroup(
        name = "sources",
        srcs = native.glob(["*.java"], exclude = ["*Test.java"]) + native.glob(["*.scala"], exclude = ["*Test.scala"]),
        visibility = visibility,
    )

def test_sources(visibility = None):
    if visibility == None:
        visibility = _package_visibility(native.package_name())
    native.filegroup(
        name = "test_sources",
        srcs = native.glob(["*Test.java"]) + native.glob(["*Test.scala"]),
        visibility = visibility,
    )

def third_party_dep(d):
    return "//external:wix_incubator_bazelizer_rules/dependency/%s" % d

def third_party_deps():
    _deps = [
        third_party_dep(d.name)
        for d in third_party_deps_list
    ]

    return _deps
