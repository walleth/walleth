package org.walleth.valueview

import android.widget.EditText
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class ValueViewTextObserver(private val valueView: EditText,
                            private val viewModel: ValueViewController) : ObservableProperty<String?>(null) {
    override fun beforeChange(property: KProperty<*>,
                              oldValue: String?,
                              newValue: String?) = (!newValue.isNullOrEmpty()).also {
        if (!it && valueView.text.toString() != "0") {
            valueView.setText("0")
        }
    }


    override fun afterChange(property: KProperty<*>, oldValue: String?, newValue: String?) {

        if (oldValue != newValue) {
            if (valueView.text.toString() != newValue) {
                valueView.setText(newValue)
                viewModel.refreshNonValues()
            }
        }
    }
}