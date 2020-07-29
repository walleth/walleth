package org.walleth.security


import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.sourcify_config.*
import org.koin.android.ext.android.inject
import org.ligi.compat.HtmlCompat
import org.walleth.R
import org.walleth.data.config.Settings

class SourcifyConfigFragment : Fragment() {

    val settings: Settings by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.sourcify_config, container, false)


    override fun onResume() {
        super.onResume()

        sourcify_intro_text.text = HtmlCompat.fromHtml("Access to contract Sources and MetaData. Please <a href='https://sourcify.dev'>read here for details</a>")
        sourcify_intro_text.movementMethod = LinkMovementMethod()

        sourcify_base_url_input.setText(settings.sourcifyBaseURL)

        sourcify_base_url_input.addTextChangedListener {
            settings.sourcifyBaseURL = it.toString()
        }
    }
}
