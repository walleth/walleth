package org.ligi.ewallet.data

data class Currency(val name: String, val symbol: String? = null) {
    override fun toString(): String {
        if (symbol==null) {
            return name
        }
        return "$name ($symbol)"
    }
}
