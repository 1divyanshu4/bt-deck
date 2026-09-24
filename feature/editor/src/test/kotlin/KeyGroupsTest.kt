package dev.keyset.feature.editor

import dev.keyset.core.model.Keys
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyGroupsTest {
    @Test fun everyKeyIsInExactlyOneGroup() {
        val grouped = KeyGroups.flatMap { it.second }.map { it.name }
        assertEquals(grouped.distinct(), grouped)
        assertEquals(Keys.all.map { it.name }.sorted(), grouped.sorted())
    }
}
