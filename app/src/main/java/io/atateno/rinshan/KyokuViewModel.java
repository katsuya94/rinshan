package io.atateno.rinshan;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Log;
import android.util.Pair;

import java.util.Date;
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
        WAITING_FOR_RESUME,
    }

    private long extraTime;
    private long baseTime;

    private long eastTime;
    private long southTime;
    private long westTime;
    private long northTime;

    private long lastDiscardAt;
    private long pausedAt;
    private long totalTime;
    private long time;

    private Timer updateTimer;
    private Timer tickTimer;

    private States resumeState;

    private MutableLiveData<States> state;
    private MutableLiveData<Pair<States, Integer>> display;

    private Runnable onTick;

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

    private long ceilTime() {
        if (time >= 0) {
            return (time + 999) / 1000;
        } else {
            return time / 1000;
        }
    }

    private void setTime(long t) {
        time = t;
        Integer integer = null;
        if (time < totalTime - baseTime) {
            integer = new Integer((int)ceilTime());
        }
        display.postValue(new Pair<>(state.getValue(), integer));
    }

    private void cancelUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }

    private void cancelTickTimer() {
        if (tickTimer != null) {
            tickTimer.cancel();
            tickTimer = null;
        }
    }


    private long elapsedTime() {
        return System.currentTimeMillis() - lastDiscardAt;
    }

    private long firstTickAt() {
        return lastDiscardAt + totalTime - ((totalTime - baseTime + 999) / 1000 - 1) * 1000;
    }

    private long nextTickAt() {
        if (time >= 0) {
            return lastDiscardAt + totalTime - ((time + 999) / 1000 - 1) * 1000;
        } else {
            return lastDiscardAt + totalTime - (time / 1000 + 1) * 1000;
        }
    }

    private synchronized void update() {
        setTime(totalTime - elapsedTime());
        scheduleUpdate(nextTickAt());
    }

    private void scheduleUpdate(long at) {
        cancelUpdateTimer();
        updateTimer = new Timer();
        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                update();
            }
        }, new Date(at));
    }

    private void scheduleTicks(long starting) {
        if (tickTimer != null) {
            tickTimer.cancel();
        }
        tickTimer = new Timer();
        tickTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTick.run();
            }
        }, new Date(starting), 1000);
    }

    private void resetTime() {
        cancelUpdateTimer();
        long currentSeatTime = getCurrentSeatTime();
        lastDiscardAt = System.currentTimeMillis();
        totalTime = getCurrentSeatTime() + baseTime;
        long firstUpdateAt = lastDiscardAt + baseTime;
        setTime(totalTime);
        scheduleTicks(firstTickAt());
        scheduleUpdate(firstUpdateAt);
    }

    private void handleDiscard(States nextState) {
        state.setValue(nextState);
        long elapsedTime = elapsedTime();
        if (elapsedTime <= baseTime) {
            return;
        }
        switch (nextState) {
            case WAITING_FOR_EAST:
                northTime -= elapsedTime - baseTime;
                break;
            case WAITING_FOR_SOUTH:
                eastTime -= elapsedTime - baseTime;
                break;
            case WAITING_FOR_WEST:
                southTime -= elapsedTime - baseTime;
                break;
            case WAITING_FOR_NORTH:
                westTime -= elapsedTime - baseTime;
                break;
        }
    }

    public synchronized void init(Runnable onTick) {
        cancelUpdateTimer();
        cancelTickTimer();

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

        this.onTick = onTick;
    }

    public LiveData<States> getState() {
        return state;
    }

    public LiveData<Pair<States, Integer>> getDisplay() {
        return display;
    }

    public synchronized void start() {
        state.setValue(States.WAITING_FOR_EAST);
        resetTime();
    }

    public synchronized void eastDiscard() {
        handleDiscard(States.WAITING_FOR_SOUTH);
        resetTime();
    }

    public synchronized void southDiscard() {
        handleDiscard(States.WAITING_FOR_WEST);
        resetTime();
    }

    public synchronized void westDiscard() {
        handleDiscard(States.WAITING_FOR_NORTH);
        resetTime();
    }

    public synchronized void northDiscard() {
        handleDiscard(States.WAITING_FOR_EAST);
        resetTime();
    }

    public synchronized void pause() {
        cancelUpdateTimer();
        cancelTickTimer();
        pausedAt = System.currentTimeMillis();
        resumeState = state.getValue();
        state.setValue(States.WAITING_FOR_RESUME);
        display.setValue(new Pair<>(States.WAITING_FOR_RESUME, display.getValue().second));
    }

    public synchronized void resume() {
        lastDiscardAt += System.currentTimeMillis() - pausedAt;
        long firstUpdateAt = lastDiscardAt + baseTime;
        if (time > totalTime - baseTime) {
            scheduleTicks(firstTickAt());
            scheduleUpdate(firstUpdateAt);
        } else {
            scheduleTicks(nextTickAt());
            scheduleUpdate(nextTickAt());
        }
        state.setValue(resumeState);
        display.setValue(new Pair<>(resumeState, display.getValue().second));
    }
}
