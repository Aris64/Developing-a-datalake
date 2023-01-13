package example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class AemetLoader implements Loader {
    public static final String aemetUrl = "https://opendata.aemet.es/opendata/api/observacion/convencional/todas";
    private static final String apiKey = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcmlzdmF6ZGVrYW5vNkBnbWFpbC5jb20iLCJqdGkiOiJhNjBiZWRmYy01ODAxLTRiMWQtODcyNi01NDFkY2EzZmJkYTAiLCJpc3MiOiJBRU1FVCIsImlhdCI6MTY3MTExNjE0NiwidXNlcklkIjoiYTYwYmVkZmMtNTgwMS00YjFkLTg3MjYtNTQxZGNhM2ZiZGEwIiwicm9sZSI6IiJ9.ikSzjE7E4DA6EJgfqLPAk0Ir7Utq8rqdA9iVRFkcQHE";

    @Override
    public List<Event> load(){
        try{
            String json = get(aemetUrl);
            String url = urlIn(json);
            return parse(get(url));
        } catch(Exception e){
            System.out.println("ERROR EN HACER EL PARSE(GET(URLIN(GET()))))): " + e.getMessage());
            return emptyList();
        }
    }

    private String urlIn(String json) {
        return new Gson().fromJson(json, JsonObject.class).get("datos").getAsString();
    }

    private String get(String url) throws IOException {
        System.out.println("EJECUTANDO GET... (se repetira dos veces)");
        return Jsoup.connect(url)
                .validateTLSCertificates(false)
                .timeout(16000)
                .ignoreContentType(true)
                .maxBodySize(0)
                .header("accept", "application/json")
                .header("api_key", apiKey)
                .method(Connection.Method.GET)
                .maxBodySize(0).execute().body();
    }


    private List<Event> parse(String json)  {
        JsonArray jsonArray= new Gson().fromJson(json, JsonArray.class);
        return filter (jsonArray.asList());
    }

    private List<Event> filter(List<JsonElement> list) {
        System.out.println("FILTRANDO GRAN CANARIA...");
        return list.stream()
                .map(JsonElement -> JsonElement.getAsJsonObject())
                .filter(o -> isInGranCanaria(o)) //Si esta en gran canaria
                .map(o -> fromJsonObject(o))
                .collect(toList());
    }


    private boolean isInGranCanaria(JsonObject o) { //Si lon y lat estan en canarias

        return isInGranCanaria(lonIn(o),latIn(o));
    }

    private boolean isInGranCanaria(double lon, double lat) { //Estan en canarias si cumplen esta condición
        return lon > -16 && lon < -15 && lat > 27.5 && lat < 28.4;
    }

    private double lonIn(JsonObject p) {

        return p.getAsJsonPrimitive("lon").getAsDouble();
    }

    private double latIn(JsonObject p) {

        return p.getAsJsonPrimitive("lat").getAsDouble();
    }

    public static Event fromJsonObject(JsonObject o) {
        // Obtener los valores necesarios del objeto JsonObject
        try {
            //para hacer la maldita fecha en variable tipo instant
            String dateString = o.get("fint").getAsString(); //lee la string de la fecha
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME; //crea un formato de una fecha
            LocalDateTime date = LocalDateTime.parse(dateString, formatter); //parsea la string que indica la fecha del evento con el formato de una fecha
            Instant instant = date.toInstant(ZoneOffset.UTC); //le decimos que a este parseo lo instanciemos en una variable tipo instant

            String ubi = o.get("ubi").getAsString();
            String station = o.get("idema").getAsString();
            double temp = 0.0;
            double tempmax = 0.0;
            double tempmin = 0.0;
            if (o.has("ta")) {
                temp = o.getAsJsonPrimitive("ta").getAsDouble();
            }
            if (o.has("tamax")) {
                tempmax = o.getAsJsonPrimitive("tamax").getAsDouble();
            }
            if (o.has("tamin")) {
                tempmin = o.getAsJsonPrimitive("tamin").getAsDouble();
            }
            return new Event(instant, ubi, station, temp, tempmax,tempmin );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}