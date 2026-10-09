package dev.btdeck.feature.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.btdeck.core.designsystem.theme.Palette
import dev.btdeck.core.designsystem.theme.ZexDeckColors
import dev.btdeck.core.designsystem.theme.ZexDeckShapes
import kotlinx.coroutines.launch

private data class WalkthroughPageData(
    val badge: String,
    val title: String,
    val description: String,
    val showLogo: Boolean = false,
    val bullets: List<Pair<ImageVector, String>> = emptyList(),
    @param:DrawableRes val image: Int? = null,
    val tip: String? = null,
)

private val pages = listOf(
    WalkthroughPageData(
        badge = "WELCOME",
        title = "Welcome to ZexDeck",
        description = "Turn your phone into a customizable wireless macro pad and keyboard " +
            "for your PC, Mac, iPad, or tablet.",
        showLogo = true,
        bullets = listOf(
            Icons.Default.Bluetooth to "No desktop software or drivers needed — works via standard Bluetooth.",
            Icons.Default.GridView to "Custom shortcut pads for Photoshop, Blender, Krita, IDEs, and gaming.",
            Icons.Default.Keyboard to "Full wireless keyboard with text sender and modifier controls.",
        ),
        tip = "If you ever get confused, check the Tutorials section in Settings for visual guides and walkthroughs.",
    ),
    WalkthroughPageData(
        badge = "STEP 1 · PAIRING",
        title = "Pair Once, Connect Anytime",
        description = "Connect wirelessly in just a few quick steps:",
        bullets = listOf(
            Icons.Default.Bluetooth to "Open Bluetooth settings on your computer or tablet.",
            Icons.Default.GridView to "Tap the status chip at the top of the app to Scan & Pair.",
            Icons.Default.Keyboard to "Accept the pairing prompt on both screens. That's it!",
        ),
        image = R.drawable.tutorial_connect_2,
        tip = "Once paired, ZexDeck reconnects automatically whenever you open the app.",
    ),
    WalkthroughPageData(
        badge = "STEP 2 · SHORTCUTS",
        title = "Customize Your Buttons",
        description = "Send any key combination with a single tap:",
        bullets = listOf(
            Icons.Default.Edit to "Tap the pencil icon (✎) in the top bar to edit the layout.",
            Icons.Default.GridView to "Add, rename, pick custom icons, and assign hotkeys.",
            Icons.AutoMirrored.Filled.ViewQuilt to "Drag buttons around to place them right where your thumb rests.",
        ),
        image = R.drawable.tutorial_button_3,
        tip = "Buttons support single Tap, Hold (great for spacebar pan), and Toggle modes.",
    ),
    WalkthroughPageData(
        badge = "STEP 3 · LAYOUTS & KEYBOARD",
        title = "Profiles for Every App",
        description = "Organize shortcuts and type seamlessly:",
        bullets = listOf(
            Icons.AutoMirrored.Filled.ViewQuilt to
                "Layouts Tab: Build distinct button sets for Photoshop, Blender, Premiere, etc.",
            Icons.Default.Keyboard to "Keyboard Tab: Full wireless keyboard with text sender for long sentences.",
        ),
        image = R.drawable.tutorial_layout_1,
        tip = "You're all set! Remember you can always revisit guides in Settings → Tutorials.",
    ),
)

/**
 * Interactive first-time walkthrough dialog introducing the app's features,
 * Bluetooth connection flow, button editor, and layouts.
 */
@Composable
fun WalkthroughDialog(onDismiss: () -> Unit, onConnect: () -> Unit = onDismiss, modifier: Modifier = Modifier) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)

        val pager = rememberPagerState(pageCount = { pages.size })
        val scope = rememberCoroutineScope()
        val isLastPage = pager.currentPage == pages.lastIndex

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxHeight(0.92f)
                    .clip(ZexDeckShapes.Medium)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), ZexDeckShapes.Medium),
                color = ZexDeckColors.Background,
                shape = ZexDeckShapes.Medium,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    // Top header: Close / Skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Walkthrough",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Gray,
                        )
                        TextButton(onClick = onDismiss) {
                            Text("Skip", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Pager Content
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        pageSpacing = 16.dp,
                    ) { pageIndex ->
                        WalkthroughPageContent(pages[pageIndex])
                    }

                    Spacer(Modifier.height(12.dp))

                    // Bottom Navigation Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (pager.currentPage > 0) {
                            TextButton(
                                onClick = {
                                    scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                                },
                            ) {
                                Text("Back")
                            }
                        } else {
                            Spacer(Modifier.width(64.dp))
                        }

                        // Indicator Dots
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(pages.size) { i ->
                                val active = i == pager.currentPage
                                Box(
                                    Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Gray.copy(alpha = 0.4f)
                                            },
                                        ),
                                )
                            }
                        }

                        if (!isLastPage) {
                            Button(
                                onClick = {
                                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                                },
                                shape = ZexDeckShapes.Small,
                            ) {
                                Text("Next")
                            }
                        } else {
                            Button(
                                onClick = onConnect,
                                shape = ZexDeckShapes.Small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black,
                                ),
                            ) {
                                Text("Connect", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isLastPage) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        ) {
                            Text("Explore Pad First", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ZexDeckColors.SurfaceHigh)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier
                        .size(21.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFB9A6FF)),
                )
                Box(
                    Modifier
                        .size(21.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier
                        .size(21.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                )
                Box(
                    Modifier
                        .size(21.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF9E7A)),
                )
            }
        }
    }
}

@Composable
private fun WalkthroughPageContent(page: WalkthroughPageData) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val pictureMaxHeight = maxHeight * 0.44f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // App Logo
        if (page.showLogo) {
            Spacer(Modifier.height(8.dp))
            AppLogo()
            Spacer(Modifier.height(14.dp))
        }

        // Optional Illustration
        if (page.image != null) {
            val picture = painterResource(page.image)
            Image(
                painter = picture,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .heightIn(max = pictureMaxHeight)
                    .aspectRatio(picture.intrinsicSize.width / picture.intrinsicSize.height)
                    .clip(ZexDeckShapes.Medium)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), ZexDeckShapes.Medium),
            )
            Spacer(Modifier.height(14.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Badge / Subheading
            Text(
                text = page.badge,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Title
            Text(
                text = page.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
            )

            // Description
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE0E0E6),
                lineHeight = 20.sp,
            )

            if (page.bullets.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    page.bullets.forEach { (icon, text) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ZexDeckShapes.Small)
                                .background(ZexDeckColors.Surface)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = text,
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }

            if (page.tip != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ZexDeckShapes.Small)
                        .background(Palette[1].copy(alpha = 0.15f))
                        .border(1.dp, Palette[1].copy(alpha = 0.3f), ZexDeckShapes.Small)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Palette[1])) {
                                append("Tip: ")
                            }
                            append(page.tip)
                        },
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
    }
}
