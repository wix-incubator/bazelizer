
M2_REPO_IMG_EXT = ".tar"
BASE_POM_NAME = "base_pom.xml"
_TOOL = Label("//private/tools/jvm/mvn:mvn")


def _find_exactly_one(_depset, file_sufix):
    array = [x for x in _depset.to_list() if x.path.endswith(file_sufix)]
    if len(array) != 1: fail("not found file that ends with %s in %s" % (file_sufix, _depset))
    else: return array[0]


MvnBuildpackInfo = provider(
    fields = {"pom": "pom file", "tarball": "archive m2 repository"},
)

def _create_mvn_repository_impl(ctx):
    _content = ctx.attr.pom_file_init_content
    _content_file = ctx.file.pom_file_init_src
    if _content and not _content_file:
        initial_pom = ctx.actions.declare_file(BASE_POM_NAME)
        ctx.actions.write(initial_pom, ctx.attr.pom_file_init_content)
    elif _content_file and not _content:
        initial_pom = _content_file
    else:
        fail('Must use either "pom_file_init_content" or "pom_file_init_src" attr')

    archive = ctx.actions.declare_file(ctx.label.name + M2_REPO_IMG_EXT)

    args = ctx.actions.args()
    args.add("repo2tar")
    args.add("--output", archive.path)
    args.add("--pom", initial_pom.path)

    ctx.actions.run(
        inputs = depset([initial_pom], transitive = []),
        outputs = [archive],
        arguments = [args],
        executable = ctx.executable._tool,
        use_default_shell_env = True,
        progress_message = "createing mvn repository tarball... %s" % (ctx.label),
    )

    return [
        DefaultInfo(files = depset([archive, initial_pom])),
        MvnBuildpackInfo(pom = initial_pom, tarball = archive),
    ]

create_mvn_buildpack = rule(
    implementation = _create_mvn_repository_impl,
    attrs = {
        "pom_file_init_content": attr.string(),
        "pom_file_init_src": attr.label(allow_single_file = True),
        "_tool": attr.label(
            default = _TOOL,
            allow_files = True,
            executable = True,
            cfg = "host",
        ),
    },
)


def _manifest(name, ctx, files_paths):
    manifest = ctx.actions.declare_file(name + ".manifest")
    args = ctx.actions.args()
    args.add_all(files_paths)
    ctx.actions.write(manifest, args)
    return manifest

def _collect_deps(dep_targets):
    # Collect only direct dependencies for each target
    _direct_deps = []
    for dep_target in dep_targets:
        # Expect only java compatible targets
        java_provider = dep_target[JavaInfo]

        # We should use full_compile_jars to omit interface jars (ijar and hjar)
        # as maven build required fully compiled jar files
        for d in java_provider.full_compile_jars.to_list():
            # Rule scala_import provide specific jar to collect all exports
            # so, we should exclude it
            if d.path.endswith("PlaceHolderClassToCreateEmptyJarForScalaImport.jar"):
                continue
            _direct_deps.append(d)

    return _direct_deps

def _execute_build_impl(ctx):
    buildpack_info = ctx.attr.buildpack[MvnBuildpackInfo]
    deps = _collect_deps(ctx.attr.deps)
    srcs_manifest = _manifest("srcs", ctx, [src for src in ctx.files.srcs])
    deps_manifest = _manifest("deps", ctx, [d.path for d in deps])
    outputs = []
    output_args = []
    for o in ctx.attr.outputs:
        declare_file = ctx.actions.declare_file(o)
        outputs.append(declare_file)
        output_args.append("-O{declared_file}={file_in_mvn_target}".format(declared_file = declare_file.path, file_in_mvn_target = o))

    args = ctx.actions.args()
    args.add("build")
    args.add("--pom", buildpack_info.pom)
    args.add("--repo", buildpack_info.tarball)
    args.add("--srcs", srcs_manifest)
    args.add("--deps", deps_manifest)
    if ctx.attr.artifactId:
        args.add("--artifactId", ctx.attr.artifactId)
    if ctx.attr.groupId:
        args.add("--groupId", ctx.attr.groupId)
    args.add_all(output_args)

    ctx.actions.run(
        inputs = depset([srcs_manifest, deps_manifest],
                        transitive = [depset([buildpack_info.pom, buildpack_info.tarball]), depset(deps)] + [f.files for f in ctx.attr.srcs]),
        outputs = outputs,
        arguments = [args],
        executable = ctx.executable._tool,
        use_default_shell_env = True,
        progress_message = "Running maven build...",
    )

    runfiles = ctx.runfiles(
        files = outputs,
    )

    return [
        DefaultInfo(
            files = depset(outputs),
            runfiles = runfiles,
        ),
    ]

run_mvn_buildpack = rule(
    implementation = _execute_build_impl,
    attrs = {
        "deps": attr.label_list(),
        "srcs": attr.label_list(mandatory = True),
        "outputs": attr.string_list(),
        "artifactId": attr.string(),
        "groupId": attr.string(),
        "buildpack": attr.label(mandatory = True, allow_files = True),
        "_tool": attr.label(
            default = _TOOL,
            allow_files = True,
            executable = True,
            cfg = "host",
        ),
    },
)
