_BUILD = """
package(default_visibility = ["//visibility:{visibility}"])
{imports}
"""


_BUILD_PIN = """
load("@wix_incubator_bazelizer//private/ruls/maven:repository.bzl", _maven_repository = "maven_repository")

_maven_repository(
    name = "{maven_repository_name}",
    visibility = ["//visibility:public"],
    modules = [{modules}],
    data = [{data}],
    {properties}
)

"""

_BUILD_M2 = """

filegroup(
    name = "{name}",
    srcs = glob([
        "{user_m2_repo}/**/*"
    ]),
)
"""


_EXEC_RULE = """
load("@wix_incubator_bazelizer//private/ruls/maven:runner.bzl", _run_mvn = "run_mvn")

def execute_build(name, **kwargs):
    _run_mvn(
        name = name,
        repository = "{maven_repository_ref}",
        **kwargs
    )
"""


_maven_repository_registry_attrs = {
    "modules": attr.label_list(),
    "use_unsafe_local_cache": attr.bool(default = True),
}

def _maven_repository_registry_impl(repository_ctx):
    use_unsafe_local_cache = repository_ctx.attr.use_unsafe_local_cache
    visibility = "public"
    repository_name = repository_ctx.name
    maven_repository_target_name = "pinned_maven_repository"
    modules = ",".join([
        '"@%s%s"' % (d.workspace_name, d) for d in repository_ctx.attr.modules
    ])

    properties = dict()
    data = []

    user_mvn_repo = repository_ctx.path(repository_ctx.os.environ["HOME"] + "/.m2/repository/")
    if use_unsafe_local_cache and user_mvn_repo.exists:
        repository_ctx.report_progress("use host's maven repository: %s" % (user_mvn_repo))
        properties['unsafe_local_cache'] = '"%s"' % (user_mvn_repo)

    repository_ctx.file(
        "BUILD",
        (_BUILD + _BUILD_PIN).format(
            visibility = visibility,
            maven_repository_name = maven_repository_target_name,
            modules = modules,
             data = ",".join(data),
            imports = '',
            properties = ",".join([
               '%s = %s' % (key, value) for key,value in properties.items()
            ]),
        ),
        False,  # not executable
    )

    repository_ctx.file(
        "execute_build.bzl",
        (_EXEC_RULE).format(
            maven_repository_ref = "@%s//:%s" % (repository_name, maven_repository_target_name),
            imports = "",
        ),
        False,  # not executable
    )


maven_repository_registry = repository_rule(
    attrs = _maven_repository_registry_attrs,
    implementation = _maven_repository_registry_impl,
    environ = ["MAVEN_HOME"]
)