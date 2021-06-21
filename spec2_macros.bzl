load("@io_bazel_rules_scala//scala:scala.bzl", "scala_specs2_junit_test","scala_library")
load("@io_bazel_rules_scala//specs2:specs2_junit.bzl", "specs2_junit_dependencies")

target_test_classes = "target/test-classes"

_unit_prefixes = ["Test"]
_unit_suffixes = _unit_prefixes
_unit_tags = ["UT"]


def specs2_unit_test(name,
                     extra_runtime_dirs = [target_test_classes],
                     extra_runtime_entries = [target_test_classes],
                     **kwargs):
      """
      Adopted from https://github.com/wix/exodus/blob/dfb0c9713b07a8b6a49b548b7b543021e748d80b/tests.bzl
      """
      size = kwargs.pop("size", "small")
      timeout = kwargs.pop("timeout", None)

      #extract attribute(s) common to both test and scala_library
      user_test_tags = kwargs.pop("tags", _unit_tags)
      data = kwargs.pop("data", [])

      _add_test_target(
          name,
          _unit_prefixes,
          _unit_suffixes,
          _unit_tags,
          size,
          timeout,
          extra_runtime_dirs,
          extra_runtime_entries,
          **kwargs
      )

def _add_test_target(name,
                     prefixes,
                     suffixes,
                     test_tags,
                     size,
                     timeout,
                     extra_runtime_dirs,
                     extra_runtime_entries,
                     **kwargs):
  #extract attribute(s) common to both test and scala_library
  user_test_tags = kwargs.pop("tags", test_tags)
  #Bazel idiomatic wise `data` is needed in both.
  #(scala_library for other tests that might need the data in the runfiles and the test needs it so that it can do $location expansion)
  data = kwargs.pop("data", [])
  #extract attributes which are only for the test runtime
  end_prefixes = kwargs.pop("prefixes", prefixes)
  end_suffixes = kwargs.pop("suffixes", suffixes)
  jvm_flags = kwargs.pop("jvm_flags", [])
  flaky = kwargs.pop("flaky", None)
  shard_count = kwargs.pop("shard_count", None)
  args = kwargs.pop("args", None)
  local = kwargs.pop("local", None)
  deps = kwargs.pop("deps",[])

  jvm_flags.extend([
      "-Dextra.dirs=" + ":".join(extra_runtime_dirs),
  ])

  testonly = kwargs.pop("testonly", 1)

  junit_specs2_deps = specs2_junit_dependencies() + [
    "//external:io_bazel_rules_scala/dependency/junit/junit",
    "//external:io_bazel_rules_scala/dependency/hamcrest/hamcrest_core",
  ]

  scala_library(
      name = name,
      tags = user_test_tags,
      data = data,
      testonly = testonly,
      deps = junit_specs2_deps + deps,
      **kwargs
  )

  scala_specs2_junit_test(
      name = name + "_test_runner",
      prefixes = end_prefixes,
      suffixes = end_suffixes,
      deps = deps,
      runtime_deps = [":" + name],
      tests_from = [":" + name],
      jvm_flags = jvm_flags,
      size = size,
      timeout = timeout,
      flaky = flaky,
      shard_count = shard_count,
      args = args,
      local = local,
      data = data,
      tags = user_test_tags,
  )
