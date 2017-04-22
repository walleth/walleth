package org.ligi.walleth.data

import org.ligi.walleth.ui.ChangeObserver

interface Observeable {

    fun registerChangeObserver(changeObserver: ChangeObserver): Unit

    fun registerChangeObserverWithInitialObservation(changeObserver: ChangeObserver)

    fun unRegisterChangeObserver(changeObserver: ChangeObserver)

}