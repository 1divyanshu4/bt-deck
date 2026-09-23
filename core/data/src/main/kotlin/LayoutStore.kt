package dev.keyset.core.data

import android.content.Context
import dev.keyset.core.model.Keys
import dev.keyset.core.model.Mod
import dev.keyset.core.model.PadButton
import dev.keyset.core.model.PadLayout
import dev.keyset.core.model.PadState
import dev.keyset.core.model.PressMode
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

    fun updateSelected(transform: (PadLayout) -> PadLayout) {
        val s = _state.value
        val updated = transform(s.selected)
        save(s.copy(layouts = s.layouts.map { if (it.id == updated.id) updated else it }))
    }

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

    fun addLayout(name: String, copyCurrent: Boolean) {
        val s = _state.value
        val new = if (copyCurrent) {
            s.selected.copy(
                id = UUID.randomUUID().toString(),
                name = name,
                buttons = s.selected.buttons.map { it.copy(id = UUID.randomUUID().toString()) },
            )
        } else {
            PadLayout(name = name, buttons = emptyList())
        }
        save(PadState(s.layouts + new, new.id))
    }

    fun deleteSelected() {
        val s = _state.value
        if (s.layouts.size <= 1) return
        val rest = s.layouts.filterNot { it.id == s.selectedId }
        save(PadState(rest, rest.first().id))
    }

    /** Replaces everything with the built-in layouts (tests, or a future "reset" option). */
    fun resetToDefaults() = save(defaultState())

    private fun save(s: PadState) {
        _state.value = s
        prefs.edit().putString("data", encodeState(s)).apply()
    }

    private fun load(): PadState = prefs.getString("data", null)?.let(::decodeState) ?: defaultState()
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

private fun PadLayout.toJson() = JSONObject()
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

private fun JSONObject.toLayout(): PadLayout {
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
