_BUILD = """
load("@wix_incubator_bazelizer//private/ruls/maven:repository.bzl", _go_offline_modules = "maven_repository")

_go_offline_modules(
    name = "{go_offline_target_name}",
    visibility = ["//visibility:public"],
    modules = [{go_offline_modules}],
    unsafe_global_settings = "{unsafe_global_settings}",
)

filegroup(
    name = "{go_offline_target_name}_files",
    visibility = ["//visibility:public"],
    srcs = glob([
        "_m2/repository/**/*"
    ]),
)
"""

_SETTINGS = """<?xml version="1.0" encoding="UTF-8"?>
<!-- Autogenerated by Basel rule, do not edit. -->
<settings>
    <localRepository>{local_repository}</localRepository>

    <activeProfiles>
        {active_profiles}
    </activeProfiles>

    {payload}
</settings>
"""

_SETTINGS_XML_NEW_REPO_PROFILE = """
  <profiles>
    <profile>
        <id>{profile}</id>
        <repositories>
            <repository>
              <snapshots><enabled>false</enabled></snapshots>
              <id>host_m2_cache</id>
              <name>Host m2 repo</name>
              <url>{url}</url>
            </repository>
        </repositories>
        <pluginRepositories>
            <pluginRepository>
              <id>host_m2_cache_plgn</id>
              <name>Host m2 repo</name>
              <url>{url}</url>
             </pluginRepository>
       </pluginRepositories>
    </profile>
  </profiles>
"""


_RUNNER_BZL_FILE = """
load("@wix_incubator_bazelizer//private/ruls/maven:runner.bzl", _run_mvn = "run_mvn")

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
}

_maven_binary_version = "3.6.3"
_maven_binary_sha256 = "26ad91d751b3a9a53087aefa743f4e16a17741d3915b219cf74112bf87a438c5"
_maven_binary_uri = "https://www2.apache.paket.ua/maven/maven-3/" + _maven_binary_version + "/binaries/apache-maven-" + _maven_binary_version + "-bin.tar.gz"

_maven_dist_name = "maven_%s" % (_maven_binary_version.replace(".", "_"))

def _maven_repository_registry_impl(repository_ctx):
    repository_name = repository_ctx.name
    maven_repository_target_name = "pinned_maven_repository"

    m2_dir = "_m2"
    settings_xml = repository_ctx.path(m2_dir + "/" + "settings.xml")
    local_repository = repository_ctx.path(m2_dir + "/" + "repository")

    settings_xml_append = ""
    settings_xml_profiles = []
    user_mvn_repo = repository_ctx.path(repository_ctx.os.environ["HOME"] + "/.m2/repository/")
    if repository_ctx.attr.use_unsafe_local_cache and user_mvn_repo.exists:
        repository_ctx.report_progress("Using host maven repository: %s" % (user_mvn_repo))
        profile_id = "host_m2_cache"
        settings_xml_append += _SETTINGS_XML_NEW_REPO_PROFILE.format(
            url = "file://%s" % (user_mvn_repo),
            profile = profile_id
        )
        settings_xml_profiles.append(profile_id)

    repository_ctx.file(
        "BUILD",
        (_BUILD).format(
            maven_dist_name = _maven_dist_name,
            go_offline_target_name = maven_repository_target_name,
            go_offline_modules = ",".join([
                '"@%s%s"' % (d.workspace_name, d) for d in repository_ctx.attr.modules
            ]),
            unsafe_global_settings = settings_xml,
        ),
        False,  # not executable
    )

    repository_ctx.file(
        settings_xml,
        (_SETTINGS).format(
            local_repository = local_repository,
            payload = settings_xml_append,
            active_profiles = "\n".join([ "<activeProfile>%s</activeProfile>" % (p) for p in settings_xml_profiles ])
        ),
        False,  # not executable
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

maven_repository_registry = repository_rule(
    attrs = _maven_repository_registry_attrs,
    implementation = _maven_repository_registry_impl,
    environ = ["MAVEN_HOME"]
)