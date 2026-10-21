package com.maxarcher.launcher.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.OrderedFavoriteModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Owns render remnants only. No removed composable, layout slot, input node or store write. */
internal class HomeFavoriteExitTransitions(
    val graphics: GraphicsContext,
    private val scope: CoroutineScope,
    private val duration: Int,
) {
    private var previous: Map<String, List<LaunchableIdentity>>? = null
    private val records = mutableSetOf<HomeFavoriteExitLayer>()
    private val restorations = mutableMapOf<HomeFavoriteEnterKey, Float>()
    val ghosts = mutableStateListOf<HomeFavoriteExitGhost>()
    var viewport: Rect? = null
    private var cleanup: Job? = null
    private var disposed = false

    fun sync(modules: List<OrderedFavoriteModule>?) {
        val current = modules?.associate(transform = { it.id to it.identities })
        if (current == previous) return
        val old = previous
        previous = current
        if (current == null) {
            clearGhosts()
            return
        }
        ghosts.toList().forEach(action = { ghost ->
            val identities = current[ghost.record.key.moduleId]
            val restored = identities != null && (ghost.record.key.identity == null || ghost.record.key.identity in identities)
            if (restored) {
                restorations[ghost.record.key] = ghost.alpha.value
                // A coalesced Undo can restore membership before lazy disposal runs. Do not
                // leave that still-attached node permanently suppressed as an outgoing layer.
                ghost.record.retiring = false
                ghost.record.layer.alpha = 1f
                remove(ghost = ghost)
            }
        })
        if (old != null && scope.coroutineContext[MotionDurationScale]?.scaleFactor != 0f) {
            // Reversing the membership comparison selects removals, still excluding moves and
            // selecting a parent instead of its children when the complete module disappears.
            val removed = homeFavoriteEnterKeys(previous = current, current = old)
            removed.forEach(action = { key ->
                val record = records.lastOrNull(predicate = { it.key == key && !it.retiring && it.drawn })
                    ?: return@forEach
                val clip = record.clip.intersect(other = viewport ?: record.clip)
                if (clip.isEmpty) return@forEach
                val parentAlpha = if (key.identity == null) 1f else records.lastOrNull(
                    predicate = { it.key == HomeFavoriteEnterKey(moduleId = key.moduleId) && !it.retiring },
                )?.opacity?.invoke() ?: 1f
                val ghost = HomeFavoriteExitGhost(record = record, clip = clip, opacity = record.opacity() * parentAlpha)
                record.retiring = true
                ghosts.add(element = ghost)
                if (key.identity == null) {
                    // Retain the parent and its child display lists before cancelling an earlier
                    // child exit; cancellation can synchronously run resource cleanup.
                    ghosts.filter(predicate = { it !== ghost && it.record.key.moduleId == key.moduleId })
                        .forEach(action = { remove(ghost = it) })
                }
                ghost.job = scope.launch(start = CoroutineStart.UNDISPATCHED, block = {
                    try {
                        ghost.alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = duration))
                    } finally {
                        ghosts.remove(element = ghost)
                        releaseUnused()
                    }
                })
            })
        }
        scheduleCleanup()
    }

    fun restorationAlpha(key: HomeFavoriteEnterKey): Float? = restorations.remove(key = key)

    fun attach(record: HomeFavoriteExitLayer) {
        record.attached = true
        records.add(element = record)
    }

    fun detach(record: HomeFavoriteExitLayer) {
        record.attached = false
        // A normal composable's onDispose may precede the parent's membership SideEffect;
        // lazy-child disposal may follow it. Keep the last display list through both orders.
        scheduleCleanup()
    }

    private fun scheduleCleanup() {
        if (disposed || cleanup?.isActive == true) return
        cleanup = scope.launch(block = {
            withFrameNanos(onFrame = {})
            releaseUnused()
            restorations.clear()
        })
    }

    private fun remove(ghost: HomeFavoriteExitGhost) {
        ghosts.remove(element = ghost)
        ghost.job?.cancel()
    }

    private fun clearGhosts() {
        ghosts.toList().forEach(action = { remove(ghost = it) })
        restorations.clear()
        releaseUnused()
    }

    private fun releaseUnused() {
        val heldModules = ghosts.filter(predicate = { it.record.key.identity == null })
            .mapTo(destination = mutableSetOf(), transform = { it.record.key.moduleId })
        records.filter(predicate = { record ->
            !record.attached && ghosts.none(predicate = { it.record === record }) && record.key.moduleId !in heldModules
        }).forEach(action = { record ->
            records.remove(element = record)
            record.release()
        })
    }

    fun dispose() {
        disposed = true
        cleanup?.cancel()
        clearGhosts()
        records.toList().forEach(action = { it.release() })
        records.clear()
    }
}

