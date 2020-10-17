repository_name = "bazelizer"

workspace(name = repository_name)

load("//third_party:rules_repository.bzl", "install")
install()


load("//private/ruls/buildpack:new_repository.bzl", "maven_repository")

maven_repository(
    name = "maven_repository"
)