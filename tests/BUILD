load(":e2e.bzl", "e2e_has_jar_test")

# E2E tests
e2e_has_jar_test(
    name = "mvn-lib-a__jar-test",
    dep = "//tests/e2e/mvn-lib",
    jar = "tests/e2e/mvn-lib/libmvn-lib.jar"
)

e2e_has_jar_test(
    name = "mvn-lib-module-a__jar-test",
    dep = "//tests/e2e/mvn-lib-subparent/mvn-lib-module-a",
    jar = "tests/e2e/mvn-lib-subparent/mvn-lib-module-a/libmvn-lib-module-a.jar",
    clazz = "A.class"
)

e2e_has_jar_test(
    name = "mvn-lib-module-b__jar-test",
    dep = "//tests/e2e/mvn-lib-subparent/mvn-lib-module-b",
    jar = "tests/e2e/mvn-lib-subparent/mvn-lib-module-b/libmvn-lib-module-b.jar",
    clazz = "B.class"
)

e2e_has_jar_test(
    name = "mvn-lib-module-non-compiled__jar-test",
    dep = "//tests/e2e/mvn-lib-subparent/mvn-lib-module-non-compiled",
    jar = "tests/e2e/mvn-lib-subparent/mvn-lib-module-non-compiled/libmvn-lib-module-non-compiled.jar",
)

e2e_has_jar_test(
    name = "mvn-lib-codegen__jar-test",
    dep = "//tests/e2e/mvn-lib-codegen",
    jar = "tests/e2e/mvn-lib-codegen/libmvn-lib-codegen.jar",
)