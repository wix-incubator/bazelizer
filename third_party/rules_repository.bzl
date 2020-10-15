
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")

_def_server_urls = [
        "https://jcenter.bintray.com/",
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
]

_maven_binary_version = "3.6.3"
_maven_binary_sha256 = "26ad91d751b3a9a53087aefa743f4e16a17741d3915b219cf74112bf87a438c5"

MAVEN_BINARY_NAME = "io_bazelizer_maven_binary_tool"

def install(server_urls = _def_server_urls):

    if native.existing_rule(MAVEN_BINARY_NAME) == None:
        http_archive(
           name = MAVEN_BINARY_NAME,
           url = "https://www2.apache.paket.ua/maven/maven-3/" + _maven_binary_version + "/binaries/apache-maven-" + _maven_binary_version + "-bin.tar.gz",
           build_file_content = "\n".join([
               'filegroup(name = "' + MAVEN_BINARY_NAME + '", visibility = ["//visibility:public"],',
               '  srcs = glob(["bin/**", "boot/**", "conf/**", "lib/**"])',
               ")"
           ]),
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

    jvm_maven_import_external(
	    name = "org_projectlombok_lombok",
        artifact = 'org.projectlombok:lombok:1.18.12',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "49381508ecb02b3c173368436ef71b24c0d4418ad260e6cc98becbcf4b345406",
        srcjar_sha256 = "e673bd5c8a9e253ca32c8a40b8f433659d4bc34dae19813182eb430a79d5a4d9",
    )

    jvm_maven_import_external(
	    name = "com_google_guava_guava",
        artifact = 'com.google.guava:guava:29.0-jre',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "b22c5fb66d61e7b9522531d04b2f915b5158e80aa0b40ee7282c8bfb07b0da25",
        srcjar_sha256 = "cfcbe29dd5125f5b360370b4ecd7f7ef44fba68f4f037e90bce7315682afc0ea",
    )

    jvm_maven_import_external(
	    name = "ch_qos_logback_logback_classic",
        artifact = 'ch.qos.logback:logback-classic:1.2.3',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "fb53f8539e7fcb8f093a56e138112056ec1dc809ebb020b59d8a36a5ebac37e0",
        srcjar_sha256 = "480cb5e99519271c9256716d4be1a27054047435ff72078d9deae5c6a19f63eb",
    )

    jvm_maven_import_external(
	    name = "ch_qos_logback_logback_core",
        artifact = 'ch.qos.logback:logback-core:1.2.3',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "5946d837fe6f960c02a53eda7a6926ecc3c758bbdd69aa453ee429f858217f22",
        srcjar_sha256 = "1f69b6b638ec551d26b10feeade5a2b77abe347f9759da95022f0da9a63a9971",
    )

    jvm_maven_import_external(
	    name = "org_slf4j_slf4j_api",
        artifact = 'org.slf4j:slf4j-api:1.7.30',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "cdba07964d1bb40a0761485c6b1e8c2f8fd9eb1d19c53928ac0d7f9510105c57",
        srcjar_sha256 = "9ee459644577590fed7ea94afae781fa3cc9311d4553faee8a3219ffbd7cc386",
    )

    jvm_maven_import_external(
	    name = "commons_io_commons_io",
        artifact = 'commons-io:commons-io:2.5',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "a10418348d234968600ccb1d988efcbbd08716e1d96936ccc1880e7d22513474",
        srcjar_sha256 = "3b69b518d9a844732e35509b79e499fca63a960ee4301b1c96dc32e87f3f60a1",
    )

    jvm_maven_import_external(
	    name = "org_apache_commons_commons_compress",
        artifact = 'org.apache.commons:commons-compress:1.20',
        fetch_sources = True,
        server_urls = server_urls,
        artifact_sha256 = "0aeb625c948c697ea7b205156e112363b59ed5e2551212cd4e460bdb72c7c06e",
        srcjar_sha256 = "0eb5d7f270c2fccdab31daa5f7091b038ad0099b29885040604d66e07fd46a18",
    )