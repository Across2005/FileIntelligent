package com.crossk.ai

import com.crossk.data.Entity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EntityIDFactoryTest {

    @Test fun `same input produces same ID`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习")
        val b = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习")
        assertThat(a).isEqualTo(b)
    }

    @Test fun `different name produces different ID`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习")
        val b = EntityIDFactory.entityID(Entity.Type.CONCEPT, "深度学习")
        assertThat(a).isNotEqualTo(b)
    }

    @Test fun `different type produces different ID for same name`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "神经网络")
        val b = EntityIDFactory.entityID(Entity.Type.METHOD, "神经网络")
        assertThat(a).isNotEqualTo(b)
    }

    @Test fun `ID is case-insensitive`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "Machine Learning")
        val b = EntityIDFactory.entityID(Entity.Type.CONCEPT, "machine learning")
        assertThat(a).isEqualTo(b)
    }

    @Test fun `ID format is ent_type_uuid`() {
        val id = EntityIDFactory.entityID(Entity.Type.CONCEPT, "测试")
        assertThat(id).startsWith("ent_")
        assertThat(id).contains("concept")
    }

    @Test fun `100 unique names produce 100 unique IDs`() {
        val ids = (1..100).map { EntityIDFactory.entityID(Entity.Type.CONCEPT, "entity_$it") }
        assertThat(ids.toSet()).hasSize(100)
    }

    @Test fun `ID survives Chinese punctuation and emoji`() {
        val a = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习!")
        val b = EntityIDFactory.entityID(Entity.Type.CONCEPT, "机器学习！")  // full-width
        // Different bytes → different IDs (no fancy normalization beyond case)
        assertThat(a).isNotEqualTo(b)
    }
}
