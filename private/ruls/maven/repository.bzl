load(":pom.bzl", "PomInfo")

def get_transitive_deps(srcs, deps):
  """Obtain the source files for a target and its transitive dependencies.

  Args:
    srcs: a list of source files
    deps: a list of targets that are direct dependencies
  Returns:
    a collection of the transitive sources
  """
  return depset(
        srcs,
        transitive = [dep[PomInfo].deps for dep in deps])

_BuildDef = provider(fields={
    "id": "pom file",
    "parent_id": "id of parent",
    "file": "pom file",
})

def _maven_repository_impl(ctx):
    pom_artifact_deps = depset([],transitive = [dep[PomInfo].deps for dep in ctx.attr.deps])
    pom_providers = [dep[PomInfo] for dep in ctx.attr.deps]
    pom_files = [f.file for f in pom_artifact_deps.to_list()  ]

    tar_name = ctx.label.name + ".tar"
    tar = ctx.actions.declare_file(tar_name)

    reposiotry_def_args = ctx.actions.args()

    for pom_provider in pom_providers:
        reposiotry_def_args.add(
            _BuildDef(
                id = pom_provider.id,
                parent_id = pom_provider.parent_id
            ).to_json()
        )

    build_def = ctx.actions.declare_file('reposiotry_def_args.json')
    ctx.actions.write(
        output = build_def,
        content = reposiotry_def_args
    )

    args = ctx.actions.args()
    args.add('repository')
    args.add('--def', build_def)

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
        "deps": attr.label_list(),
        "_tool": attr.label(default="//private/tools/jvm/mvn", allow_files = True, executable = True, cfg = "host")
    }
)