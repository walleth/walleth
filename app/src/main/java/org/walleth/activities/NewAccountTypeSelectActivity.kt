package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_account_type_select.*
import kotlinx.android.synthetic.main.item_account_type.view.*
import org.walleth.R
import org.walleth.data.*
import org.walleth.data.addressbook.AccountKeySpec
import org.walleth.model.ACCOUNT_TYPE_LIST
import org.walleth.model.AccountType

class AccountTypeViewHolder(itemView: View, val inSpec: AccountKeySpec) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: AccountType) {
        itemView.bitmap_type.isEnabled = false
        itemView.bitmap_type.setImageResource(item.drawable)
        itemView.account_type_label.text = item.action
        itemView.account_type_description.text = item.description
        itemView.setOnClickListener {
            item.callback.invoke(itemView.context as Activity, inSpec)
        }
    }
}

class AccountTypeAdapter(val list: List<AccountType>, val inSpec: AccountKeySpec) : RecyclerView.Adapter<AccountTypeViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = AccountTypeViewHolder(LayoutInflater.from(p0.context).inflate(R.layout.item_account_type, p0, false), inSpec)


    override fun getItemCount() = list.size

    override fun onBindViewHolder(viewHolder: AccountTypeViewHolder, p1: Int) {
        viewHolder.bind(list[p1])
    }
}

open class NewAccountTypeSelectActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_type_select)

        supportActionBar?.subtitle = "New account"

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AccountTypeAdapter(ACCOUNT_TYPE_LIST, AccountKeySpec(ACCOUNT_TYPE_NONE))

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_ENTER_PASSWORD -> {
                    val pwdExtra = data.getStringExtra(EXTRA_KEY_PWD)
                    val spec = AccountKeySpec(ACCOUNT_TYPE_PASSWORD_PROTECTED, pwd = pwdExtra)
                    setResult(resultCode, data.putExtra(EXTRA_KEY_ACCOUNTSPEC, spec))
                }
                REQUEST_CODE_ENTER_PIN -> {
                    val pinExtra = data.getStringExtra(EXTRA_KEY_PIN)
                    val spec = AccountKeySpec(ACCOUNT_TYPE_PIN_PROTECTED, pwd = pinExtra)
                    setResult(resultCode, data.putExtra(EXTRA_KEY_ACCOUNTSPEC, spec))
                }
                else -> {
                    setResult(resultCode, data)
                }
            }
            finish()


        }
    }
}