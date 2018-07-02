package io.atateno.rinshan;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

class KyokuViewModel extends ViewModel {
    interface Savable {
        void save(String marshalled);
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

    public void setExtraTime(int extraTime) {
        this.extraTime = extraTime * 1000;
    }

    public void setBaseTime(int baseTime) {
        this.baseTime = baseTime * 1000;
    }

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
    public LiveData<States> getState() {
        return state;
    }
    public LiveData<Pair<States, Integer>> getDisplay() {
        return display;
    }

    private Runnable onTick;
    public void setOnTick(Runnable onTick) {
        this.onTick = onTick;
    }

    private Savable onPause;
    public void setOnPause(Savable onPause) {
        this.onPause = onPause;
    }

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
        lastDiscardAt = System.currentTimeMillis();
        totalTime = getCurrentSeatTime() + baseTime;
        setTime(totalTime);
        scheduleTicks(firstTickAt());
        scheduleUpdate(lastDiscardAt + baseTime);
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

    public synchronized void init() {
        state = new MutableLiveData<>();
        display = new MutableLiveData<>();
        onTick = () -> {};
        onPause = (marshalled) -> {};
    }

    public synchronized void reset() {
        cancelUpdateTimer();
        cancelTickTimer();

        eastTime = extraTime;
        southTime = extraTime;
        westTime = extraTime;
        northTime = extraTime;

        state.setValue(States.WAITING_FOR_START);
        display.setValue(new Pair<>(state.getValue(), null));
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
        States currentState = state.getValue();
        if (currentState == States.WAITING_FOR_RESUME ||
                currentState == States.WAITING_FOR_START) {
            return;
        }
        cancelUpdateTimer();
        cancelTickTimer();
        pausedAt = System.currentTimeMillis();
        resumeState = currentState;
        state.setValue(States.WAITING_FOR_RESUME);
        setTime(time);
        onPause.save(marshall());
    }

    public synchronized void resume() {
        if (state.getValue() != States.WAITING_FOR_RESUME) {
            return;
        }
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
        setTime(time);
    }

    class MarshallException extends RuntimeException {
        public MarshallException(Throwable cause) {
            super(cause);
        }
    }

    public synchronized String marshall() {
        try {
            JSONObject json = new JSONObject();
            json.put("extraTime", Long.toString(extraTime));
            json.put("baseTime", Long.toString(baseTime));
            json.put("eastTime", Long.toString(eastTime));
            json.put("southTime", Long.toString(southTime));
            json.put("westTime", Long.toString(westTime));
            json.put("northTime", Long.toString(northTime));
            json.put("lastDiscardAt", Long.toString(lastDiscardAt));
            json.put("pausedAt", Long.toString(pausedAt));
            json.put("totalTime", Long.toString(totalTime));
            json.put("time", Long.toString(time));
            json.put("resumeState", resumeState.toString());
            return json.toString();
        } catch (JSONException e) {
            throw new MarshallException(e);
        }
    }

    class UnmarshallException extends RuntimeException {
        public UnmarshallException(Throwable cause) {
            super(cause);
        }
    }

    public synchronized void unmarshall(String marshalled) {
        try {
            JSONObject json = new JSONObject(marshalled);
            extraTime = Long.parseLong(json.getString("extraTime"));
            baseTime = Long.parseLong(json.getString("baseTime"));
            eastTime = Long.parseLong(json.getString("eastTime"));
            southTime = Long.parseLong(json.getString("southTime"));
            westTime = Long.parseLong(json.getString("westTime"));
            northTime = Long.parseLong(json.getString("northTime"));
            lastDiscardAt = Long.parseLong(json.getString("lastDiscardAt"));
            pausedAt = Long.parseLong(json.getString("pausedAt"));
            totalTime = Long.parseLong(json.getString("totalTime"));
            resumeState = States.valueOf(json.getString("resumeState"));
            state.setValue(States.WAITING_FOR_RESUME);
            setTime(Long.parseLong(json.getString("time")));
        } catch (JSONException e) {
            throw new UnmarshallException(e);
        }
    }
}
