package org.walleth.transactions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_change_action.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kethereum.abi.getAllFunctions
import org.kethereum.abi.model.EthereumFunction
import org.kethereum.abi.model.EthereumNamedType
import org.kethereum.erc55.isValid
import org.kethereum.erc681.generateURL
import org.kethereum.metadata.repo.model.MetaDataRepo
import org.kethereum.metadata.repo.model.MetaDataResolveResultOK
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.EXTRA_KEY_ERC681

class ChangeActionParametersActivity : BaseSubActivity() {

    private val metaDataRepo: MetaDataRepo by inject()
    private var currentFunction: EthereumFunction? = null
    private var currentInputs: MutableList<View> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_change_action)

        lifecycleScope.launch {
            val res = metaDataRepo.getMetaDataForAddressOnChain(Address(erc681.address!!), erc681.chainId!!)
            lifecycleScope.launch(Dispatchers.Main) {
                if (res !is MetaDataResolveResultOK) {
                    alert("MetaData not found")
                } else {
                    currentFunction = res.data.output.abi.getAllFunctions().find { it.name == erc681.function }
                    supportActionBar?.subtitle = "Edit Action " + currentFunction?.name

                    currentInputs = mutableListOf()
                    currentFunction?.inputs?.forEach {
                        val input = getViewForType(it)
                        currentInputs.add(input)
                        action_container.addView(input)
                    }
                }
            }
        }

        fab.setOnClickListener {
            var hadErrorWileGeneratingFunctionParams = false

            val generatedFunctionParams = currentFunction?.inputs?.mapIndexed { index, ethereumNamedType ->
                val type = ethereumNamedType.type
                ethereumNamedType.type to when {
                    type == "string" || type.startsWith("uint") || type.startsWith("int") -> (currentInputs[index] as EditText).text.toString()
                    type.startsWith("address") -> {
                        val address = Address((currentInputs[index] as EditText).text.toString())
                        if (address.isValid()) {
                            (currentInputs[index] as EditText).error = null
                        } else {
                            (currentInputs[index] as EditText).error = "Not a valid address"
                            hadErrorWileGeneratingFunctionParams = true
                        }

                        address.hex
                    }
                    type == "bool" -> (currentInputs[index] as CheckBox).isChecked.toString()
                    else -> throw(IllegalArgumentException("Invalid type " + ethereumNamedType.type))
                }
            }

            if (!hadErrorWileGeneratingFunctionParams) {
                val intent = Intent().putExtra(EXTRA_KEY_ERC681, erc681.apply {
                    function = currentFunction?.name
                    functionParams = generatedFunctionParams ?: emptyList()
                }.generateURL())

                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun getViewForType(it: EthereumNamedType): TextView {
        return when {
            it.type == "string" -> EditText(this).apply { hint = it.name + " (string)" }
            it.type == "address" -> EditText(this).apply { hint = it.name + " (address)" }
            it.type == "bytes32" -> EditText(this).apply { hint = it.name + " (bytes32)" }
            it.type == "bool" -> CheckBox(this).apply { hint = it.name }
            it.type.startsWith("uint") -> EditText(this).apply {
                hint = it.name
                inputType = TYPE_CLASS_NUMBER
            }
            it.type.startsWith("int") -> EditText(this).apply {
                hint = it.name
                inputType = TYPE_CLASS_NUMBER + TYPE_NUMBER_FLAG_SIGNED
            }
            else -> TextView(this).apply { text = "Cannot process type type " + it.type }
        }
    }

}