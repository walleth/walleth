package org.walleth.walletconnect

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.google.accompanist.coil.rememberCoilPainter
import org.walleth.walletconnect.model.WalletConnectEnhancedApp


class WalletConnectAppPreviewParameterProvider : PreviewParameterProvider<WalletConnectEnhancedApp> {
    override val values: Sequence<WalletConnectEnhancedApp> = sequenceOf(
        WalletConnectEnhancedApp(name = "yolo", url = "url", icon = null, networks = "foo")
    )
}

@Preview
@Composable
fun WalletConnectAppComposable(
    @PreviewParameter(WalletConnectAppPreviewParameterProvider::class) app: WalletConnectEnhancedApp
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.url)).apply {
                    flags += Intent.FLAG_ACTIVITY_NEW_TASK
                })
            },
        elevation = 2.dp,
    ) {
        Row {
            Image(
                painter = rememberCoilPainter(request = app.icon),
                contentDescription = null,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .align(Alignment.CenterVertically)
                    .padding(4.dp)
            )
            Column {
                Text(app.name, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                app.networks?.run {
                    Text(this, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}