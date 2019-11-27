package org.walleth.valueview

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import kotlinx.android.synthetic.main.value.view.*
import org.ligi.kaxt.startActivityFromClass
import org.walleth.R
import org.walleth.preferences.reference.SelectReferenceActivity
import org.walleth.tokens.SelectTokenActivity

open class ValueView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    open val layoutRes = R.layout.value
    val showsPrecise: Boolean
    private val allowEdit: Boolean

    init {
        // extract the showPrecise value
        val a: TypedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ValueView,
                0, 0)
        try {
            showsPrecise = a.getBoolean(R.styleable.ValueView_showPrecise, true)
            allowEdit = a.getBoolean(R.styleable.ValueView_allowEdit, false)
        } finally {
            a.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(layoutRes, this, true)

        if (!allowEdit) {
            current_eth.keyListener = null
            current_fiat.keyListener = null
        }
        current_eth.isCursorVisible = allowEdit
        current_eth.isFocusableInTouchMode = allowEdit
        ViewCompat.setBackground(current_eth, null)

        current_fiat.isEnabled = allowEdit
        current_fiat.isFocusableInTouchMode = allowEdit
        ViewCompat.setBackground(current_fiat, null)

        if (showsPrecise) {
            current_token_symbol.setOnClickListener {
                context.startActivityFromClass(SelectTokenActivity::class)
            }

            current_fiat_symbol.setOnClickListener {
                context.startActivityFromClass(SelectReferenceActivity::class)
            }
        }

    }

}