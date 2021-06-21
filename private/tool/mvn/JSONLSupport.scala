package mvn

import java.lang.reflect.Type
import java.nio.file.{Files, Path, Paths}
import com.google.gson.{Gson, GsonBuilder, JsonDeserializationContext, JsonDeserializer, JsonElement}

import collection.JavaConverters._
import scala.reflect.ClassTag

object JSONLSupport {

  val GSON: Gson = new GsonBuilder().setPrettyPrinting()
    .registerTypeAdapter(classOf[Path], new JsonDeserializer[Path] {
      override def deserialize(json: JsonElement, `typeOfT`: Type, context: JsonDeserializationContext): Path = {
        val asString = json.getAsString
        Paths.get(asString)
      }
    })
    .create


  def lines(f: Path): Seq[String] = {
    Files.readAllLines(f).asScala.map(l => {
      var line = l
      if (line.startsWith("'") || line.startsWith("\"")) line = line.substring(1)
      if (line.endsWith("'") || line.endsWith("\"")) line = line.substring(0, line.length - 1)
      line
    })
  }
}
