package org.walleth.transactions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_list_nofab.*
import kotlinx.android.synthetic.main.item_function.view.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.abi.getAllFunctions
import org.kethereum.abi.model.EthereumFunction
import org.kethereum.erc681.generateURL
import org.kethereum.metadata.model.EthereumMetaData
import org.kethereum.metadata.repo.model.MetaDataRepo
import org.kethereum.metadata.repo.model.MetaDataResolveResultOK
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.EXTRA_KEY_ERC681
import org.walleth.data.REQUEST_CODE_CHANGE_ACTION

suspend fun AppCompatActivity.getMetaData(address: Address, chainId: ChainId): EthereumMetaData? {
    val metaDataRepo: MetaDataRepo by inject()

    return withContext(lifecycleScope.coroutineContext) {
        val res = metaDataRepo.getMetaDataForAddressOnChain(address, chainId)
        if (res !is MetaDataResolveResultOK) {
            alert("MetaData not found") {
                finish()
            }
            null
        } else {
            res.data
        }
    }
}

class ChangeActionFunctionActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_nofab)

        recycler_view.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            getMetaData(Address(erc681.address!!), erc681.chainId!!)?.let {
                recycler_view.adapter = FunctionAdapter(it.output.abi.getAllFunctions(), onSelect = { function ->
                    val new681 = erc681.copy(function = function.name)
                    if (function.inputs.isEmpty()) {
                        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_ERC681, erc681.copy(function = function.name).generateURL()))
                        finish()
                    } else {
                        startActivityForResult(getERC681ActivityIntent(new681, ChangeActionParametersActivity::class), REQUEST_CODE_CHANGE_ACTION)
                    }
                })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setResult(resultCode, data)
        finish()
    }
}

class FunctionViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    fun bind(function: EthereumFunction, onSelect: (function: EthereumFunction) -> Unit) {
        view.function_name.text = function.name
        view.function_card.setOnClickListener { onSelect(function) }
    }
}

class FunctionAdapter(
        private val allFunctions: List<EthereumFunction>,
        private val onSelect: (function: EthereumFunction) -> Unit) : RecyclerView.Adapter<FunctionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FunctionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_function, parent, false))

    override fun getItemCount() = allFunctions.size

    override fun onBindViewHolder(holder: FunctionViewHolder, position: Int) {
        holder.bind(allFunctions[position], onSelect)
    }

}
