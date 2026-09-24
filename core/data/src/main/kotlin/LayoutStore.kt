package dev.btdeck.core.data

import android.content.Context
import dev.btdeck.core.model.Keys
import dev.btdeck.core.model.Mod
import dev.btdeck.core.model.PadButton
import dev.btdeck.core.model.PadLayout
import dev.btdeck.core.model.PadState
import dev.btdeck.core.model.PressMode
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Layouts persisted as JSON in SharedPreferences. */
class LayoutStore(context: Context) {
    private val prefs = context.getSharedPreferences("layouts", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<PadState> = _state.asStateFlow()

    fun select(id: String) = save(_state.value.copy(selectedId = id))

    fun updateLayout(id: String, transform: (PadLayout) -> PadLayout) {
        val s = _state.value
        save(s.copy(layouts = s.layouts.map { if (it.id == id) transform(it) else it }))
    }

    fun updateSelected(transform: (PadLayout) -> PadLayout) = updateLayout(_state.value.selected.id, transform)

    fun upsertButton(button: PadButton) = updateSelected { layout ->
        val exists = layout.buttons.any { it.id == button.id }
        layout.copy(
            buttons = if (exists) {
                layout.buttons.map { if (it.id == button.id) button else it }
            } else {
                layout.buttons + button
            },
        )
    }

    fun deleteButton(id: String) = updateSelected { l -> l.copy(buttons = l.buttons.filterNot { it.id == id }) }

    fun moveButton(id: String, delta: Int) = updateSelected { l ->
        val list = l.buttons.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@updateSelected l
        list.add(j, list.removeAt(i))
        l.copy(buttons = list)
    }

    /** Adds an empty layout and switches to it. */
    fun createLayout(name: String) {
        val new = PadLayout(name = name, buttons = emptyList())
        save(PadState(_state.value.layouts + new, new.id))
    }

    /** Copies layout [id] (with fresh button ids) under a new name and switches to it. */
    fun duplicateLayout(id: String, name: String) {
        val s = _state.value
        val source = s.layouts.firstOrNull { it.id == id } ?: return
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = name,
            buttons = source.buttons.map { it.copy(id = UUID.randomUUID().toString()) },
        )
        save(PadState(s.layouts + copy, copy.id))
    }

    /** Deletes layout [id]. The last layout is never deleted. */
    fun deleteLayout(id: String) {
        val s = _state.value
        if (s.layouts.size <= 1) return
        val rest = s.layouts.filterNot { it.id == id }
        val selected = if (s.selectedId == id) rest.first().id else s.selectedId
        save(PadState(rest, selected))
    }

    /**
     * Adds fresh copies of the built-in layouts next to the user's own and switches to the
     * first one. Nothing is deleted; copies get a new name if the old one is taken.
     */
    fun restoreDefaults() {
        val s = _state.value
        val taken = s.layouts.map { it.name }.toMutableSet()
        val copies = defaultLayouts().map { layout ->
            val name = uniqueName(layout.name, taken)
            taken += name
            layout.copy(name = name)
        }
        save(PadState(s.layouts + copies, copies.first().id))
    }

    /**
     * Adds [layout] (e.g. an imported one) with fresh ids and switches to it. Its name gets
     * " (imported)" if the user already has a layout with that name.
     */
    fun addImported(layout: PadLayout) {
        val s = _state.value
        val copy = layout.copy(
            id = UUID.randomUUID().toString(),
            name = uniqueName(layout.name, s.layouts.map { it.name }.toSet(), tag = "imported"),
            buttons = layout.buttons.map { it.copy(id = UUID.randomUUID().toString()) },
        )
        save(PadState(s.layouts + copy, copy.id))
    }

    /** Replaces everything with the built-in layouts (tests, or a future "reset" option). */
    fun resetToDefaults() = save(defaultState())

    private fun save(s: PadState) {
        _state.value = s
        prefs.edit().putString("data", encodeState(s)).apply()
    }

    private fun load(): PadState = prefs.getString("data", null)?.let(::decodeState) ?: defaultState()
}

