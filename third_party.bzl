load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven", "parse")

def dependency(coordinates, exclusions = None):
    artifact = parse.parse_maven_coordinate(coordinates)
    return maven.artifact(
        group = artifact["group"],
        artifact = artifact["artifact"],
        packaging = artifact.get("packaging"),
        classifier = artifact.get("classifier"),
        version = artifact["version"],
        exclusions = exclusions,
    )

deps = [
    dependency("org.scala-lang.modules:scala-java8-compat_2.12:0.8.0"),
    dependency("org.scala-lang.modules:scala-parser-combinators_2.12:1.0.4"),
    dependency("org.scala-lang.modules:scala-xml_2.12:1.1.0"),
    dependency("org.scala-lang:scala-compiler:2.12.6"),
    dependency("org.scala-lang:scala-library:2.12.6"),
    dependency("org.scala-lang:scala-reflect:2.12.6"),
    dependency("com.github.scopt:scopt_2.12:4.0.1"),

    dependency("org.apache.maven:maven-model:3.6.2"),
    dependency("com.google.code.gson:gson:2.8.7"),
    dependency("org.codehaus.plexus:plexus-utils:3.2.1"),
    dependency("org.apache.maven.shared:maven-invoker:3.0.1"),
    dependency("com.google.guava:guava:29.0-jre"),
    dependency('info.picocli:picocli:4.6.1'),
    dependency('org.apache.commons:commons-compress:1.20'),
    dependency('com.github.spullara.mustache.java:compiler:0.9.10'),
    dependency('org.projectlombok:lombok:1.18.2'),
]

_repositories = [
    "https://repo.maven.apache.org/maven2/",
    "https://mvnrepository.com/artifact",
    "https://maven-central.storage.googleapis.com",
    "http://gitblit.github.io/gitblit-maven",
]

def dependencies(repos = _repositories):
    maven_install(
        name = "bazelizer_deps",
        artifacts = deps,
        repositories = repos,
        generate_compat_repositories = True,
        # bazel 'run @bazelizer_deps//:pin' to acquire this json file
        # maven_install_json = "//:bazelizer_deps_install.json",
    )
