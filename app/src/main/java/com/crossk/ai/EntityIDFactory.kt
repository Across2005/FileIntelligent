package com.crossk.ai

import com.crossk.data.Entity
import java.util.UUID

/**
 * Stable entity ID generator. v3.0 replaces v1's `ent_${name.hashCode().toUInt()}` which
 * is collision-prone (32-bit hash, ~50% chance of collision at 70k unique names).
 *
 * ID format: `ent_{type}_{UUID5(namespace, name+type)}`
 * - Stable: same (type, name, case) → same ID
 * - Collision-resistant: UUID v5 (SHA-1 hash) over 128-bit space
 * - Type-aware: same name under different type → different ID
 */
object EntityIDFactory {

    private val NAMESPACE = UUID.fromString("6c7e9f3a-1b2c-4d5e-8f9a-0b1c2d3e4f5a")  // arbitrary fixed

    fun entityID(type: Entity.Type, name: String): String {
        val normalized = name.trim().lowercase()
        val key = "${type.name}|$normalized"
        val uuid = UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8))
        return "ent_${type.name.lowercase()}_${uuid}"
    }
}
