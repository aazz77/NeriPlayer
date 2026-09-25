package moe.ouom.neriplayer.ui.component.navigation

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.component.navigation/NeriNavigationRail
 * Created: 2026/9/22
 *
 * TV 端左侧导航栏: 布局与 NeriBottomBar 同构 (玻璃背景 + Material3 导航项),
 * 仅在 TV 设备上由 NeriApp 渲染, 替代底部导航栏以适配遥控器操作。
 * 顶部圆形封面图标作为 NowPlaying 入口, 替代底部 MiniPlayer。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import coil.compose.AsyncImage
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassRole
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassSurface
import moe.ouom.neriplayer.ui.effect.glass.LocalAdvancedGlassController
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import moe.ouom.neriplayer.util.media.fastScrollableImageRequest

@Composable
fun NeriNavigationRail(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier,
    selectAlpha: Float = DEFAULT_BOTTOM_BAR_SELECTION_ALPHA,
    /** 当前播放封面 URL, 非空时在顶部显示圆形入口 */
    nowPlayingCoverUrl: String? = null,
    /** 是否正在播放 (用于视觉提示, 可选) */
    isNowPlayingActive: Boolean = false,
    /** 点击顶部圆形入口进入播放页 */
    onNowPlayingClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val baseBlurRequested = LocalAdvancedGlassController.current.isBaseBlurRequested
    val fallbackScrimAlpha = resolveBottomBarFallbackScrimAlpha(
        selectAlpha = selectAlpha,
        baseBlurRequested = baseBlurRequested
    )
    val fallbackColor = if (fallbackScrimAlpha > 0f) {
        MaterialTheme.colorScheme.background.copy(alpha = fallbackScrimAlpha)
    } else {
        Color.Transparent
    }

    AdvancedGlassSurface(
        role = AdvancedGlassRole.SideNavigation,
        modifier = modifier,
        fallbackColor = fallbackColor
    ) {
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color.Transparent),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            header = if (onNowPlayingClick != null) {
                {
                    val coverDesc = stringResource(R.string.player_now_playing)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (isNowPlayingActive) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = coverDesc
                            }
                            .clickable {
                                context.performHapticFeedback()
                                onNowPlayingClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val cover = nowPlayingCoverUrl?.trim()?.takeIf { it.isNotEmpty() }
                        if (cover != null) {
                            AsyncImage(
                                model = fastScrollableImageRequest(
                                    context = context,
                                    data = cover,
                                    sizePx = 128,
                                    crossfade = false,
                                    offlineMode = false
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                null
            }
        ) {
            items.forEach { (dest, icon) ->
                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                val label = stringResource(dest.labelResId)
                NavigationRailItem(
                    selected = selected,
                    onClick = {
                        context.performHapticFeedback()
                        onItemSelected(dest)
                    },
                    icon = { Icon(icon, contentDescription = label) },
                    label = {
                        Text(
                            text = label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    // TV 遥控器场景下固定显示文字标签, 与选中态图标一起提供明确导航提示
                    alwaysShowLabel = true,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                            alpha = selectAlpha
                        ),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }
        }
    }
}
