

PomArtifactInfo = provider(fields = {
    "file": ""
})

PomInfo = provider(fields = {
    "id": "",
    "parent_id": "",
    "deps": ""
})


def _declare_pom_impl(ctx):
    pom_file = ctx.file.pom_file

    id = pom_file.short_path
    parent_deps = depset([])
    parent_id = None
    if ctx.attr.parent:
        parent_deps = ctx.attr.parent[PomInfo].deps
        parent_id = ctx.attr.parent[PomInfo].id

    return [
        DefaultInfo(files = depset([pom_file])),
        PomInfo(
            id = id,
            parent_id = parent_id,
            deps = depset(
                direct=[PomArtifactInfo(file=pom_file)], transitive = [parent_deps])
            )
    ]


declare_pom = rule(
    implementation = _declare_pom_impl,
    attrs = {
        "pom_file": attr.label(allow_single_file=True),
        "parent": attr.label(allow_files=True),
    }
)