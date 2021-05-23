package com.ocha.prototype_final.CustomView.camera

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ocha.prototype_final.CustomView.objectDetected.DetectedObject
import com.ocha.prototype_final.CustomView.objectDetected.SearchedObject
import com.ocha.prototype_final.settings.UtilsPreference
import java.lang.NullPointerException

class WorkFlow(application: Application) : AndroidViewModel(application) {

    val workflowState = MutableLiveData<WorkflowState>()
    val objectToSearch = MutableLiveData<DetectedObject>()
    val searchedObject = MutableLiveData<SearchedObject>()

    private val obtIdsToSearch =HashSet<Int>()

    var cameraLive = false
        private set

    private val context: Context
        get() = getApplication<Application>().applicationContext

    private var confirmedObject: DetectedObject? = null

    enum class WorkflowState {
        NOT_STARTED,
        DETECTING,
        DETECTED,
        CONFIRMING,
        CONFIRMED,
        SEARCHING,
        SEARCHED
    }

    @MainThread
    fun setWorkState(workflowState: WorkflowState){
        if(workflowState != WorkflowState.CONFIRMING && workflowState != WorkflowState.SEARCHING && workflowState != WorkflowState.SEARCHED){
            confirmedObject = null
        }
        this.workflowState.value = workflowState
    }

    @MainThread
    fun confirmingObject(confirmingObject: DetectedObject, progress: Float){
        val isConfirmed = progress.compareTo(1f) == 0
        if (isConfirmed){
            confirmedObject = confirmingObject
            if (UtilsPreference.isAutoSearchEnable){
                setWorkState(WorkflowState.SEARCHING)
                triggerSearch(confirmingObject)
            }else{
                setWorkState(WorkflowState.CONFIRMED)
            }
        }else{
            setWorkState(WorkflowState.CONFIRMING)
        }
    }

    @MainThread
    fun onSearchButtonClicked(){
        confirmedObject?.let {
            setWorkState(WorkflowState.SEARCHING)
            triggerSearch(it)
        }
    }

    private fun triggerSearch(detectedObject: DetectedObject){
        val id =detectedObject.objectId ?:throw NullPointerException()
            if (obtIdsToSearch.contains(id)){
                return
            }

        obtIdsToSearch.add(id)
        objectToSearch.value = detectedObject
    }

    fun markCameraLive() {
        cameraLive = true
        obtIdsToSearch.clear()
    }

    fun markCameraFrozen() {
        cameraLive = false
    }



}