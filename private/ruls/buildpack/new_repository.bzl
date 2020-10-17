


def new_repository_impl(repository_ctx):
    pass


maven_repository = repository_rule(
    implementation = new_repository_impl,
    attrs = {
        "_tool": attr.label(default="//private/tools/jvm/mvn")
    }
)