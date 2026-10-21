package com.maxarcher.launcher.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.unit.toSize
import com.maxarcher.launcher.LaunchableIdentity
import com.maxarcher.launcher.OrderedFavoriteModule
import com.maxarcher.launcher.R
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal data class HomeFavoriteEnterKey(val moduleId: String, val identity: LaunchableIdentity? = null)

/** Durable membership, not a composable appearing or a drag's source-omitted projection. */
internal fun homeFavoriteEnterKeys(
    previous: Map<String, List<LaunchableIdentity>>?,
    current: Map<String, List<LaunchableIdentity>>,
): Set<HomeFavoriteEnterKey> {
    if (previous == null) return emptySet()
    val oldIdentities = previous.values.flatten().toSet()
    return buildSet(builderAction = {
        current.forEach(action = { (moduleId, identities) ->
            if (moduleId !in previous) {
                // Drop-to-create moves an existing identity; neither it nor its parent fades.
                if (identities.none(predicate = { it in oldIdentities })) {
                    add(element = HomeFavoriteEnterKey(moduleId = moduleId))
                }
            } else {
                identities.filterNot(predicate = { it in oldIdentities }).forEach(action = { identity ->
                    add(element = HomeFavoriteEnterKey(moduleId = moduleId, identity = identity))
                })
            }
        })
    })
}

internal class HomeFavoriteEnterTransitions(val exits: HomeFavoriteExitTransitions? = null) {
    private var previous: Map<String, List<LaunchableIdentity>>? = null
    private val moduleFades = mutableMapOf<String, Any>()
    var viewport: Rect? = null

    fun next(modules: List<OrderedFavoriteModule>?): HomeFavoriteEnterBatch {
        val current = modules?.associate(transform = { it.id to it.identities })
        val keys = if (current == null) emptySet() else homeFavoriteEnterKeys(previous = previous, current = current)
        return HomeFavoriteEnterBatch(keys = keys, moduleFades = moduleFades, owner = this)
    }

    fun commit(modules: List<OrderedFavoriteModule>?) {
        exits?.sync(modules = modules)
        previous = modules?.associate(transform = { it.id to it.identities })
    }
}

/** Eligibility lasts only through the change's first layout, never until a later scroll/return. */
internal class HomeFavoriteEnterBatch(
    keys: Set<HomeFavoriteEnterKey>,
    private val moduleFades: MutableMap<String, Any>,
    private val owner: HomeFavoriteEnterTransitions? = null,
) {
    val exitTransitions: HomeFavoriteExitTransitions? get() = owner?.exits
    val tickets = mutableListOf<HomeFavoriteEnterTicket>()
    private val unclaimed = keys.toMutableSet()
    var open: Boolean = keys.isNotEmpty()
        private set

    fun close() {
        open = false
        unclaimed.clear()
    }

    fun updateViewport(bounds: Rect) {
        owner?.viewport = bounds
    }

    fun isVisible(bounds: Rect, clippedVisible: Boolean): Boolean =
        owner?.viewport?.overlaps(other = bounds) ?: clippedVisible

    fun claim(key: HomeFavoriteEnterKey): HomeFavoriteEnterTicket? {
        if (!open || key !in unclaimed) return null
        if (key.identity != null && moduleFades.containsKey(key = key.moduleId)) return null
        val ticket = HomeFavoriteEnterTicket(batch = this, key = key)
        tickets.add(element = ticket)
        return ticket
    }

    fun attach(ticket: HomeFavoriteEnterTicket) {
        unclaimed.remove(element = ticket.key)
    }

    fun begin(ticket: HomeFavoriteEnterTicket) {
        if (ticket.key.identity == null) moduleFades[ticket.key.moduleId] = ticket
    }

    fun finish(ticket: HomeFavoriteEnterTicket) {
        if (moduleFades[ticket.key.moduleId] === ticket) moduleFades.remove(key = ticket.key.moduleId)
    }
}

