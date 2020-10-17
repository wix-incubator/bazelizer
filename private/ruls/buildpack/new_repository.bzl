


def new_repository_impl(repository_ctx):

    _bin = repository_ctx.path(repository_ctx.attr._tool.package)
    # print(_bin)
    # repository_ctx.execute([_bin, "-h"])
    x = repository_ctx.execute(["echo", "h"])
    print(x)


maven_repository = repository_rule(
    implementation = new_repository_impl,
    attrs = {
        "_tool": attr.label(default="//private/tools/jvm/mvn")
    }
)