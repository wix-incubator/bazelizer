_BUILD = """
package(default_visibility = ["//visibility:{visibility}"])
load("@wix_incubator_bazelizer//private/ruls/maven:repository.bzl", _maven_repository = "maven_repository")
{imports}
"""


_BUILD_PIN = """

_maven_repository(
    name = "{maven_repository_name}",
    visibility = ["//visibility:public"],
    modules = [
        {modules}
    ]
)

def execute_build(name, **kwargs):
    _run_mvn(
        name = name,
        repository = "{maven_repository_name}",
        **kwargs
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
}

def _maven_repository_registry_impl(repository_ctx):
    repository_name = repository_ctx.name
    visibility = "public"
    maven_repository_target_name = "pinned_maven_repository"
    modules = ",".join(
        [ '"@%s%s"' % (d.workspace_name,d) for d in repository_ctx.attr.modules ]
    )

    repository_ctx.file(
        "BUILD",
        (_BUILD + _BUILD_PIN).format(
            visibility = visibility,
            maven_repository_name = maven_repository_target_name,
            modules = modules,
            imports = "",
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

#    mvn_cache_location = repository_ctx.path(repository_ctx.os.environ["HOME"] + "/" +".m2/repository")
#    if mvn_cache_location.exists:
#        repository_ctx.report_progress("creating mvn repo compat symlinks from "+ str(mvn_cache_location))
#        records = mvn_cache_location.readdir()
#        for mvn_cache_record in records:
#            print(mvn_cache_record)
#    else:
#        repository_ctx.report_progress("No ~/.m2/repository found yet")


maven_repository_registry = repository_rule(
    attrs = _maven_repository_registry_attrs,
    implementation = _maven_repository_registry_impl,
    environ = ["MAVEN_HOME"]
)