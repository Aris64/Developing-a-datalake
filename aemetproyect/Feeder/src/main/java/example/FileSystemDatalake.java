package example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;



public class FileSystemDatalake implements Datalake{

    private final File datalakeRoot;
    LocalDate today = LocalDate.now(); //para ver el momento actual de ejecucion del programa
    LocalDate yesterday = today.minusDays(1); //al ser 24 horas atras los eventos, esto para tener el dia anterior

    public FileSystemDatalake(File datalakeRoot) {
        this.datalakeRoot = datalakeRoot;
        if (!this.datalakeRoot.mkdirs()){
            System.out.println("Se ha creado correctamente");
        }else{
            System.out.println("No hay archivo");
        }
    }

    @Override //para leer la hora del ultimo evento registrado en el dia actual de la ejecucion
    public int readtoday(LocalDate date, File filetoday) throws IOException {
        List<String> totalhours = new ArrayList<>();
        String[] event;
        int lasthour;
        try (BufferedReader reader = new BufferedReader(new FileReader(filetoday))) {
            String line;
            while ((line = reader.readLine()) != null) {
                event = line.split("\n");
                for (String Event : event) {
                    String hour = Event.substring(18, 20);
                    totalhours.add(hour);
                }
            }
            int lastline = totalhours.size() - 1;
            lasthour = Integer.parseInt(totalhours.get(lastline));
        }
        return lasthour;
    }

    @Override //para leer la hora del ultimo evento registrado en el dia previo a la ejecucion
    public int readyesterday(LocalDate date, File fileyesterday) throws IOException {
        List<String> totalhours = new ArrayList<>();
        String[] event;
        int lasthour;
        try (BufferedReader reader = new BufferedReader(new FileReader(fileyesterday))) {
            String line;
            while ((line = reader.readLine()) != null) {
                event = line.split("\n");
                for (String Event : event) {
                    String hour = Event.substring(18, 20);
                    totalhours.add(hour);
                }
            }
            int lastline = totalhours.size() - 1;
            lasthour = Integer.parseInt(totalhours.get(lastline));
        }
        return lasthour;
    }

    @Override //Para escribir
    public void store(LocalDate date, List<Event> events) throws IOException {
        System.out.println("CREANDO EVENTOS...");
        //para lo del maldito instant y su necesidad de un typeadapter
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Instant.class, new InstantSerializer()); //serializamos el instant
        Gson gson = builder.create();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String filenametoday = today.format(formatter) + ".events";
        String filenameyesterday = yesterday.format(formatter) + ".events";
        File filetoday = new File(datalakeRoot, filenametoday);
        File fileyesterday = new File(datalakeRoot, filenameyesterday);

        //incializamos variables para la primera vez de la ejecucion no se queden sin nigun valor y las condiciones no se puedan cumplir
        int latestTimestamptoday = 0;
        int latestTimestampyesterday = 0;
        try {
            latestTimestamptoday = readtoday(date, filetoday);
            latestTimestampyesterday = readyesterday(date, fileyesterday);
        } catch (IOException e) {
        } finally { //para que siga la ejecucion del programa porque la primera vez no existira ningun evento en los ficheros .events
            for (Event event : events) {

                Instant time = event.getTs(); //conseguir timestamp del evento
                ZoneId zone = ZoneId.of("Atlantic/Canary"); //para ubicar su zona horaria (canarias)
                LocalTime localtime = time.atZone(zone).toLocalTime(); //lo convertimos a localdate para extraer el dia
                int hour = localtime.getHour();

                LocalDate localdate = time.atZone(zone).toLocalDate();
                int day = localdate.getDayOfMonth();

                if (hour > latestTimestampyesterday && day < today.getDayOfMonth()) { //esta funcion se utiliza para cuando lee una fecha
                    try {
                        int actualday = today.getDayOfMonth();

                        //el dia del evento analizado
                        Instant eventtime = event.getTs(); //conseguir timestamp del evento
                        ZoneId zoneId = ZoneId.of("Atlantic/Canary"); //para ubicar su zona horaria (canarias)
                        LocalDate localDate = eventtime.atZone(zoneId).toLocalDate(); //lo convertimos a localdate para extraer el dia
                        int eventday = localDate.getDayOfMonth(); //dia de el evento

                        // escribe json en el archivo
                        String json = gson.toJson(event);
                            if (actualday == eventday) { //esta condicion valorara en que fichero escribir el evento segun su dia
                                FileWriter writertoday = new FileWriter(filetoday, true);
                                writertoday.write(json + "\n");
                                writertoday.close();
                            } else {
                                FileWriter writeryesterday = new FileWriter(fileyesterday, true);
                                writeryesterday.write(json + "\n");
                                writeryesterday.close();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                    }
                } else {
                    if (hour > latestTimestamptoday && day >= today.getDayOfMonth()) {
                        try {
                            //el dia de ejecucion de programa
                            int actualday = today.getDayOfMonth();

                            //el dia del evento analizado
                            Instant eventtime = event.getTs(); //conseguir timestamp del evento
                            ZoneId zoneId = ZoneId.of("Atlantic/Canary"); //para ubicar su zona horaria (canarias)
                            LocalDate localDate = eventtime.atZone(zoneId).toLocalDate(); //lo convertimos a localdate para extraer el dia
                            int eventday = localDate.getDayOfMonth(); //dia de el evento

                            // escribe json en el archivo
                            String json = gson.toJson(event);

                            if (actualday == eventday) { //esta condicion valorara en que fichero escribir el evento segun su dia
                                FileWriter writertoday = new FileWriter(filetoday, true);
                                writertoday.write(json + "\n");
                                writertoday.close();
                            } else {
                                FileWriter writeryesterday = new FileWriter(fileyesterday, true);
                                writeryesterday.write(json + "\n");
                                writeryesterday.close();
                            }
                        } catch (IOException e) {
                            System.out.println("Error al escribir en el archivo");
                        }
                    }
                }
            }
        }
    }
}





