package example;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.Calendar.HOUR;

public class Controller {
    private final Loader loader;
    private final Datalake datalake;
    private boolean isRunning = true;

    public Controller(Loader loader, Datalake datalake) {
        this.loader = loader;
        this.datalake = datalake;
    }

    public void start() {
        schedule(task());
    }

    private TimerTask task() {
        return new TimerTask() {
            @Override
            public void run() {
                if(!isRunning){
                    return;
                }
                try {
                    datalake.store(LocalDate.now(), loader.load());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }


    private void schedule(TimerTask task) {
        new Timer().schedule(task, 0, HOUR*60000*60);
    }


}