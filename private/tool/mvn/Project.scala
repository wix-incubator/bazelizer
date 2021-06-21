package mvn

import mvn.Project.ModelDirective
import org.apache.maven.model.io.xpp3.{MavenXpp3Reader, MavenXpp3Writer}
import org.apache.maven.model.{Dependency, Model}
import org.codehaus.plexus.util.dag.{DAG, TopologicalSorter}

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.Path
import scala.annotation.meta.{beanGetter, beanSetter}
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Try

class Project(file: File) {

  private lazy val model: Model = {
    val in = new FileInputStream(file)
    try {
      new MavenXpp3Reader().read(new FileInputStream(file))
    } finally {
      in.close()
    }
  }

  /**
   * Project ID.
   */
  lazy val id: String = file.toPath
    .toAbsolutePath.toString

  private def parentRelativePath: Option[String] = {
    Option(model.getParent).map(_.getRelativePath)
  }

  /**
   * Parent project ID.
   */
  lazy val parentId: Option[String] = for {
    aParent <- parentRelativePath
  } yield {
    file.toPath.getParent.resolve(aParent).normalize.toAbsolutePath.toString
  }

  /**
   * Parent project pom.xml file.
   */
  lazy val parentPath: Option[Path] = {
    for {
      aParent <- parentRelativePath
    } yield {
      file.toPath.getParent.resolve(aParent).normalize
    }
  }

  /**
   * Parent project.
   *
   * @return
   */
  def parentProject: Option[Project] = {
    parentPath.map(d => new Project(d.toFile))
  }

  /**
   * Emit pom file
   *
   * @return
   */
  def emitPom(dirs: ModelDirective*): Try[File] = Try {
    val p = file.getParentFile.toPath.resolve("pom.bazelizer.__gen__.xml")
    val aFile = p.toFile
    if (aFile.exists()) {
      aFile
    } else {
      val newModel = model.clone()
      dirs.foreach(_.apply(newModel))
      writePomFile(aFile, newModel)

      println(aFile)
      aFile
    }
  }

  private def writePomFile(aFile: File, newModel: Model): Unit = {
    val out = new FileOutputStream(aFile)
    try {
      new MavenXpp3Writer().write(new FileOutputStream(aFile), newModel)
    } finally {
      out.close()
    }
  }


  override def toString: String = s"Pom(${model.getId})"
}


object Project {

  //noinspection VarCouldBeVal
  private class ProjectDefStruct {
    @beanGetter
    @beanSetter
    var file: Path = _
  }

  /**
   * From json.
   *
   * @param line a json line
   * @return Project
   */
  def apply(line: String): Project = {
    val item = JSONLSupport.GSON.fromJson(line, classOf[ProjectDefStruct])
    new Project(
      item.file.toFile)
  }

  trait ModelDirective {
    def apply(m: Model): Unit
  }

  object ModelDirective {
    case class Deps(deps: Seq[Dep]) extends ModelDirective {
      override def apply(model: Model): Unit = {
        for (d <- deps) {
          val dependency = new Dependency
          dependency.setGroupId(d.gid)
          dependency.setArtifactId(d.aid)
          dependency.setVersion(d.v)
          model.addDependency(dependency)
        }
      }
    }
  }


  /**
   * Topology sort.
   *
   * @param items projects
   * @return
   */
  case class TopologySorted(private val items: Seq[Project]) extends Iterable[Project] {
    private val dag = new DAG()
    private val vertices = mutable.Map.empty[String, Project]

    for (project <- items) {
      addVertex(project)
    }

    for (project <- items) {
      project.parentId match {
        case None =>
        case Some(pId) => dag.addEdge(project.id, pId)
      }
    }

    override def iterator: Iterator[Project] = {
      val labels = TopologicalSorter.sort(dag)
      labels.asScala.map(vertices).iterator
    }

    @tailrec
    private def addVertex(aProject: Project): Unit = {
      val projectId = aProject.id
      dag.addVertex(projectId)

      if (!vertices.contains(projectId))
        vertices.update(projectId, aProject)

      aProject.parentProject match {
        case None => ()
        case Some(value) => addVertex(value)
      }
    }
  }

}
