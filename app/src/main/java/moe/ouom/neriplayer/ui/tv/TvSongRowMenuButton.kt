package moe.ouom.neriplayer.ui.tv

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
 * File: moe.ouom.neriplayer.ui.tv/TvSongRowMenuButton
 * Created: 2026/9/25
 *
 * 歌曲行右侧"更多操作"(三个竖点)按钮的 TV 适配:
 * - 非 TV: 保持 IconButton, 点击弹出歌曲操作菜单
 * - TV: 三点按钮不作为独立焦点目标 (焦点只能落在歌曲卡片上,
 *   遥控器点击卡片直接播放), 仅静态渲染图标
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SongRowMenuButton(
    onOpenMenu: () -> Unit,
    contentDescription: String?,
    size: Dp = 40.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val isTvDevice = LocalIsTvDevice.current
    if (isTvDevice) {
        // TV: 整行一个焦点, 三点图标仅静态展示, 不参与焦点导航
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = tint
            )
        }
    } else {
        IconButton(
            onClick = onOpenMenu,
            modifier = Modifier.size(size)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}
