

_M2_REPO_IMG_EXT = ".tar"
_BASE_POM_NAME = "base_pom.xml"
_TOOL = Label("//private/tools/jvm/mvn:mvn")
_MARKER_SRC_DEFAULT_OUTPUT_JAR = "@@TARGET-JAR-OUTPUT@@"

MvnBuildpackInfo = provider(
    fields = {"pom": "pom file", "tarball": "archive m2 repository"},
)


def _setup_common_tool_falgs(ctx, args):
    if ctx.attr.log_level:
        args.add("--syslog=%s" % (ctx.attr.log_level))


def _merged_dict(dicta, dictb):
    return dict(dicta.items() + dictb.items())

_common_attr = {
    "log_level": attr.string(doc="specify log level for the tool")
}

_create_mvn_repository_attr = _merged_dict(
    _common_attr,
    {
        "pom_file_init_content": attr.string(),
        "pom_file_init_src": attr.label(allow_single_file = True),
        "_tool": attr.label(default = _TOOL, allow_files = True, executable = True, cfg = "host"),
    }
)

def _create_mvn_repository_impl(ctx):
    _content = ctx.attr.pom_file_init_content
    _content_file = ctx.file.pom_file_init_src
    if _content and not _content_file:
        initial_pom = ctx.actions.declare_file(_BASE_POM_NAME)
        ctx.actions.write(initial_pom, ctx.attr.pom_file_init_content)
    elif _content_file and not _content:
        initial_pom = _content_file
    else:
        fail('Must use either "pom_file_init_content" or "pom_file_init_src" attr')

    archive = ctx.actions.declare_file(ctx.label.name + _M2_REPO_IMG_EXT)

    args = ctx.actions.args()
    _setup_common_tool_falgs(ctx, args)
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
    attrs = _create_mvn_repository_attr,
)


def _write_manifest_file(name, ctx, files_paths):
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


_run_mvn_buildpack_attr = _merged_dict(
    _common_attr,
    {
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
        )
    }
)


def _run_mvn_buildpack_impl(ctx):
    buildpack_info = ctx.attr.buildpack[MvnBuildpackInfo]
    deps = _collect_deps(ctx.attr.deps)
    srcs_manifest = _write_manifest_file("srcs", ctx, [src for src in ctx.files.srcs])
    deps_manifest = _write_manifest_file("deps", ctx, [d.path for d in deps])
    outputs = []
    output_args = []
    output_param = "-O{declared_file}={file_in_mvn_target}"
    for o in ctx.attr.outputs:
        declare_file = ctx.actions.declare_file(o)
        outputs.append(declare_file)
        output_args.append(
            output_param.format(
                declared_file = declare_file.path,
                file_in_mvn_target = o
            )
        )
    out_jar = ctx.label.name + ".jar"
    declared_out_jar = ctx.actions.declare_file(out_jar)
    outputs.append(declared_out_jar)
    output_args.append(
        output_param.format(
            declared_file = declared_out_jar.path,
            file_in_mvn_target = _MARKER_SRC_DEFAULT_OUTPUT_JAR
        )
    )

    args = ctx.actions.args()
    _setup_common_tool_falgs(ctx, args)
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
                        transitive = [
                            depset([buildpack_info.pom, buildpack_info.tarball]),
                            depset(deps)
                        ] + [f.files for f in ctx.attr.srcs]),
        outputs = outputs,
        arguments = [args],
        executable = ctx.executable._tool,
        use_default_shell_env = True,
        progress_message = "running maven build... %s" % (ctx.label),
    )

    runfiles = ctx.runfiles(
        files = outputs,
    )

    return [
        DefaultInfo(
            files = depset(outputs),
            runfiles = runfiles,
        ),
        JavaInfo(output_jar = declared_out_jar, compile_jar = declared_out_jar)
    ]

run_mvn_buildpack = rule(
    implementation = _run_mvn_buildpack_impl,
    attrs = _run_mvn_buildpack_attr,
)
