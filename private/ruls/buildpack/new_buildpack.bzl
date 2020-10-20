

_TOOL = Label("//private/tools/jvm/mvn:mvn")
_MARKER_SRC_DEFAULT_OUTPUT_JAR = "@@TARGET-JAR-OUTPUT@@"
#_MARKER_TALLY = "@@MVN_ARTIFACT_ALL@@"

_MvnBuildpackInfo = provider()

MvnRepositoryInfo = provider(fields={
    "pom": "",
    "pom_parent": "",
    "image": "",
})


_DepInfo = provider(
    fields = {
        "path": """Artifact binaries as is. Can be more then one file""",
        "tags": ""
    }
)
MvnRunArtifactInfo = provider(fields = {
    "pkg": """Artifact binaries as is. Can be more then one file"""
})


def _merged_dict(dicta, dictb):
    return dict(dicta.items() + dictb.items())

def _rlocation(ctx, file):
       return "${RUNFILES_DIR}/" + ctx.workspace_name + "/" + file.short_path


_common_attr = {
    "log_level": attr.string(doc="specify log level for the tool"),
    "mvn_flags": attr.string_list(),
}


_new_mvn_repository_outputs = {
    "image": "%{name}_img.tar",
}

_new_mvn_repository_attr = _merged_dict(
    _common_attr,
    {
        "pom_parent": attr.label(allow_files = True),
        "pom_file": attr.label(allow_single_file = True, mandatory = True),
        "_tool": attr.label(default = _TOOL, allow_files = True, executable = True, cfg = "host")
    }
)

_mvn_args_join_str = " "

def _mvn_args(args, ctx):
    if ctx.attr.mvn_flags and len(ctx.attr.mvn_flags) > 0:
        args.add("--args", _mvn_args_join_str.join(ctx.attr.mvn_flags))

def _mvn_args_as_list(ctx):
    if ctx.attr.mvn_flags and len(ctx.attr.mvn_flags) > 0:
        return ["--args='%s'" % (_mvn_args_join_str.join(ctx.attr.mvn_flags))]
    else: return []

def _new_mvn_repository_impl(ctx):
    optional_transitive_inputs = []
    args = ctx.actions.args()

    pom_file_input = ctx.file.pom_file

    archive = ctx.outputs.image

    if ctx.attr.log_level:
        # add jvm flags for java_binary
        args.add("--jvm_flag=-Dtools.jvm.mvn.LogLevel=%s" % (ctx.attr.log_level))

    args.add("repository")
    args.add("--writeImg", archive.path)
    args.add("--pomFile", pom_file_input.path)

    _mvn_args(args, ctx)

    if ctx.attr.pom_parent:
        parent_info = ctx.attr.pom_parent[MvnRepositoryInfo]
        args.add("--parentPomFile", parent_info.pom.path)
        args.add("--parentPomImg", parent_info.image.path)
        optional_transitive_inputs.append(parent_info.pom)
        optional_transitive_inputs.append(parent_info.image)

    ctx.actions.run(
        inputs = depset([pom_file_input], transitive = [depset(optional_transitive_inputs)]),
        outputs = [archive],
        arguments = [args],
        executable = ctx.executable._tool,
        # use_default_shell_env = True,
        progress_message = "createing mvn embedded tool... %s" % (ctx.label),
    )

    return [
        DefaultInfo(files = depset([pom_file_input, archive] + optional_transitive_inputs)),
        MvnRepositoryInfo(
           pom = pom_file_input,
           pom_parent = ctx.attr.pom_parent,
           image = archive
        )
    ]


new_mvn_repository = rule(
    implementation = _new_mvn_repository_impl,
    attrs = _new_mvn_repository_attr,
    outputs = _new_mvn_repository_outputs,
)


_create_mvn_repository_attr = _merged_dict(
    _common_attr,
    {
        "pom": attr.label(allow_files = True),
        "_tool": attr.label(default = _TOOL, allow_files = True, executable = True, cfg = "host")
    }
)

