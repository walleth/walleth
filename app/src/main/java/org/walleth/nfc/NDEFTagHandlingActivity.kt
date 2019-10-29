package org.walleth.nfc

import android.os.Bundle


class NDEFTagHandlingActivity : BaseNFCActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
