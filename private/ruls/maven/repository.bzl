
MavenModuleInfo = provider(fields = {
    "file": "",
    "parent_file": "",
    "deps": "",
})


def _declare_pom_impl(ctx):
    pom_file = ctx.file.pom_file
    parent_file = None
    transitive_deps = []

    if ctx.attr.parent:
        mvn_module_info = ctx.attr.parent[MavenModuleInfo]
        parent_file = mvn_module_info.file
        transitive_deps = [mvn_module_info.deps]


    return [
        DefaultInfo(files = depset([pom_file])),
        MavenModuleInfo(
            file = pom_file,
            parent_file = parent_file,
            deps = depset(direct=[], transitive = transitive_deps)
        )
    ]


declare_pom = rule(
    implementation = _declare_pom_impl,
    attrs = {
        "pom_file": attr.label(allow_single_file=True),
        "parent": attr.label(allow_files=True),
    }
)


_BuildDef = provider(fields={
    "file": "pom file",
    "parent_file": "pom file",
})

def _maven_repository_impl(ctx):
    pom_artifact_deps = depset([],transitive = [dep[MavenModuleInfo].deps for dep in ctx.attr.modules])


    tar_name = ctx.label.name + ".tar"
    tar = ctx.actions.declare_file(tar_name)

    reposiotry_def_args = ctx.actions.args()
    pom_providers = [dep[MavenModuleInfo] for dep in ctx.attr.modules]
    for pom_provider in pom_providers:
        reposiotry_def_args.add(
            _BuildDef(
                file = pom_provider.file.path,
                parent_file = pom_provider.parent_file.path if pom_provider.parent_file else None
            ).to_json()
        )


    build_def = ctx.actions.declare_file('reposiotry_def_args.json')
    ctx.actions.write(
        output = build_def,
        content = reposiotry_def_args
    )

    args = ctx.actions.args()
    args.add('build-repository')
    args.add('--def', build_def.path)
    args.add('--writeImg', tar.path)

    pom_files = [f.file for f in pom_artifact_deps.to_list()]
    print(pom_files)
    ctx.actions.run(
        inputs = pom_files + [build_def],
        outputs = [ctx.outputs.img],
        arguments = [args],
        executable = ctx.executable._tool
    )

    return [
        DefaultInfo(
            files= depset([tar])
        )
    ]

maven_repository = rule(
    implementation = _maven_repository_impl,
    outputs = {
      "img": "%{name}.tar"
    },
    attrs = {
        "modules": attr.label_list(),
        "_tool": attr.label(default="//private/tools/jvm/mvn", allow_files = True, executable = True, cfg = "host")
    }
)