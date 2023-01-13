package example;

import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.Calendar.HOUR;

public class Controller {
    public Datalake datalake;
    public SqliteWriter datamart;
    private TimerTask task;

    public Controller(FileDatalake datalake, SqliteWriter datamart) {
        this.datalake = datalake;
        this.datamart = datamart;
        task = new TimerTask() {
            @Override
            public void run() {
                try {
                    datamart.delete();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                List<Event> eventstoday = datalake.getEventsToday();
                List<Event> eventsyesterday = datalake.getEventsYesterday();
                //ahora averiguaremos el evento con la temperatura minima mas baja, tanto del dia actual como del dia anterior
                Event minEventtoday = null;
                Event minEventyesterday = null;

                for (Event i : eventstoday) {
                    if (i.getTamin() != 0.0) { //condicion necesaria ya que un sensor de temperatura de la aemet no devuelve correctamente las temperaturas, por lo que son 0.0 (precisamente el sensor C659H)
                        if (minEventtoday == null || i.getTamin() < minEventtoday.getTamin()) {
                            minEventtoday = i;
                        }
                    }
                }
                System.out.println("El evento HOY con el número menor es: " + minEventtoday + " con temperatura: " + minEventtoday.getTamin());

                // agregamos el evento al datamart
                for (Event i : eventsyesterday) {
                    if (i.getTamin() != 0.0) { //condicion necesaria ya que un sensor de temperatura de la aemet no devuelve correctamente las temperaturas, por lo que son 0.0 (precisamente el sensor C659H)
                        if (minEventyesterday == null || i.getTamin() < minEventyesterday.getTamin()) {
                            minEventyesterday = i;
                        }
                    }
                }
                System.out.println("El evento AYER con el número menor es: " + minEventyesterday + " con temperatura: " + minEventyesterday.getTamin());
                datamart.addmin( minEventtoday, minEventyesterday); // agregamos el evento al datamart

                //ahora averiguaremos el evento con la temperatura maxima mas alta, tanto de el dia actual como el dia anterior
                Event maxEventtoday = null;
                Event maxEventyesterday = null;

                for(Event i :eventstoday) {
                    if (maxEventtoday == null || i.getTamax() > maxEventtoday.getTamax()) {
                        maxEventtoday = i;
                    }
                }
                System.out.println("El evento HOY con el número mayor es: "+maxEventtoday +" con temperatura: "+maxEventtoday.getTamax());

                for(Event i :eventsyesterday) {
                    if (maxEventyesterday == null || i.getTamax() > maxEventyesterday.getTamax()) {
                        maxEventyesterday = i;
                    }
                }
                System.out.println("El evento AYER con el número mayor es: "+maxEventyesterday +" con temperatura: "+maxEventyesterday.getTamax());
                datamart.addmax(maxEventtoday, maxEventyesterday); // agregamos el evento al datamart
            }
        };
    }

    public void start() {
        Timer timer = new Timer();
        // Ejecutar tarea cada hora (3.600.000 milisegundos)
        timer.scheduleAtFixedRate(task, 0, HOUR*60000*60);
    }
}