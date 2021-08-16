load(":maven_project.bzl", "PomProjectInfo")

CLI_TOOL = "//private/src/main/java/com/wix/incubator/mvn"

GoOfflineMavenInfo = provider(fields={
    "tar": "Snapshot of m2 repo"
})


def _go_offline_impl(ctx):
    reposiotry_def_args = ctx.actions.args()
    pom_providers = [ dep[PomProjectInfo] for dep in ctx.attr.modules ]

    for pom_provider in pom_providers:
        reposiotry_def_args.add(
            struct(
                file = pom_provider.file.path,
                # parent_file = pom_provider.parent_file.path if pom_provider.parent_file else None,
                flags = pom_provider.flags
            ).to_json()
        )

    reposiotry_projects_file = ctx.actions.declare_file('reposiotry_projects.json')
    ctx.actions.write(
        output = reposiotry_projects_file,
        content = reposiotry_def_args
    )

    args = ctx.actions.args()
    snapshot_tar = ctx.outputs.snapshot
    logfile = ctx.outputs.log

    # jfm flags
    args.add("--jvm_flag=-Dtools.jvm.mvn.LogFile=%s" % (logfile.path))
#     args.add("--jvm_flag=-Dtools.jvm.mvn.LogFile=%s" % (logfile))
    # args.add("--jvm_flag=-Dtools.jvm.mvn.Ws=%s" % (ctx.workspace_name))

    # cli options
    args.add('build-repository')
    args.add('--config', reposiotry_projects_file.path)
    args.add('--settingsXml', ctx.attr.settings_xml)
    args.add('--output', snapshot_tar.path)
    print('MavenRepositoryMaker: log %s' % logfile.path)
    outputs = [
        snapshot_tar,
        logfile
    ]

    ctx.actions.run(
        inputs = depset([reposiotry_projects_file] + [d.file for d in pom_providers]),
        outputs = outputs,
        arguments = [args],
        executable = ctx.executable._tool,
        mnemonic = "MavenRepositoryMaker"
    )

    return [
        DefaultInfo(
            files= depset(outputs)
        ),
        GoOfflineMavenInfo(
           tar = snapshot_tar
        )
    ]

go_offline = rule(
    implementation = _go_offline_impl,
    outputs = {
      "snapshot": "%{name}.snapshot.tar",
      "log": "%{name}.log",
    },
    attrs = {
        "modules": attr.label_list(),
        "settings_xml": attr.string(),
        "data": attr.label_list(allow_files = True),
        "_tool": attr.label(default=CLI_TOOL, allow_files = True, executable = True, cfg = "host")
    }
)