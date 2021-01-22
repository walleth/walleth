package org.walleth.transactions

import android.content.Intent
import android.os.Bundle
import org.walleth.R
import org.walleth.base_activities.BaseSubActivity
import org.walleth.data.REQUEST_CODE_CHANGE_ACTION

class ChangeActionActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_change_action)

        startActivityForResult(getERC681ActivityIntent(erc681, if (erc681.function == null) {
            ChangeActionFunctionActivity::class
        } else {
            ChangeActionParametersActivity::class
        }), REQUEST_CODE_CHANGE_ACTION)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setResult(resultCode, data)
        finish()
    }
}