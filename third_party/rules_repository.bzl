
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")

_def_server_urls = [
        "https://jcenter.bintray.com/",
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
]

_maven_binary_version = "3.6.3"
_maven_binary_sha256 = "26ad91d751b3a9a53087aefa743f4e16a17741d3915b219cf74112bf87a438c5"

MAVEN_BINARY_WS_NAME = "io_bazelizer_maven_binary_tool"

def install(server_urls = _def_server_urls):

    if native.existing_rule(MAVEN_BINARY_WS_NAME) == None:
        http_archive(
           name = MAVEN_BINARY_WS_NAME,
           url = "https://www2.apache.paket.ua/maven/maven-3/" + _maven_binary_version + "/binaries/apache-maven-" + _maven_binary_version + "-bin.tar.gz",
           build_file = "//private/ruls/maven:BUILD.maven",
           sha256 = _maven_binary_sha256,
           strip_prefix = "apache-maven-" + _maven_binary_version
        )

    jvm_maven_import_external(
        name = "org_apache_maven_shared_maven_invoker",
        artifact = 'org.apache.maven.shared:maven-invoker:3.0.1',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "d20e5d26c19c04199c73fd4f0b6caebf4bbdc6b872a4504c5e71a192751d9464",
        srcjar_sha256 = "94b766c063bf6345faa571ada4bd19fd4804ff2354ea6779d5894b3e37c7afa7",
        deps = [
            "@org_apache_maven_shared_maven_shared_utils"
        ]
    )

    jvm_maven_import_external(
        name = "org_apache_maven_shared_maven_shared_utils",
        artifact = 'org.apache.maven.shared:maven-shared-utils:3.2.1',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "3ba9c619893c767db0f9c3e826d5118b57c35229301bcd16d865a89cec16a7e5",
        srcjar_sha256 = "25064c72c178a98335048d0f7c3e08839e949426bc92bf905ea964146235f388",
    )

    jvm_maven_import_external(
        name = "com_github_spullara_mustache_java_compiler",
        artifact = 'com.github.spullara.mustache.java:compiler:0.9.6',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "c4d697fd3619cb616cc5e22e9530c8a4fd4a8e9a76953c0655ee627cb2d22318",
        srcjar_sha256 = "fb3cf89e4daa0aaa4e659aca12a8ddb0d7b605271285f3e108201e0a389b4c7a",
    )

    jvm_maven_import_external(
        name = "info_picocli_picocli",
        artifact = 'info.picocli:picocli:4.5.0',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "9058d90167d293f1379f49129f94424bc4c8c6cdc6a33e31bd6e4886a77733c1",
        srcjar_sha256 = "ccdad5e02718c03ef21fe0767425cb12cbf24c1be072f4b9414446fbe3332637",
    )

    jvm_maven_import_external(
        name = "com_jcabi_jcabi_xml",
        artifact = 'com.jcabi:jcabi-xml:0.22.2',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "dba351234ffa0c37557c45d06eed51b3c972f801ad56f9bfb12c6a355469e2c3",
        srcjar_sha256 = "c68148e5343c7bfa8e3aafb2ebbefdadd49d8518612f8703fc4ce9cab0d93dc5",
        deps = ["@com_jcabi_jcabi_log", "@org_cactoos_cactoos"]
    )

    jvm_maven_import_external(
        name = "com_jcabi_jcabi_log",
        artifact = 'com.jcabi:jcabi-log:0.17.2',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "9f7bc8d4ea2b17287e53add1415f08dd8095543c81db82e78c65f20d23f2454b",
        srcjar_sha256 = "1f013cef943e68f1c966714cc1fbb412c8cc791f0d500f99175634546c460c81",
    )

    jvm_maven_import_external(
	    name = "org_cactoos_cactoos",
        artifact = 'org.cactoos:cactoos:0.20',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "3ac075084c5dda9ede829d74a382fbdbe29fb49a4df2abf4d79bf01d924801f7",
        srcjar_sha256 = "010aafbc1d9ef952c7c5d74e3aca589f1474e92ae0a499439a507fd65ab95775",
    )

    jvm_maven_import_external(
	    name = "junit_junit",
        artifact = 'junit:junit:4.13.1',
        fetch_sources = True,
        server_urls = server_urls,
#        artifact_sha256 = "3ac075084c5dda9ede829d74a382fbdbe29fb49a4df2abf4d79bf01d924801f7",
#        srcjar_sha256 = "010aafbc1d9ef952c7c5d74e3aca589f1474e92ae0a499439a507fd65ab95775",
    )

    jvm_maven_import_external(
	    name = "junit_junit",
        artifact = 'junit:junit:4.13.1',
        fetch_sources = True,
        server_urls = server_urls,
#        artifact_sha256 = "3ac075084c5dda9ede829d74a382fbdbe29fb49a4df2abf4d79bf01d924801f7",
#        srcjar_sha256 = "010aafbc1d9ef952c7c5d74e3aca589f1474e92ae0a499439a507fd65ab95775",
        deps = [
            "@org_hamcrest_hamcrest_core",
            "@org_hamcrest_hamcrest_library",
        ]
    )

    jvm_maven_import_external(
	    name = "org_hamcrest_hamcrest_core",
        artifact = 'org.hamcrest:hamcrest-core:1.3',
        fetch_sources = True,
        server_urls = server_urls,
#        artifact_sha256 = "3ac075084c5dda9ede829d74a382fbdbe29fb49a4df2abf4d79bf01d924801f7",
#        srcjar_sha256 = "010aafbc1d9ef952c7c5d74e3aca589f1474e92ae0a499439a507fd65ab95775",
    )


    jvm_maven_import_external(
	    name = "org_hamcrest_hamcrest_library",
        artifact = 'org.hamcrest:hamcrest-library:1.3',
        fetch_sources = True,
        server_urls = server_urls,
#        artifact_sha256 = "3ac075084c5dda9ede829d74a382fbdbe29fb49a4df2abf4d79bf01d924801f7",
#        srcjar_sha256 = "010aafbc1d9ef952c7c5d74e3aca589f1474e92ae0a499439a507fd65ab95775",
    )