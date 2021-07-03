package org.walleth.tests

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.walleth.R
import org.walleth.trezor.checkTrezorCompatibility

class TheTrezorCheck {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

    @Test
    fun canDetectBadModel() {
        assertThat(context.checkTrezorCompatibility(KotlinVersion(1, 0, 0), "X"))
            .isEqualTo(context.getString(R.string.trezor_invalid_model, "X"))
    }

    @Test
    fun canDetectOldTVersion() {
        val version = KotlinVersion(2, 0)
        assertThat(context.checkTrezorCompatibility(version, "T"))
            .isEqualTo(context.getString(R.string.trezor_t_too_old, version))
    }

    @Test
    fun canDetectTooNewTVersion() {
        val version = KotlinVersion(3, 0, 0)
        assertThat(context.checkTrezorCompatibility(version, "T"))
            .isEqualTo(context.getString(R.string.trezor_too_new, version))
    }

    @Test
    fun canDetectOld1Version() {
        val version = KotlinVersion(1, 7, 0)
        assertThat(context.checkTrezorCompatibility(version, "1"))
            .isEqualTo(context.getString(R.string.trezor_t_too_old, version))
    }

    @Test
    fun canDetectTooNew1Version() {
        val version = KotlinVersion(2, 0, 0)
        assertThat(context.checkTrezorCompatibility(version, "1"))
            .isEqualTo(context.getString(R.string.trezor_too_new, version))
    }

    @Test
    fun isHappyAboutCompatibleVersions() {
        assertThat(context.checkTrezorCompatibility(KotlinVersion(2, 1), "T")).isNull()
        assertThat(context.checkTrezorCompatibility(KotlinVersion(1, 8), "1")).isNull()

    }


}
