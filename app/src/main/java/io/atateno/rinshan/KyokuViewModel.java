package io.atateno.rinshan;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Pair;

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

    private long extraTime;
    private long baseTime;

    private long eastTime;
    private long southTime;
    private long westTime;
    private long northTime;

    private Timer timer;
    private long time;

    private MutableLiveData<States> state;
    private MutableLiveData<Pair<States, Integer>> display;

    private long getCurrentSeatTime() {
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
       return 0;
    }

    private void setTime(long time) {
        this.time = time;
        if (this.time < extraTime) {
            if (this.time >= 0) {
                display.postValue(new Pair<>(state.getValue(), new Integer((int) (this.time + 999) / 1000)));
            } else {
                display.postValue(new Pair<>(state.getValue(), new Integer((int) this.time / 1000)));
            }
        } else {
            display.postValue(new Pair<>(state.getValue(), null));
        }
    }

    private void scheduleTick(long in) {
        long scheduledAt = System.currentTimeMillis();
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - scheduledAt;
                setTime(time - elapsed);
                scheduleTick(((time % 1000) + 1000) % 1000);
            }
        }, in);
    }

    private void resetTime() {
        setTime(getCurrentSeatTime() + baseTime);
        if (time < extraTime) {
            scheduleTick(time % 1000);
        } else {
            scheduleTick(time - extraTime);
        }
    }

    public void init() {
        extraTime = 30 * 1000;
        baseTime = 10 * 1000;

        eastTime = extraTime;
        southTime = extraTime;
        westTime = extraTime;
        northTime = extraTime;

        state = new MutableLiveData<>();
        state.setValue(States.WAITING_FOR_START);

        display = new MutableLiveData<>();
        display.setValue(new Pair<>(state.getValue(), null));
    }

    public LiveData<States> getState() {
        return state;
    }

    public LiveData<Pair<States, Integer>> getDisplay() {
        return display;
    }

    public void start() {
        state.setValue(States.WAITING_FOR_EAST);
        resetTime();
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
