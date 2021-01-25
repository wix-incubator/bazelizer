package tools.jvm.v2.mvn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Path.class, (JsonDeserializer<Path>) (json, typeOfT, context) -> {
                final String asString = json.getAsString();
                return Paths.get(asString);
            }).create();


    public static void main(String[] args) {

    }
}