internal class HomeFavoriteExitLayer(
    private val owner: HomeFavoriteExitTransitions,
    val key: HomeFavoriteEnterKey,
) : RememberObserver {
    private var snapshot: GraphicsLayer? = null
    val layer: GraphicsLayer get() = snapshot ?: owner.graphics.createGraphicsLayer().also(block = { snapshot = it })
    var opacity: () -> Float = { 1f }
    var origin = Offset.Zero
    var clip = Rect.Zero
    var drawn = false
    var attached = false
    var retiring = false
    override fun onRemembered() = owner.attach(record = this)
    override fun onForgotten() = owner.detach(record = this)
    override fun onAbandoned() = release()
    fun release() {
        snapshot?.let(block = { layer -> if (!layer.isReleased) owner.graphics.releaseGraphicsLayer(layer = layer) })
    }
}

internal class HomeFavoriteExitGhost(val record: HomeFavoriteExitLayer, val clip: Rect, opacity: Float) {
    val origin = record.origin
    val alpha = Animatable(initialValue = opacity)
    var job: Job? = null
}

@Composable
internal fun Modifier.homeFavoriteExitCapture(
    owner: HomeFavoriteExitTransitions?,
    key: HomeFavoriteEnterKey,
    opacity: () -> Float,
): Modifier {
    if (owner == null) return this
    val record = remember(key1 = owner, key2 = key, calculation = { HomeFavoriteExitLayer(owner = owner, key = key) })
    SideEffect(effect = { record.opacity = opacity })
    return onGloballyPositioned(onGloballyPositioned = { coordinates ->
        if (!record.retiring) {
            record.origin = coordinates.positionInWindow()
            record.clip = coordinates.boundsInWindow()
        }
    }).drawWithContent(onDraw = {
        if (!record.retiring) {
            // Do not allocate a capture layer for eagerly composed, offscreen grid cells.
            if (record.clip.isEmpty) {
                drawContent()
                return@drawWithContent
            }
            record.layer.record(
                density = this, layoutDirection = layoutDirection,
                size = IntSize(width = size.width.roundToInt(), height = size.height.roundToInt()),
                block = { this@drawWithContent.drawContent() },
            )
            record.drawn = true
            drawLayer(graphicsLayer = record.layer)
        }
    })
}

@Composable
internal fun HomeFavoriteExitOverlay(owner: HomeFavoriteExitTransitions?, rootOrigin: Offset) {
    if (owner == null) return
    Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
        owner.ghosts.forEach(action = { ghost ->
            val clip = ghost.clip.intersect(other = owner.viewport ?: ghost.clip).translate(offset = -rootOrigin)
            if (!clip.isEmpty && !ghost.record.layer.isReleased) {
                clipRect(left = clip.left, top = clip.top, right = clip.right, bottom = clip.bottom, block = {
                    val origin = ghost.origin - rootOrigin
                    translate(left = origin.x, top = origin.y, block = {
                        ghost.record.layer.alpha = ghost.alpha.value
                        drawLayer(graphicsLayer = ghost.record.layer)
                    })
                })
            }
        })
    })
}
