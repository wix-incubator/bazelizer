package mvn

;

import java.io.File
import java.lang.reflect.Type
import java.nio.file.{Files, Path, Paths}

import com.google.gson._

import scala.collection.JavaConverters._
import scopt.OParser

object Cli extends App {

  case class Config(repo: Option[BuildRepoImage] = None, target: Option[BuildTarget] = None)

  case class BuildRepoImage(conf: File = null, outputTar: File = null)

  case class BuildTarget(defFile: File = null, outputTar: File = null)


  val builder = OParser.builder[Config]

  val parser1 = {
    import builder._
    OParser.sequence(
      programName("MavenMaker"),
      cmd("build-repository")
        .text("Build full local repository for all modules")
        .action((_, c) => c.copy(repo = Some(BuildRepoImage())))
        .children(
          opt[File]("config")
            .abbr("c")
            .action((f, c) => c.copy(repo = c.repo.map(_.copy(conf = f)))),
          opt[File]("output")
            .text("output tar file for the repository")
            .abbr("o")
            .action((f, c) => c.copy(repo = c.repo.map(_.copy(outputTar = f)))),
        )
    )
  }

  OParser.parse(parser1, args, Config()) match {
    case Some(Config(Some(BuildRepoImage(defFile, outFile)), _)) =>

      val env = new Env

      val projects = JSONLSupport.lines(defFile.toPath)
        .map(d => Project.apply(d))

      println(projects)

      Project.TopologySorted(projects).foreach(project => {
        env.eval(Args("dependency:go-offline", "clean", "install"), project).get
      })

    case _ =>
      sys.exit(1)
  }
}