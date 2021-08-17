


def e2e_has_jar_test(name, dep, jar, clazz=None):
    """
    sh_library(
        name = "mvn-build-lib-with-profile__maven",
        srcs = ["dummy.sh"],
        data = ["//tests/e2e/mvn-build-lib-with-profile"],
    )

    sh_test(
        name = "mvn-build-lib-one__test",
        srcs = ["test_jar.sh"],
        args = ["tests/e2e/mvn-build-lib-one/libmvn-build-lib-one.jar"],
        data = ["//tests/e2e/mvn-build-lib-one"],
        deps = ["//tests/e2e/mvn-build-lib-one"]
    )
    """
    name_ = "agg=_" + str(name)
    native.sh_library(
        name = name_,
        srcs = ["dummy.sh"],
        data = [dep],
    )

    arg_ = [jar]
    if clazz:
        arg_.append(str(clazz))

    native.sh_test(
        name = str(name),
        srcs = ["test_jar.sh"],
        args = arg_,
        data = [":" + name_],
        deps = [":" + name_]
    )
