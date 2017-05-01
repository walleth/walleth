package org.ligi.walleth.ui

import android.content.Context
import android.util.AttributeSet
import kotlinx.android.synthetic.main.value.view.*

class ValueViewSmall(context: Context, attrs: AttributeSet) : ValueView(context, attrs) {

    override fun onFinishInflate() {
        super.onFinishInflate()
        current_eth.textSize = 16f
        current_eth_symbol.textSize = 16f

        current_fiat.textSize =  14f
        current_fiat_symbol.textSize =  14f
    }

}