internal class HomeFavoriteEnterTicket(val batch: HomeFavoriteEnterBatch, val key: HomeFavoriteEnterKey) {
    var placed = false
    var opacity = 1f
}

@Composable
internal fun rememberHomeFavoriteEnterBatch(modules: List<OrderedFavoriteModule>?): HomeFavoriteEnterBatch {
    val graphics = LocalGraphicsContext.current
    val scope = rememberCoroutineScope()
    val duration = integerResource(id = R.integer.short_property_animation_duration_ms)
    val exits = remember(key1 = graphics, key2 = scope, key3 = duration, calculation = {
        HomeFavoriteExitTransitions(graphics = graphics, scope = scope, duration = duration)
    })
    DisposableEffect(key1 = exits, effect = { onDispose { exits.dispose() } })
    val transitions = remember(key1 = exits, calculation = { HomeFavoriteEnterTransitions(exits = exits) })
    val batch = remember(key1 = transitions, key2 = modules, calculation = { transitions.next(modules = modules) })
    SideEffect(effect = { transitions.commit(modules = modules) })
    LaunchedEffect(key1 = batch, block = {
        // Lazy children are subcomposed during this frame's measure/layout, after the parent's
        // composition. Closing in SideEffect would reject those genuine additions too early.
        withFrameNanos(onFrame = { batch.close() })
    })
    return batch
}

@Composable
internal fun Modifier.homeFavoriteEnter(
    batch: HomeFavoriteEnterBatch?,
    key: HomeFavoriteEnterKey,
): Modifier {
    // Keep an in-flight fade when another mutation creates a new batch. Existing cells must
    // never acquire a fresh ticket just because their data, edit controls, or parent updated.
    val ticket = remember(key1 = key, calculation = { batch?.claim(key = key) })
    val alpha = remember(key1 = ticket, calculation = { Animatable(initialValue = 1f) })
    SideEffect(effect = { ticket?.opacity = alpha.value })
    var finished by remember(key1 = ticket, calculation = { mutableStateOf(value = false) })
    if (ticket == null || finished) {
        return homeFavoriteExitCapture(
            owner = batch?.exitTransitions,
            key = key,
            opacity = { alpha.value },
        )
    }
    val scope = rememberCoroutineScope()
    val duration = integerResource(id = R.integer.short_property_animation_duration_ms)
    DisposableEffect(key1 = ticket, effect = {
        ticket.batch.attach(ticket = ticket)
        onDispose { ticket.batch.finish(ticket = ticket) }
    })
    return onGloballyPositioned(onGloballyPositioned = { coordinates ->
        if (!ticket.placed) {
            ticket.placed = true
            // A growing module can temporarily clip a genuine new row. Use the main viewport,
            // not that animated inner clip, to distinguish it from offscreen content.
            val visible = ticket.batch.isVisible(
                bounds = Rect(offset = coordinates.positionInWindow(), size = coordinates.size.toSize()),
                clippedVisible = !coordinates.boundsInWindow().isEmpty,
            )
            val disabled = scope.coroutineContext[MotionDurationScale]?.scaleFactor == 0f
            if (ticket.batch.open && visible && !disabled) {
                ticket.batch.begin(ticket = ticket)
                scope.launch(start = CoroutineStart.UNDISPATCHED, block = {
                    try {
                        alpha.snapTo(targetValue = ticket.batch.exitTransitions?.restorationAlpha(key = key) ?: 0f)
                        alpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = duration))
                    } finally {
                        ticket.batch.finish(ticket = ticket)
                        finished = true
                    }
                })
            } else {
                ticket.batch.finish(ticket = ticket)
                finished = true
            }
        }
    }).graphicsLayer(block = { this.alpha = alpha.value }).homeFavoriteExitCapture(
        owner = batch?.exitTransitions,
        key = key,
        opacity = { alpha.value },
    )
}
