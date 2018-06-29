package io.atateno.rinshan;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Log;

import org.statefulj.fsm.FSM;
import org.statefulj.fsm.Persister;
import org.statefulj.fsm.RetryException;
import org.statefulj.fsm.model.Action;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.impl.StateImpl;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

import java.util.LinkedList;
import java.util.List;

class KyokuViewModel extends ViewModel {
    static final String start = "start";
    static final String eastDiscard = "eastDiscard";
    static final String southDiscard = "southDiscard";
    static final String westDiscard = "westDiscard";
    static final String northDiscard = "northDiscard";
    static final State<KyokuViewModel> waitingForStart = new StateImpl<KyokuViewModel>("waitingForStart");
    static final State<KyokuViewModel> waitingForEast = new StateImpl<KyokuViewModel>("waitingForEast");
    static final State<KyokuViewModel> waitingForSouth = new StateImpl<KyokuViewModel>("waitingForSouth");
    static final State<KyokuViewModel> waitingForWest = new StateImpl<KyokuViewModel>("waitingForWest");
    static final State<KyokuViewModel> waitingForNorth = new StateImpl<KyokuViewModel>("waitingForNorth");

    @org.statefulj.persistence.annotations.State
    private String state;
    private MutableLiveData<String> liveState;
    private FSM<KyokuViewModel> fsm;

    public LiveData<String> getState() {
        if (liveState == null) {
            liveState = new MutableLiveData<String>();
        }
        return liveState;
    }

    class UpdateLiveData<T> implements Action<T> {
        public void execute(T stateful, String event, Object ... args) throws RetryException {
            Log.d("", args.toString());
            liveState.setValue("stuff");
        }
    }

    private void initStateMachine() {
        waitingForStart.addTransition(start, waitingForEast, new UpdateLiveData<KyokuViewModel>());
        waitingForEast.addTransition(eastDiscard, waitingForSouth, new UpdateLiveData<KyokuViewModel>());
        waitingForEast.addTransition(southDiscard, waitingForWest, new UpdateLiveData<KyokuViewModel>());
        waitingForEast.addTransition(westDiscard, waitingForNorth, new UpdateLiveData<KyokuViewModel>());
        waitingForEast.addTransition(northDiscard, waitingForEast, new UpdateLiveData<KyokuViewModel>());
        waitingForSouth.addTransition(eastDiscard, waitingForSouth, new UpdateLiveData<KyokuViewModel>());
        waitingForSouth.addTransition(southDiscard, waitingForWest, new UpdateLiveData<KyokuViewModel>());
        waitingForSouth.addTransition(westDiscard, waitingForNorth, new UpdateLiveData<KyokuViewModel>());
        waitingForSouth.addTransition(northDiscard, waitingForEast, new UpdateLiveData<KyokuViewModel>());
        waitingForWest.addTransition(eastDiscard, waitingForSouth, new UpdateLiveData<KyokuViewModel>());
        waitingForWest.addTransition(southDiscard, waitingForWest, new UpdateLiveData<KyokuViewModel>());
        waitingForWest.addTransition(westDiscard, waitingForNorth, new UpdateLiveData<KyokuViewModel>());
        waitingForWest.addTransition(northDiscard, waitingForEast, new UpdateLiveData<KyokuViewModel>());
        waitingForNorth.addTransition(eastDiscard, waitingForSouth, new UpdateLiveData<KyokuViewModel>());
        waitingForNorth.addTransition(southDiscard, waitingForWest, new UpdateLiveData<KyokuViewModel>());
        waitingForNorth.addTransition(westDiscard, waitingForNorth, new UpdateLiveData<KyokuViewModel>());
        waitingForNorth.addTransition(northDiscard, waitingForEast, new UpdateLiveData<KyokuViewModel>());

        List<State<KyokuViewModel>> states = new LinkedList<State<KyokuViewModel>>();
        states.add(waitingForStart);
        states.add(waitingForEast);
        states.add(waitingForSouth);
        states.add(waitingForWest);
        states.add(waitingForNorth);

        Persister<KyokuViewModel> persister = new MemoryPersisterImpl<KyokuViewModel>(states, waitingForStart);
        fsm = new FSM<KyokuViewModel>("KyokuViewModel", persister);
    }

    public void init() {
        initStateMachine();
    }
}
