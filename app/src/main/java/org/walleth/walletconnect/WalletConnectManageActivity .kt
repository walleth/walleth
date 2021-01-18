package org.walleth.walletconnect

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import kotlinx.android.synthetic.main.activity_list_nofab.*
import kotlinx.android.synthetic.main.item_wc_session.view.*
import org.koin.android.ext.android.inject
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.walletconnect.impls.WCSessionStore
import org.walletconnect.impls.WCSessionStore.State
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity

class WalletConnectManageActivity : BaseSubActivity() {

    private val sessionStore: WCSessionStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_nofab)

        if (sessionStore.list().isEmpty()) {
            startActivityFromClass(WalletConnectManageEmptyActivity::class)
            finish()
        } else {
            recycler_view.layoutManager = LinearLayoutManager(this)
            recycler_view.adapter = SessionAdapter(sessionStore.list()) { }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_manage_wc, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_input_text -> true.also {
            showWalletConnectURLInputAlert()
        }
        else -> super.onOptionsItemSelected(item)
    }


}

class StateViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    fun bind(state: State, onSelect: (function: State) -> Unit) {
        view.session_name.text = state.peerData?.meta?.name
        val description = state.peerData?.meta?.description

        view.session_description.setVisibility(description != null)
        if (description != null) {
            view.session_description.text = description
        }
        view.session_description.visibility = View.GONE
        state.peerData?.meta?.icons?.firstOrNull()?.let {
            view.session_icon.load(it)
        }
        view.session_card.setOnClickListener { onSelect(state) }
    }
}

class SessionAdapter(
        private val allFunctions: List<State>,
        private val onSelect: (function: State) -> Unit) : RecyclerView.Adapter<StateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = StateViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_wc_session, parent, false))

    override fun getItemCount() = allFunctions.size

    override fun onBindViewHolder(holder: StateViewHolder, position: Int) {
        holder.bind(allFunctions[position], onSelect)
    }

}
