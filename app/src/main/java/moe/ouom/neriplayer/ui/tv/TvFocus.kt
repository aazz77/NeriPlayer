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
 * File: moe.ouom.neriplayer.ui.tv/TvFocus
 * Created: 2026/9/21
 *
 * TV 遥控器焦点支持:
 * - LocalIsTvDevice: 全局 TV 设备标识, 由 MainActivity 注入
 * - TvFocusIndication: 全局 Indication, 焦点态绘制高亮描边 + 压暗, 按压态加深
 * - rememberTvEntryFocusRequester / Modifier.tvEntryFocusTarget: 进入界面后自动落焦点
 */

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 是否运行在 TV 设备上, 由 MainActivity 在组合树根部注入 */
val LocalIsTvDevice = staticCompositionLocalOf { false }

/**
 * TV 全局焦点指示器。
 *
 * 替换默认 ripple (仅在 TV 上通过 LocalIndication 注入):
 * - 焦点态: 1.03 放大 + 淡亮蓝背景 + 3dp 亮蓝描边, 遥控器用户能明确看到当前焦点在哪
 * - 按压态: 亮蓝背景加深, 提供按下反馈
 *
 * 通过 LocalIndication 全局注入后, 所有未显式指定 indication 的
 * clickable / combinedClickable 组件自动获得该效果, 无需逐个修改。
 */
object TvFocusIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return TvFocusIndicationNode(interactionSource)
    }

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = System.identityHashCode(this)
}

private const val TV_FOCUS_CORNER_RADIUS_DP = 14
private const val TV_FOCUS_STROKE_WIDTH_DP = 3
private const val TV_FOCUS_SCALE = 1.03f
private const val TV_FOCUS_BLUE_SCRIM_ALPHA = 0.16f
private const val TV_PRESSED_BLUE_SCRIM_ALPHA = 0.28f

// 亮蓝 (Light Blue 300), 明暗主题下都清晰可辨
private val TvFocusBlue = Color(0xFF4FC3F7)

private class TvFocusIndicationNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private var isFocused by mutableStateOf(false)
    private var isPressed by mutableStateOf(false)
    private var interactionJob: Job? = null

    override fun onAttach() {
        interactionJob = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> isFocused = true
                    is FocusInteraction.Unfocus -> isFocused = false
                    is PressInteraction.Press -> isPressed = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                }
            }
        }
    }

    override fun onDetach() {
        interactionJob?.cancel()
        interactionJob = null
    }

    override fun ContentDrawScope.draw() {
        val cornerRadius = CornerRadius(TV_FOCUS_CORNER_RADIUS_DP.dp.toPx())
        if (isFocused) {
            // 焦点态: 内容 + 蓝色背景 + 描边整体放大, 视觉上"浮起"
            withTransform({
                scale(TV_FOCUS_SCALE, TV_FOCUS_SCALE, pivot = center)
            }) {
                drawContent()
                drawRoundRect(
                    color = TvFocusBlue.copy(alpha = TV_FOCUS_BLUE_SCRIM_ALPHA),
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = TvFocusBlue,
                    style = Stroke(width = TV_FOCUS_STROKE_WIDTH_DP.dp.toPx()),
                    cornerRadius = cornerRadius
                )
            }
        } else {
            drawContent()
            if (isPressed) {
                drawRoundRect(
                    color = TvFocusBlue.copy(alpha = TV_PRESSED_BLUE_SCRIM_ALPHA),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}

/**
 * TV 上进入界面后自动请求焦点到挂载了 [Modifier.tvEntryFocusTarget] 的元素。
 * 非 TV 设备返回 null, 调用方无需做任何分支。
 *
 * 入场动画期间组件可能尚未挂载, 这里带重试直到成功。
 */
@Composable
fun rememberTvEntryFocusRequester(): FocusRequester? {
    val isTvDevice = LocalIsTvDevice.current
    val requester = remember(isTvDevice) {
        if (isTvDevice) FocusRequester() else null
    }
    requester?.let { target ->
        LaunchedEffect(target) {
            repeat(ENTRY_FOCUS_MAX_ATTEMPTS) { attempt ->
                delay(if (attempt == 0) ENTRY_FOCUS_INITIAL_DELAY_MILLIS else 250L)
                val success = runCatching { target.requestFocus() }.isSuccess
                if (success) return@LaunchedEffect
            }
        }
    }
    return requester
}

/** 把入焦 requester 挂到目标元素上; 非 TV (requester == null) 时是 no-op */
fun Modifier.tvEntryFocusTarget(requester: FocusRequester?): Modifier =
    if (requester != null) {
        this.focusRequester(requester)
    } else {
        this
    }

/**
 * TV 不可见焦点锚点: 1dp 透明 focusable, 拿到焦点后立即把焦点下移到
 * 屏幕上第一个真实可聚焦项。用于首个可聚焦元素是动态内容的主列表页
 * (Home/Library/Explore/Settings), 避免逐个往深层卡片上挂 requester。
 *
 * 非传 null requester 时是 no-op。
 */
fun Modifier.tvEntryFocusAnchor(requester: FocusRequester?): Modifier = composed {
    if (requester == null) {
        Modifier
    } else {
        val focusManager = LocalFocusManager.current
        Modifier
            .focusRequester(requester)
            .requiredSize(1.dp)
            .focusable()
            .onFocusChanged { state ->
                if (state.isFocused) {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            }
    }
}

private const val ENTRY_FOCUS_MAX_ATTEMPTS = 8
private const val ENTRY_FOCUS_INITIAL_DELAY_MILLIS = 450L