/** [base], or "[base] ([tag])", "[base] ([tag] 2)"… whichever isn't in [taken]. */
internal fun uniqueName(base: String, taken: Set<String>, tag: String = "default"): String {
    if (base !in taken) return base
    val candidates = sequenceOf("$base ($tag)") + generateSequence(2) { it + 1 }.map { "$base ($tag $it)" }
    return candidates.first { it !in taken }
}

internal fun defaultState(): PadState {
    val defaults = defaultLayouts()
    return PadState(defaults, defaults.first().id)
}

internal fun encodeState(s: PadState): String = JSONObject()
    .put("selected", s.selectedId)
    .put("layouts", JSONArray(s.layouts.map { it.toJson() }))
    .toString()

/** Null when [raw] is unreadable or has no layouts, so the caller can fall back to defaults. */
internal fun decodeState(raw: String): PadState? = runCatching {
    val json = JSONObject(raw)
    val arr = json.getJSONArray("layouts")
    val layouts = (0 until arr.length()).map { arr.getJSONObject(it).toLayout() }
    if (layouts.isEmpty()) return null
    val selected = json.optString("selected").takeIf { id -> layouts.any { it.id == id } } ?: layouts.first().id
    PadState(layouts, selected)
}.getOrNull()

internal fun PadLayout.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("columns", columns)
    .put(
        "buttons",
        JSONArray(
            buttons.map {
                JSONObject().put("id", it.id).put("label", it.label).put("mods", it.modifiers)
                    .put("key", it.key).put("mode", it.mode.name).put("color", it.color)
                    .put("icon", it.icon ?: "").put("tint", it.tintImage)
            },
        ),
    )

internal fun JSONObject.toLayout(): PadLayout {
    val arr = getJSONArray("buttons")
    return PadLayout(
        id = getString("id"),
        name = getString("name"),
        columns = optInt("columns", 3),
        buttons = (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            PadButton(
                id = b.getString("id"),
                label = b.getString("label"),
                modifiers = b.optInt("mods"),
                key = b.optInt("key"),
                mode = runCatching { PressMode.valueOf(b.getString("mode")) }.getOrDefault(PressMode.TAP),
                color = b.optInt("color"),
                icon = b.optString("icon").ifEmpty { null },
                tintImage = b.optBoolean("tint", false),
            )
        },
    )
}

/** The layouts a fresh install starts with. */
fun defaultLayouts(): List<PadLayout> {
    fun k(name: String) = Keys.all.first { it.name == name }.usage
    val drawing = PadLayout(
        name = "Drawing",
        columns = 3,
        buttons = listOf(
            PadButton(label = "Undo", modifiers = Mod.CTRL, key = k("Z"), color = 1, icon = "undo"),
            PadButton(label = "Redo", modifiers = Mod.CTRL or Mod.SHIFT, key = k("Z"), color = 1, icon = "redo"),
            PadButton(label = "Save", modifiers = Mod.CTRL, key = k("S"), icon = "save"),
            PadButton(label = "Brush", modifiers = 0, key = k("B"), color = 2, icon = "brush"),
            PadButton(label = "Eraser", modifiers = 0, key = k("E"), color = 2, icon = "eraser"),
            PadButton(label = "Invert", modifiers = Mod.CTRL, key = k("I"), icon = "invert"),
            PadButton(label = "Size −", modifiers = 0, key = k("["), color = 3, icon = "size_down"),
            PadButton(label = "Size +", modifiers = 0, key = k("]"), color = 3, icon = "size_up"),
            PadButton(label = "Deselect", modifiers = Mod.CTRL or Mod.SHIFT, key = k("A"), icon = "deselect"),
            PadButton(label = "Pan", modifiers = 0, key = k("Space"), mode = PressMode.HOLD, color = 4, icon = "pan"),
            PadButton(label = "Ctrl", modifiers = Mod.CTRL, key = 0, mode = PressMode.HOLD, color = 4, icon = "ctrl"),
            PadButton(
                label = "Shift",
                modifiers = Mod.SHIFT,
                key = 0,
                mode = PressMode.HOLD,
                color = 4,
                icon = "shift",
            ),
        ),
    )
    return listOf(drawing)
}
