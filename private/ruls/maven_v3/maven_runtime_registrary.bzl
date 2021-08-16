load("//third_party:maven_binaries.bzl", "MAVEN_BINARY_NAME")

_BUILD = """
load("@wix_incubator_bazelizer//private/ruls/maven_v3:go_offline.bzl", _go_offline = "go_offline")

_go_offline(
    name = "{go_offline_target_name}",
    visibility = ["//visibility:public"],
    modules = [{go_offline_modules}],
    settings_xml = "{unsafe_global_settings}",
    data = [ ":{go_offline_target_name}_metadata" ],
)

filegroup(
    name = "{go_offline_target_name}_files",
    visibility = ["//visibility:public"],
    srcs = glob([
        "_m2/repository/**/*"
    ]),
)

filegroup(
    name = "{go_offline_target_name}_metadata",
    visibility = ["//visibility:public"],
    srcs = glob([
        "_m2/settings.xml"
    ]),
)
"""

_RUNNER_BZL_FILE = """
load("@wix_incubator_bazelizer//private/ruls/maven_v3:runner.bzl", _run_mvn = "run_mvn")

def execute_build(name, **kwargs):
    _run_mvn(
        name = name,
        repository = "{maven_repository_ref}",
        data = ["{data}"],
        **kwargs
    )
"""

def repository(name, url, snapshot=False):
    pass

_maven_repository_registry_attrs = {
    "modules": attr.label_list(),
    "use_global_cache": attr.bool(default = True),
    "repositories": attr.string_dict(),
    "_dummy": attr.label(default = Label("//maven:defs.bzl")),
}

def _maven_repository_registry_impl(repository_ctx):
    repository_name = repository_ctx.name
    maven_repository_target_name = "pinned_maven_repository"
    settings_xml_json = repository_ctx.path("settings_xml.json")

    repositories = []
    user_mvn_repo = repository_ctx.path(repository_ctx.os.environ["HOME"] + "/.m2/repository/")
    if user_mvn_repo.exists:
        repository_ctx.report_progress("Using host maven repository: %s" % (user_mvn_repo))
        profile_id = "_host_local_m2_cache"
        url = "file://%s" % (user_mvn_repo)
        repositories.append(struct(id = profile_id, url = url))

    repository_ctx.file(
        "BUILD",
        (_BUILD).format(
            go_offline_target_name = maven_repository_target_name,
            go_offline_modules = ",".join([
                '"@%s%s"' % (d.workspace_name, d)
                for d in repository_ctx.attr.modules
            ]),
            unsafe_global_settings = settings_xml_json,
            use_global_cache = repository_ctx.attr.use_global_cache
        ),
        False,  # not executable
    )

    if repository_ctx.attr.repositories:
        for id, url in repository_ctx.attr.repositories.items():
            repositories.append(
                struct(id = id, url = url),
            )

    repository_ctx.file(
        "execute_build.bzl",
        (_RUNNER_BZL_FILE).format(
            maven_repository_ref = "@%s//:%s" % (repository_name, maven_repository_target_name),
            imports = "",
            data = "@%s//:%s" % (repository_name, "%s_files" % (maven_repository_target_name)),
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
