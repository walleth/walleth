package org.walleth.testdata

import org.walleth.data.BalanceProvider

class BalanceProviderWithResetFun : BalanceProvider() {

    fun reset() {
        balanceMap.clear()
    }
}
