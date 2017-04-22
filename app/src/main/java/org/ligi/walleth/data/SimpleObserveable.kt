package org.ligi.walleth.data

import org.ligi.walleth.ui.ChangeObserver

open class SimpleObserveable : Observeable {

    val changeObserves = HashSet<ChangeObserver>()

    override fun registerChangeObserver(changeObserver: ChangeObserver) {
        changeObserves.add(changeObserver)
    }

    override fun registerChangeObserverWithInitialObservation(changeObserver: ChangeObserver) {
        changeObserver.observeChange()
        changeObserves.add(changeObserver)
    }

    override fun unRegisterChangeObserver(changeObserver: ChangeObserver) {
        changeObserves.remove(changeObserver)
    }

    fun promoteChange() = changeObserves.forEach(ChangeObserver::observeChange)

}