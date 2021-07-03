package org.walleth.trezor

import android.content.Context
import org.walleth.R

/**
@return Either an error string or null in case of no error
 */
fun Context.checkTrezorCompatibility(version: KotlinVersion, model: String) = when {
    model != "1" && model != "T" && !model.startsWith("K1") -> {
        getString(R.string.trezor_invalid_model, model)
    }

    model == "T" && !(version.isAtLeast(2, 1)) -> {
        getString(R.string.trezor_t_too_old, version)
    }

    model == "T" && version.isAtLeast(3, 0) -> {
        getString(R.string.trezor_too_new, version)
    }

    model == "1" && !(version.isAtLeast(1, 8)) -> {
        getString(R.string.trezor_t_too_old, version)
    }

    model == "1" && version.isAtLeast(2, 0) -> {
        getString(R.string.trezor_too_new, version)
    }

    else -> null
}
