package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.activity_enter_password.*
import org.walleth.R
import org.walleth.data.EXTRA_KEY_PWD

const val EXTRA_KEY_RESULT_RECEIVER = "resultReceiver"

class PasswordReceivingFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivityForResult(Intent(context, RequestPasswordActivity::class.java), 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        arguments?.getParcelable<ResultReceiver>(EXTRA_KEY_RESULT_RECEIVER)?.send(resultCode, data?.extras)
    }
}

const val TAG_PASSWORD_RECEIVING = "pwdreceive"

fun FragmentActivity.getPassword(callback: (pwd: String?) -> Unit) {
    Handler().post {
        var fragmentRemovingCallback: ((foo: Bundle?) -> Unit)? = fun(resultData: Bundle?) {
            supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentByTag(TAG_PASSWORD_RECEIVING)!!).commitAllowingStateLoss()
            resultData?.getString(EXTRA_KEY_PWD).let {
                callback.invoke(it)
            }
        }
        supportFragmentManager.beginTransaction().apply {
            val fragment = PasswordReceivingFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_KEY_RESULT_RECEIVER, object : ResultReceiver(Handler(Looper.getMainLooper())) {
                        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                            super.onReceiveResult(resultCode, resultData)
                            fragmentRemovingCallback?.invoke(resultData)
                            fragmentRemovingCallback = null
                        }
                    })
                }
            }
            add(fragment, TAG_PASSWORD_RECEIVING)
            commitAllowingStateLoss()
        }
    }
}

class RequestPasswordActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_enter_password)

        input_pwd.imeOptions = EditorInfo.IME_ACTION_DONE
        input_pwd.setOnEditorActionListener { _, _, _ -> true.also { deliverResult() } }
        fab.setOnClickListener {
            deliverResult()
        }
    }

    private fun deliverResult() {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_PWD, input_pwd.text.toString()))
        finish()
    }


}
