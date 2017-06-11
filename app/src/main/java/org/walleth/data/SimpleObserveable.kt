package org.walleth.data

import org.walleth.ui.ChangeObserver

open class SimpleObserveable : Observeable {

    val lock = Any()
    val changeObserves = HashSet<ChangeObserver>()

    override fun registerChangeObserver(changeObserver: ChangeObserver) {
        synchronized(lock) {
            changeObserves.add(changeObserver)
        }
    }

    override fun registerChangeObserverWithInitialObservation(changeObserver: ChangeObserver) {
        synchronized(lock) {
            changeObserver.observeChange()
            changeObserves.add(changeObserver)
        }
    }

    override fun unRegisterChangeObserver(changeObserver: ChangeObserver) {
        synchronized(lock) {
            changeObserves.remove(changeObserver)
        }
    }

    fun promoteChange() = synchronized(lock) {
        changeObserves.forEach(ChangeObserver::observeChange)
    }

}