def _create_mvn_repository_impl(ctx):
    repos_info_provider = ctx.attr.pom[MvnRepositoryInfo]
    optional_transitive_inputs = []
    # Write the wrapper.
    # There is a {rulename}.runfiles directory adjacent to the tool's
    # executable file which contains all runfiles. This is not guaranteed
    # to be relative to the directory in which the executable file is run.

    # Use java binary as maven executable
    java_binary_tool = ctx.workspace_name + "/" + ctx.attr._tool[DefaultInfo].files_to_run.executable.short_path

    # Since this tool may be used by another tool, it must support accepting
    # a different runfiles directory root. The runfiles directory is always
    # adjacent to the *root* tool being run, which may not be this tool.
    # (In this case, this is done by environment variable RUNFILES_DIR.)

    runfiles_relative_image_path = _rlocation(ctx, repos_info_provider.image)
    runfiles_relative_pom_path = _rlocation(ctx, repos_info_provider.pom)

    has_parent = None

    if repos_info_provider.pom_parent:
        has_parent = True
        parent_pom_file = repos_info_provider.pom_parent[MvnRepositoryInfo].pom
        runfiles_relative_parent_pom_path = _rlocation(ctx, parent_pom_file)
        optional_transitive_inputs.append(parent_pom_file)

    script_args = " ".join([
        "build",
        "--pom={}".format(runfiles_relative_pom_path),
        "--repo={}".format(runfiles_relative_image_path),
        "--parent={}".format(runfiles_relative_parent_pom_path) if has_parent else ""
    ] + _mvn_args_as_list(ctx))

    executable = ctx.outputs.executable
    ctx.actions.write(
        output = executable,
        content = "\n".join([
            "#!/bin/bash",
            "# !! Autogenerated - do not edit !!",
            "if [[ -z \"${RUNFILES_DIR}\" ]]; then",
            "  RUNFILES_DIR=${0}.runfiles",
            "fi",
            "",
            "jvm_bin=${RUNFILES_DIR}/%s" % (java_binary_tool),
            "args=\"%s\"" % (script_args),
            "${jvm_bin} ${args} \"$@\""
        ]),
        is_executable = True,
    )

    optional_transitive_inputs.append(repos_info_provider.image)
    optional_transitive_inputs.append(repos_info_provider.pom)
    optional_transitive_inputs.append(executable)
    # TODO ctx.executable._tool ?
    optional_transitive_inputs.append(ctx.executable._tool)

    return [
        DefaultInfo(
            runfiles = ctx\
                .runfiles(files = optional_transitive_inputs)\
                .merge(ctx.attr._tool[DefaultInfo].default_runfiles),
            files = depset(optional_transitive_inputs)
        ),
        _MvnBuildpackInfo(),
        repos_info_provider
    ]


create_mvn_buildpack_executable = rule(
    implementation = _create_mvn_repository_impl,
    attrs = _create_mvn_repository_attr,
    executable = True,
)


def new_create_mvn_buildpack(name, **kwargs):
    id = "repository_for_%s" % (name)

    new_mvn_repository(
        name = id,
        **kwargs
    )

    keys = ["log_level", "visibility", "mvn_args"]
    kwargs_bypass = { key:value for (key,value) in kwargs.items() if key in keys}

    create_mvn_buildpack_executable(
        name = name,
        pom = id,
        **kwargs_bypass
    )


#
# Run executable
#

_run_mvn_buildpack_attr = _merged_dict(
    _common_attr,
    {
        "deps": attr.label_list(),
        "runtime_deps": attr.label_list(),
        "srcs": attr.label_list(mandatory = True,allow_files = True),
        "outputs": attr.string_list(),
        "artifactId": attr.string(),
        "groupId": attr.string(),
        "buildpack": attr.label(
            mandatory = True,
            allow_files = True,
            executable = True,
            cfg = "host",
        )
    }
)

def _create_manifest_file(name, ctx, inut_files_defs):
    manifest = ctx.actions.declare_file(name + ".manifest")
    args = ctx.actions.args()
    for x in inut_files_defs:
        args.add(x.to_json())
    ctx.actions.write(manifest, args)
    return manifest


def _collect_dep(file, **kwargs):
    return _DepInfo(path=file, tags=dict(kwargs))


