package net.packsi.tunnels.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import net.packsi.tunnels.R

/** Dotted world map rendered from a PNG asset (res/drawable-nodpi/world_map.png). */
@Composable
fun WorldMap(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.world_map),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
