CLI_TOOL = "//private/src/main/java/com/wix/incubator/mvn"

PomProjectInfo = provider(fields = {
    "file": "",
    "deps": "",
    "flags": "",
})

def _maven_project_impl(ctx):
    pom_file = ctx.file.pom_file
    transitive_deps = []
    if ctx.attr.parent_project:
        parent_project_info = ctx.attr.parent_project[PomProjectInfo]
        transitive_deps = [parent_project_info.deps]

    return [
        DefaultInfo(files = depset([pom_file])),
        PomProjectInfo(
            file = pom_file,
            deps = depset([pom_file], transitive = transitive_deps),
            flags = ctx.attr.flags or []
        )
    ]

maven_project = rule(
    implementation = _maven_project_impl,
    attrs = {
        "pom_file": attr.label(allow_single_file=True, mandatory = True),
        "parent_project": attr.label(),
        "flags": attr.string_list(),
    }
)