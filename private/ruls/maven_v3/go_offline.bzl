load(":maven_project.bzl", "PomProjectInfo")

CLI_TOOL = "//private/src/main/java/com/wix/incubator/mvn"

GoOfflineMavenInfo = provider(fields={
    "tar": "Snapshot of m2 repo"
})


def _mk_json_args_file(ctx, name, list_strucs):
    file = ctx.actions.declare_file(name)
    args = ctx.actions.args()
    for s in list_strucs:
        args.add(s.to_json())

    ctx.actions.write(
        output = file,
        content = args
    )

    return file

def _go_offline_impl(ctx):
    pom_providers = [ dep[PomProjectInfo] for dep in ctx.attr.modules ]

    reposiotry_projects_file = _mk_json_args_file(
        ctx,
        'reposiotry_projects.json',
        [  struct(file = pom.file.path, flags = pom.flags) for pom in pom_providers ]
    )

    settings_conf_file = _mk_json_args_file(
        ctx,
        'settings_conf.json',
        [ struct(id=r,url=url) for r, url in ctx.attr.repos.items() ]
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
    args.add('--settingsXml', settings_conf_file.path)
    args.add('--output', snapshot_tar.path)
    print('MavenRepositoryMaker: logging file %s' % logfile.path)
    outputs = [
        snapshot_tar,
        logfile
    ]

    ctx.actions.run(
        inputs = depset([reposiotry_projects_file, settings_conf_file] + [d.file for d in pom_providers]),
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
        "repos": attr.string_dict(),
        "data": attr.label_list(allow_files = True),
        "_tool": attr.label(default=CLI_TOOL, allow_files = True, executable = True, cfg = "host")
    }
)