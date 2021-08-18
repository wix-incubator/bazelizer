load("@bazel_tools//tools/build_defs/repo:jvm.bzl", _jvm_maven_import_external = "jvm_maven_import_external")
load("//third_party:maven_binaries.bzl", register_maven_tooling = "register")

_repositories = [
    "https://repo.maven.apache.org/maven2/",
    "https://mvnrepository.com/artifact",
    "https://maven-central.storage.googleapis.com",
    "http://gitblit.github.io/gitblit-maven",
]

def dependency(name, sha256, artifact, sha256_src=None):
    return struct(
        name = name,
        artifact = artifact,
        jar_sha256 = sha256,
        srcjar_sha256 = sha256_src,
    )

deps = [
	dependency(
	    name="com_github_spullara_mustache_java_compiler",
	    sha256="2b5a9217811cb99846a473fa8e0d233eb33629347b7f44941f6c0fbd4cdf1038",
	    sha256_src="21dfdc4c878342bbf0218923aeb77f4e562fd0337da47b504445623648b620ac",
	    artifact="com.github.spullara.mustache.java:compiler:0.9.10",
	),
	dependency(
	    name="com_google_code_findbugs_jsr305",
	    sha256="766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7",
	    sha256_src="1c9e85e272d0708c6a591dc74828c71603053b48cc75ae83cce56912a2aa063b",
	    artifact="com.google.code.findbugs:jsr305:3.0.2",
	),
	dependency(
	    name="com_google_code_gson_gson",
	    sha256="67bd19c510ed227d8a9dc5f67f1b4b2f3426853f5eff02e1d9ea7e95f4923ba0",
	    sha256_src="fb7a69d6a4757a9f1437832375ff626b2f9a939591ca65f1940a9052d52d48cc",
	    artifact="com.google.code.gson:gson:2.8.7",
	),
	dependency(
	    name="com_google_errorprone_error_prone_annotations",
	    sha256="baf7d6ea97ce606c53e11b6854ba5f2ce7ef5c24dddf0afa18d1260bd25b002c",
	    sha256_src="0b1011d1e2ea2eab35a545cffd1cff3877f131134c8020885e8eaf60a7d72f91",
	    artifact="com.google.errorprone:error_prone_annotations:2.3.4",
	),
	dependency(
	    name="com_google_guava_failureaccess",
	    sha256="a171ee4c734dd2da837e4b16be9df4661afab72a41adaf31eb84dfdaf936ca26",
	    sha256_src="092346eebbb1657b51aa7485a246bf602bb464cc0b0e2e1c7e7201fadce1e98f",
	    artifact="com.google.guava:failureaccess:1.0.1",
	),
	dependency(
	    name="com_google_guava_guava",
	    sha256="b22c5fb66d61e7b9522531d04b2f915b5158e80aa0b40ee7282c8bfb07b0da25",
	    sha256_src="cfcbe29dd5125f5b360370b4ecd7f7ef44fba68f4f037e90bce7315682afc0ea",
	    artifact="com.google.guava:guava:29.0-jre",
	),
	dependency(
	    name="com_google_guava_listenablefuture",
	    sha256="b372a037d4230aa57fbeffdef30fd6123f9c0c2db85d0aced00c91b974f33f99",

	    artifact="com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
	),
	dependency(
	    name="com_google_j2objc_j2objc_annotations",
	    sha256="21af30c92267bd6122c0e0b4d20cccb6641a37eaf956c6540ec471d584e64a7b",
	    sha256_src="ba4df669fec153fa4cd0ef8d02c6d3ef0702b7ac4cabe080facf3b6e490bb972",
	    artifact="com.google.j2objc:j2objc-annotations:1.3",
	),
	dependency(
	    name="commons_io_commons_io",
	    sha256="a10418348d234968600ccb1d988efcbbd08716e1d96936ccc1880e7d22513474",
	    sha256_src="3b69b518d9a844732e35509b79e499fca63a960ee4301b1c96dc32e87f3f60a1",
	    artifact="commons-io:commons-io:2.5",
	),
	dependency(
	    name="info_picocli_picocli",
	    sha256="2a6e03310db149f8a11eb058aa78e775c229ef816333c9687379762d22833ad6",
	    sha256_src="550bf73744f4a1621a9f4ab993b09cc91a4530c39c556b09ac9fc02908164d2e",
	    artifact="info.picocli:picocli:4.6.1",
	),
	dependency(
	    name="org_apache_commons_commons_compress",
	    sha256="0aeb625c948c697ea7b205156e112363b59ed5e2551212cd4e460bdb72c7c06e",
	    sha256_src="0eb5d7f270c2fccdab31daa5f7091b038ad0099b29885040604d66e07fd46a18",
	    artifact="org.apache.commons:commons-compress:1.20",
	),
	dependency(
	    name="org_apache_maven_shared_maven_invoker",
	    sha256="d20e5d26c19c04199c73fd4f0b6caebf4bbdc6b872a4504c5e71a192751d9464",
	    sha256_src="94b766c063bf6345faa571ada4bd19fd4804ff2354ea6779d5894b3e37c7afa7",
	    artifact="org.apache.maven.shared:maven-invoker:3.0.1",
	),
	dependency(
	    name="org_apache_maven_shared_maven_shared_utils",
	    sha256="3ba9c619893c767db0f9c3e826d5118b57c35229301bcd16d865a89cec16a7e5",
	    sha256_src="25064c72c178a98335048d0f7c3e08839e949426bc92bf905ea964146235f388",
	    artifact="org.apache.maven.shared:maven-shared-utils:3.2.1",
	),
	dependency(
	    name="org_apache_maven_maven_model",
	    sha256="f4ada31d7217efc11d2264dec3716623cefa3440cfb2b6b1dcc640a825159a7d",
	    sha256_src="52fd635079b9d7669157f346b3ec662e695ebeeaa880a3c0bfb570bd1dc0f055",
	    artifact="org.apache.maven:maven-model:3.6.2",
	),
	dependency(
	    name="org_checkerframework_checker_qual",
	    sha256="015224a4b1dc6de6da053273d4da7d39cfea20e63038169fc45ac0d1dc9c5938",
	    sha256_src="7d3b990687be9b980a9dc7853f4b0f279eb437e28efe3c9903acaf20450f55b5",
	    artifact="org.checkerframework:checker-qual:2.11.1",
	),
	dependency(
	    name="org_codehaus_plexus_plexus_component_annotations",
	    sha256="a7fee9435db716bff593e9fb5622bcf9f25e527196485929b0cd4065c43e61df",
	    sha256_src="18999359e8c1c5eb1f17a06093ceffc21f84b62b4ee0d9ab82f2e10d11049a78",
	    artifact="org.codehaus.plexus:plexus-component-annotations:1.7.1",
	),
	dependency(
	    name="org_codehaus_plexus_plexus_utils",
	    sha256="8d07b497bb8deb167ee5329cae87ef2043833bf52e4f15a5a9379cec447a5b2b",
	    sha256_src="f21bd1c5f6fe712f55f28e67540a60040875424d162a15608bda0a1b46aac1e4",
	    artifact="org.codehaus.plexus:plexus-utils:3.2.1",
	),
	dependency(
	    name="org_projectlombok_lombok",
	    sha256="f13db210efa2092a58bb7befce58ffa502e5fefc5e1099f45007074008756bc0",
	    sha256_src="48922643ca1f975dd315176461fcabf7b2f440891e4cebbfd123aa0bb8d50129",
	    artifact="org.projectlombok:lombok:1.18.2",
	),
#    dependency(
#	    name="com_google_collections_google_collections",
#	    sha256="81b8d638af0083c4b877099d56aa0fee714485cd2ace1b6a09cab867cadb375d",
#	    sha256_src="dbb1a31cbbbaf5596cd7431a551cada2c329bba53b2f76900af35ab17d307f21",
#	    artifact="com.google.collections:google-collections:1.0",
#	),
#	dependency(
#	    name="commons_logging_commons_logging_api",
#	    sha256="33a4dd47bb4764e4eb3692d86386d17a0d9827f4f4bb0f70121efab6bc03ba35",
#
#	    artifact="commons-logging:commons-logging-api:1.1",
#	),
#	dependency(
#	    name="log4j_log4j",
#	    sha256="dc67378cf428c06408e7959e83bdc1518dd22ccd313e7c28a986612d65c276c7",
#
#	    artifact="log4j:log4j:1.2.12",
#	),
#	dependency(
#	    name="org_apache_maven_wagon_wagon_provider_api",
#	    sha256="e116f32edcb77067289a3148143f2c0c97b27cf9a1342f8108ee37dec4868861",
#	    sha256_src="b6d1e11b976d085d6796ccb11cca3c68e7857786294e8298760f8673e5c9c9ac",
#	    artifact="org.apache.maven.wagon:wagon-provider-api:1.0-beta-6",
#	),
#	dependency(
#	    name="org_apache_xbean_xbean_reflect",
#	    sha256="17e0efa187127034623197fb88c50c30d3baa62baa0f07d6ec693047ac92ec3b",
#	    sha256_src="1e6f89e8c5fc05b15a2def9b1414cac7e8c01e0b3dc25feece2bef4b67ef4de1",
#	    artifact="org.apache.xbean:xbean-reflect:3.4",
#	),
#	dependency(
#	    name="org_codehaus_plexus_plexus_classworlds",
#	    sha256="13a90763640e445ffa432ce9586e416572645c3ed4db6a860fe0d28256ad40ce",
#	    sha256_src="178bab3d2b950fd868854e207730429c7f1228932dbf86589313bc05bd820d6e",
#	    artifact="org.codehaus.plexus:plexus-classworlds:2.2.2",
#	),
#	dependency(
#	    name="org_codehaus_plexus_plexus_container_default",
#	    sha256="69197486cd80beb54b4e0fcabaa325ec2d4e2636e9b245c472435c87a10931cf",
#	    sha256_src="8cb58c40e61a3b40aa22802e7d00007f08dd4f58111f0f46e7515d3736c0cb54",
#	    artifact="org.codehaus.plexus:plexus-container-default:1.5.5",
#	),
#	dependency(
#	    name="org_codehaus_plexus_plexus_utils",
#	    sha256="7f87dd7347790763279191081f785ea626531fd6084a18e598f143c20d0e83f8",
#	    sha256_src="8a1b5c2d7ddac87c695adc7a3af968cfc0f25b7800ade400146f693b4d46d260",
#	    artifact="org.codehaus.plexus:plexus-utils:1.4.5",
#	),
#	dependency(
#	    name="org_sonatype_aether_aether_api",
#	    sha256="1c5c5ac5e8f29aefc8faa051ffa14eccd85b9e20f4bb35dc82fba7d5da50d326",
#	    sha256_src="462102b604877aecfdf8245bcc99e4077ce310067dc40370dabee3580af6cc5e",
#	    artifact="org.sonatype.aether:aether-api:1.7",
#	),
#	dependency(
#	    name="org_sonatype_aether_aether_connector_file",
#	    sha256="da3b17e1f457988a06205aeef10958ab210fdfae688040c0f3a68dded75768e4",
#	    sha256_src="e5ce8bdb847186da7eeff965a8b0a4ef8683e0705b33f6eced75cfa9ee6570e2",
#	    artifact="org.sonatype.aether:aether-connector-file:1.7",
#	),
#	dependency(
#	    name="org_sonatype_aether_aether_connector_wagon",
#	    sha256="18e9fabbc669ef6167df0a6c0e4fce6f50d782973a84e5631162744f10ffddd6",
#	    sha256_src="a54f560bb21b0f9f266aef4ff1424f097a7f0fee12fee6d057403c4d6493c089",
#	    artifact="org.sonatype.aether:aether-connector-wagon:1.7",
#	),
#	dependency(
#	    name="org_sonatype_aether_aether_impl",
#	    sha256="288149850d8d131763df4151f7e443fd2739e48510a6e4cfe49ca082c76130fa",
#	    sha256_src="6b76f05b5a3670ab24f2d76a9a2ba8ac3fdff122132e00a4578865bf7c1f1198",
#	    artifact="org.sonatype.aether:aether-impl:1.7",
#	),
#	dependency(
#	    name="org_sonatype_aether_aether_spi",
#	    sha256="f54a0a28ce3d62af0e1cfe41dde616f645c28e452e77f77b78bc36e74d5e1a69",
#	    sha256_src="36f4a7c85dbb8f9f3c4c51633542beee7631be2b4421cae7a78516cf29a03594",
#	    artifact="org.sonatype.aether:aether-spi:1.7",
#	),
#	dependency(
#	    name="org_sonatype_aether_aether_util",
#	    sha256="ff690ffc550b7ada3a4b79ef4ca89bf002b24f43a13a35d10195c3bba63d7654",
#	    sha256_src="fdf333cff5ba996be24ca96410a489a681f8a9a1082eeb34be10b21599164456",
#	    artifact="org.sonatype.aether:aether-util:1.7",
#	)
]

def dependencies(urls=_repositories):
    for dep in deps:
        name = dep.name
        actual_name = "bazelizer_external__%s" % name

        _jvm_maven_import_external(
            name = actual_name,
            artifact = dep.artifact,
            artifact_sha256 = dep.jar_sha256,
            srcjar_sha256 = dep.srcjar_sha256,
            server_urls = urls
        )

        native.bind(
            name = "wix_incubator_bazelizer_rules/dependency/%s" % (name),
            actual = "@%s" % (actual_name)
        )

    register_maven_tooling()
