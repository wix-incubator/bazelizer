_BUILD = """
load("@wix_incubator_bazelizer//private/ruls/maven_v2:module.bzl", _go_offline_modules = "go_offline")

_go_offline_modules(
    name = "{go_offline_target_name}",
    visibility = ["//visibility:public"],
    modules = [{go_offline_modules}],
    external_settings = "{unsafe_global_settings}",
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
load("@wix_incubator_bazelizer//private/ruls/maven_v2:runner.bzl", _run_mvn = "run_mvn")

def execute_build(name, **kwargs):
    _run_mvn(
        name = name,
        repository = "{maven_repository_ref}",
        data = ["{data}"],
        **kwargs
    )
"""

_maven_repository_registry_attrs = {
    "modules": attr.label_list(),
    "use_unsafe_local_cache": attr.bool(default = True),
    "use_global_cache": attr.bool(default = True),
    "repositories": attr.string_dict(),
    "_dummy": attr.label(default = Label("//maven:defs.bzl"))
}

_maven_binary_version = "3.6.3"
_maven_binary_sha256 = "26ad91d751b3a9a53087aefa743f4e16a17741d3915b219cf74112bf87a438c5"
_maven_binary_uri = "https://www2.apache.paket.ua/maven/maven-3/" + _maven_binary_version + "/binaries/apache-maven-" + _maven_binary_version + "-bin.tar.gz"

_maven_dist_name = "maven_%s" % (_maven_binary_version.replace(".", "_"))

def _maven_repository_registry_impl(repository_ctx):
    repository_name = repository_ctx.name
    maven_repository_target_name = "pinned_maven_repository"
    settings_xml_json = repository_ctx.path("settings_xml.json")

    repositories = []
    user_mvn_repo = repository_ctx.path(repository_ctx.os.environ["HOME"] + "/.m2/repository/")
    if repository_ctx.attr.use_unsafe_local_cache and user_mvn_repo.exists:
        repository_ctx.report_progress("Using host maven repository: %s" % (user_mvn_repo))
        profile_id = "__host_m2_cache__"
        url = "file://%s" % (user_mvn_repo)

        repositories.append(
            struct(id=profile_id, url=url)
        )

    repository_ctx.file(
        "BUILD",
        (_BUILD).format(
            maven_dist_name = _maven_dist_name,
            go_offline_target_name = maven_repository_target_name,
            go_offline_modules = ",".join([
                '"@%s%s"' % (d.workspace_name, d) for d in repository_ctx.attr.modules
            ]),
            unsafe_global_settings = settings_xml_json,
            use_global_cache = repository_ctx.attr.use_global_cache
        ),
        False,  # not executable
    )

    if repository_ctx.attr.repositories:
        for id, url in repository_ctx.attr.repositories.items():
            repositories.append(
                struct(id=id, url=url)
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
        "\n".join([ "'%s'" % (x.to_json()) for x in repositories]),
        False,  # not executable
    )

maven_repository_registry = repository_rule(
    attrs = _maven_repository_registry_attrs,
    implementation = _maven_repository_registry_impl,
    environ = ["MAVEN_HOME"]
)