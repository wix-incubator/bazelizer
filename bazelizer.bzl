load("//third_party:maven_binaries.bzl", register_tooling = "register")
load("//third_party:third_party.bzl", "dependencies")

def bazelizer():
    register_tooling()
    dependencies()