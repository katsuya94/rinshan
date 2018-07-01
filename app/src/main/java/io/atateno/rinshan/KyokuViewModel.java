package io.atateno.rinshan;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

class KyokuViewModel extends ViewModel {
    public enum Directions {
        EAST,
        SOUTH,
        WEST,
        NORTH,
    }

    public enum States {
        WAITING_FOR_START,
        WAITING_FOR_EAST,
        WAITING_FOR_SOUTH,
        WAITING_FOR_WEST,
        WAITING_FOR_NORTH,
    }

    private Duration extraTime;
    private Duration baseTime;
    private Duration time;

    private Duration eastTime;
    private Duration southTime;
    private Duration westTime;
    private Duration northTime;

    private Instant scheduledAt;
    private Timer timer;

    private MutableLiveData<States> state;
    private MutableLiveData<Duration> displayTime;

    private Duration getCurrentSeatTime() {
       switch (state.getValue()) {
           case WAITING_FOR_EAST:
               return eastTime;
           case WAITING_FOR_SOUTH:
               return southTime;
           case WAITING_FOR_WEST:
               return westTime;
           case WAITING_FOR_NORTH:
               return northTime;
       }
       return null;
    }

    private void setDisplayTime() {
        if (time.compareTo(extraTime) < 0) {
            displayTime.setValue(time);
        } else {
            displayTime.setValue(null);
        }
    }

    private void scheduleTick(Duration duration) {
        scheduledAt = Instant.now();
        timer.cancel();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                elapsed
                time = time.minus(duration);
            }
        }, duration.toMillis());
    }

    private void resetTime() {
        timer.cancel();
        time = getCurrentSeatTime() + baseTime;
        setDisplayTime();
        if (time.compareTo(extraTime) < 0) {
            timer.schedule(() -> {

            }, time.withSeconds(0).toMillis());
        }
    }

    public void init() {
        extraTime = Duration.ofSeconds(30);
        baseTime = Duration.ofSeconds(10);

        eastTime = extraTime;
        southTime = extraTime;
        westTime = extraTime;
        northTime = extraTime;

        timer = new Timer();

        state = new MutableLiveData<States>();
        state.setValue(States.WAITING_FOR_START);

        displayTime = new MutableLiveData<Duration>();
    }

    public LiveData<States> getState() {
        return state;
    }

    public LiveData<Duration> getDisplayTime() {
        return displayTime;
    }

    public void start() {
        state.setValue(States.WAITING_FOR_EAST);
    }

    public void eastDiscard() {
        state.setValue(States.WAITING_FOR_SOUTH);
        resetTime();
    }

    public void southDiscard() {
        state.setValue(States.WAITING_FOR_WEST);
        resetTime();
    }

    public void westDiscard() {
        state.setValue(States.WAITING_FOR_NORTH);
        resetTime();
    }

    public void northDiscard() {
        state.setValue(States.WAITING_FOR_EAST);
        resetTime();
    }
}
