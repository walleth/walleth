package org.walleth

import org.walleth.data.BalanceProvider

class BalanceProviderWithResetFun : BalanceProvider() {

    fun reset() {
        balanceMap.clear()
    }
}
