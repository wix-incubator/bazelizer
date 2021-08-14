load("//private/ruls/maven_v3:maven_runtime_registrary_modules.bzl", _maven_repository_registry_v3 = "maven_repository_registry")
load("//private/ruls/maven_v3:maven_project.bzl", _maven_project = "maven_project")

maven_repository_registry = _maven_repository_registry_v3
maven_project = _maven_project