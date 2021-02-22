
load(
"//private/ruls/maven:repository.bzl",
_declare_pom = "declare_pom"
)

load(
"//private/ruls/maven:repository_registry_v2.bzl",
_maven_repository_registry = "maven_repository_registry"
)

load(
"//private/ruls/maven_v2:module.bzl",
_declare_pom_v2 = "declare_pom"
)
load(
"//private/ruls/maven_v2:modules_registrary.bzl",
_maven_repository_registry_v2 = "maven_repository_registry"
)



maven_repository_registry_v2 = _maven_repository_registry_v2
declare_module = _declare_pom_v2