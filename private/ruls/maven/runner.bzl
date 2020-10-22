load(":repository.bzl", "PomDeclarationInfo")

MvnArtifactInfo = provider(fields = {
    "pkg": """
Artifact binaries from installed maven artifact.
Represent archived maven artifact content as is from the local repository.
    """
})


def _manifest_file(name, ctx, dep_info_items):
    manifest = ctx.actions.declare_file(name + ".manifest")
    args = ctx.actions.args()
    for x in dep_info_items:
        args.add(x.to_json())
    ctx.actions.write(manifest, args)
    return manifest


def _dep_info(file, **kwargs):
    return struct(path=file, tags=dict(kwargs))


def _collect_deps(dep_targets, ctx_scope, **kwargs):
    # Collect only direct dependencies for each target
    _dep_infos = ctx_scope.manifests
    _direct_deps_files = ctx_scope.files
    for dep_target in dep_targets:
        if MvnArtifactInfo in dep_target:
            mvn_run_out = dep_target[MvnRunArtifactInfo]
            _dep_infos.append(_dep_info(mvn_run_out.pkg.path, artifact=True, **kwargs))
            _direct_deps_files.append(mvn_run_out.pkg)
            continue

        # Expect only java compatible targets
        java_provider = dep_target[JavaInfo]
        # We should use full_compile_jars to omit interface jars (ijar and hjar)
        # as maven build required fully compiled jar files
        for d in java_provider.full_compile_jars.to_list():
            # Rule scala_import provide specific jar to collect all exports
            # so, we should exclude it
            if d.path.endswith("PlaceHolderClassToCreateEmptyJarForScalaImport.jar"):
                continue
            _dep_infos.append(_dep_info(d.path, **kwargs))
            _direct_deps_files.append(d)


_run_mvn_outputs = {
    "jar": "lib%{name}.jar",
    "img": "%{name}_artifact.tar",
}

_run_mvn_attrs = {
    "repository": attr.lable(mandatory=True),
    "deps": attr.label_list(),
    "runtime_deps": attr.label_list(),
    "srcs": attr.label_list(allow_files = True),
    "pom_def": attr.lable(mandatory=True),
    "outputs": attr.string_list(),
    "log_level": attr.string(default="OFF"),
    "_tool": attr.label(default="//private/tools/jvm/mvn", allow_files = True, executable = True, cfg = "host")
}

def _run_mvn_impl(ctx):
    repository_info = ctx.attr.repository[RepositoryInfo]
    pom_def_info = ctx.attr.pom_def[PomDeclarationInfo]
    deps_struct = struct(manifests=[], files=[])
    _collect_deps(ctx.attr.deps, deps_struct)
    _collect_deps(ctx.attr.runtime_deps, deps_struct, scope='provided')

    srcs_manifest = _create_manifest_file("srcs_%s" % (ctx.label.name), ctx, [_collect_dep(src.path) for src in ctx.files.srcs])
    deps_manifest = _create_manifest_file("deps_%s" % (ctx.label.name), ctx, deps_struct.manifests)

    def_output_jar_file = ctx.outputs.jar
    def_output_artifact_file = ctx.outputs.img

    output_files = [def_output_jar_file, def_output_artifact_file]
    input_files = [repository_info.img, pom_def_info.pom_file]

    args.add("--jvm_flag=-Dtools.jvm.mvn.LogLevel=%s" % (ctx.attr.log_level))
    args.add("--jvm_flag=-Dtools.jvm.mvn.BazelLabelName=%s" % (ctx.label))

    args = ctx.actions.args()
    args.add("run")
    args.add("--repo", repository_info.img.path)
    args.add("--srcs", srcs_manifest)
    args.add("--deps", deps_manifest)
    args.add("--write-artifact", def_output_artifact_file.path)
    args.add("--write-jar", def_output_jar_file.path)
    args.add("--pom", pom_def_info.pom_file.path)
    if pom_def_info.parent_file:
        args.add("--parent-pom", pom_def_info.parent_file.path)
        input_files.append(pom_def_info.parent_file)

    for out in ctx.attr.outputs:
        file = ctx.actions.declare_file(out)
        output_files.append(file)
        args.add("-O{dest}={src}".format(dest=file.path, src.out))

    ctx.actions.run(
        inputs = depset(input_files, transitive = [f.files for f in ctx.attr.srcs]),
        outputs = output_files,
        arguments = [args],
        executable = ctx.executable._tool,
        use_default_shell_env = True,
        progress_message = "running maven build... %s" % (ctx.label),
    )

    return [
        MvnArtifactInfo(pkg = def_output_artifact_file),
        DefaultInfo(files = depset(output_files)),
        JavaInfo(output_jar = def_output_jar_file, compile_jar = def_output_jar_file)
    ]


run_mvn = rule(
    implementation = _run_mvn_impl,
    attrs = _run_mvn_attrs,
    outputs = _run_mvn_outputs
)