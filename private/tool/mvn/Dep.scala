package mvn

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}

import com.google.common.hash.Hashing

case class Dep(gid: String, aid: String, v: String, file: Option[Path] = None)


object Dep {

  //noinspection UnstableApiUsage
  def hashed(p: Path): Dep = {
    val filePath = p.toAbsolutePath.toString
    val hash = Hashing.murmur3_128.hashString(filePath, StandardCharsets.UTF_8).toString
    val groupId = "io.bazelbuild." + hash
    val artifactId = p.getFileName.toString.replace("/", "_")
      .replace("=", "_").replace(".jar", "")
    val version = "rev-" + hash.substring(0, 7)
    Dep(groupId, artifactId, version, Some(p))
  }

}