package org.walleth.data

import org.walleth.ui.ChangeObserver

interface Observeable {

    fun registerChangeObserver(changeObserver: ChangeObserver): Unit

    fun registerChangeObserverWithInitialObservation(changeObserver: ChangeObserver)

    fun unRegisterChangeObserver(changeObserver: ChangeObserver)

}