def _collect_deps(dep_targets, ctx_scope, **kwargs):
    # Collect only direct dependencies for each target
    _direct_deps = ctx_scope.manifests
    _direct_deps_files = ctx_scope.files
    for dep_target in dep_targets:

        if MvnRunArtifactInfo in dep_target:
            mvn_run_out = dep_target[MvnRunArtifactInfo]
            _direct_deps.append(_collect_dep(mvn_run_out.pkg.path, artifact=True, **kwargs))
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
            _direct_deps.append(_collect_dep(d.path, **kwargs))
            _direct_deps_files.append(d)



def _run_mvn_buildpack_impl(ctx):
    if not ctx.attr.buildpack[_MvnBuildpackInfo]:
        fail("attr.buildpack must be created by 'create_mvn_buildpack' rule")

    deps_struct = struct(manifests=[], files=[])
    _collect_deps(ctx.attr.deps, deps_struct)
    _collect_deps(ctx.attr.runtime_deps, deps_struct, scope='provided')

    srcs_manifest = _create_manifest_file("srcs--%s" % (ctx.label.name), ctx, [_collect_dep(src.path) for src in ctx.files.srcs])
    deps_manifest = _create_manifest_file("deps--%s" % (ctx.label.name), ctx, deps_struct.manifests)
    outputs = []
    output_flags = []
    output_param_format = "-O{declared_file}={file_in_mvn_target}"
    for o in ctx.attr.outputs:
        declare_file = ctx.actions.declare_file(o)
        outputs.append(declare_file)
        output_flags.append(
            output_param_format.format(
                declared_file = declare_file.path,
                file_in_mvn_target = o
            )
        )

    args = ctx.actions.args()
    args.add("--srcs", srcs_manifest)
    args.add("--deps", deps_manifest)
    # if ctx.attr.args: args.add("--args", " ".join(ctx.attr.args))
    if ctx.attr.artifactId: args.add("--artifactId", ctx.attr.artifactId)
    if ctx.attr.groupId: args.add("--groupId", ctx.attr.groupId)

    special_output_flags = []
    special_output_flags_fmt = "--defOutFlag={flag}={declared_file}"

    def_output_jar = "lib" + ctx.label.name + ".jar"
    def_output_jar_file = ctx.actions.declare_file(def_output_jar)
    outputs.append(def_output_jar_file)
    special_output_flags.append(
        special_output_flags_fmt.format(flag="@DEF_JAR", declared_file=def_output_jar_file.path)
    )

    def_output_pkg = "lib" + ctx.label.name + "_pkg.tar"
    def_output_pkg_file = ctx.actions.declare_file(def_output_pkg)
    outputs.append(def_output_pkg_file)
    special_output_flags.append(
        special_output_flags_fmt.format(flag="@DEF_PKG", declared_file=def_output_pkg_file.path)
    )

    args.add_all(output_flags)
    args.add_all(special_output_flags)

    if ctx.attr.log_level:
        # add jvm flags for java_binary
        # wrap via wrapper_script_flag so it have to goes at the end of args line
        args.add("--wrapper_script_flag=--jvm_flag=-Dtools.jvm.mvn.LogLevel=%s" % (ctx.attr.log_level))

    # Name of current target + package
    args.add("--wrapper_script_flag=--jvm_flag=-Dtools.jvm.mvn.BazelLabelName=%s" % (ctx.label))

    ctx.actions.run(
        inputs = depset([srcs_manifest, deps_manifest],
                        transitive = [depset(deps_struct.files)] + [f.files for f in ctx.attr.srcs]),
        outputs = outputs,
        arguments = [args],
        executable = ctx.executable.buildpack,
        use_default_shell_env = True,
        progress_message = "running maven build... %s" % (ctx.label),
    )

    runfiles = ctx.runfiles(files = outputs).merge(ctx.attr.buildpack[DefaultInfo].default_runfiles)

    return [
        MvnRunArtifactInfo(pkg = def_output_pkg_file),
        DefaultInfo(files = depset(outputs), runfiles = runfiles),
        JavaInfo(output_jar = def_output_jar_file, compile_jar = def_output_jar_file)
    ]


new_run_mvn_buildpack = rule(
    implementation = _run_mvn_buildpack_impl,
    attrs = _run_mvn_buildpack_attr,
)
