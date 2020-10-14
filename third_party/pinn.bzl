load("@rules_bazelizer_maven//:defs.bzl", "pinned_maven_install")

def dependencies():
    pinned_maven_install()