load("//third_party:maven_binaries.bzl", "MAVEN_BINARY_NAME")


_BUILD = """
load("@wix_incubator_bazelizer//private/ruls/maven_v3:go_offline.bzl", _go_offline = "go_offline")

_go_offline(
    name = "{go_offline_target_name}",
    visibility = ["//visibility:public"],
    modules = [{go_offline_modules}],
    repos = {repos},
    global_flags = [ {flags} ]
)

"""

_RUNNER_BZL_FILE = """
load("@wix_incubator_bazelizer//private/ruls/maven_v3:runner.bzl", _run_mvn = "run_mvn")

def execute_build(name, **kwargs):
    _run_mvn(
        name = name,
        repository = "{maven_repository_ref}",
        **kwargs
    )
"""

def repository(name, url, snapshot=False):
    pass

_maven_repository_registry_attrs = {
    "modules": attr.label_list(),
    "global_falgs": attr.string_list(),
    "repositories": attr.string_dict(),
    "_dummy": attr.label(default = Label("//maven:defs.bzl")),
}

def _maven_repository_registry_impl(repository_ctx):
    repository_name = repository_ctx.name
    maven_repository_target_name = "pinned_maven_repository"
    settings_xml_json = repository_ctx.path("settings_xml.json")

    repositories = []
    if repository_ctx.attr.repositories:
        for id, url in repository_ctx.attr.repositories.items():
            repositories.append(struct(id = id, url = url))

    user_mvn_repo = repository_ctx.path(repository_ctx.os.environ["HOME"] + "/.m2/repository/")
    if user_mvn_repo.exists:
        repository_ctx.report_progress("Using host maven repository: %s" % (user_mvn_repo))
        profile_id = "_host_local_m2_cache"
        url = "file://%s" % (user_mvn_repo)
        repositories.append(struct(id = profile_id, url = url))

    flags = [ ]
    if repository_ctx.attr.global_falgs:
        flags = repository_ctx.attr.global_falgs

    repos_str = "{ %s }" % ( ",".join(['"%s":"%s"' % (r.id, r.url) for r in repositories]) )
    flags_str = ",".join(['"%s"' % r for r in flags])
    repository_ctx.file(
        "BUILD",
        (_BUILD).format(
            go_offline_target_name = maven_repository_target_name,
            go_offline_modules = ",".join([
                '"@%s%s"' % (d.workspace_name, d)
                for d in repository_ctx.attr.modules
            ]),
            flags = flags_str,
            repos = repos_str,
        ),
        False,  # not executable
    )

    repository_ctx.file(
        "execute_build.bzl",
        (_RUNNER_BZL_FILE).format(
            maven_repository_ref = "@%s//:%s" % (repository_name, maven_repository_target_name),
            imports = "",
        ),
        False,  # not executable
    )

    repository_ctx.file(
        settings_xml_json,
        "\n".join(["'%s'" % (x.to_json()) for x in repositories]),
        False,  # not executable
    )

maven_repository_registry = repository_rule(
    attrs = _maven_repository_registry_attrs,
    implementation = _maven_repository_registry_impl,
    environ = ["MAVEN_HOME"],
)
