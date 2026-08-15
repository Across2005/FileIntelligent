package com.crossk.data

import kotlinx.coroutines.CancellationException

/**
 * 仓库层统一结果类型 — 替代赤裸的抛异常 / 静默吞错。
 *
 * Ok 表示成功 + 数据，T 允许 null（用于"成功但无数据"如 delete）；
 * Err 携带可向用户展示的消息与可追因的 throwable。
 */
sealed interface RepoResult<out T> {
    data class Ok<T>(val value: T) : RepoResult<T>
    data class Err(val message: String, val cause: Throwable? = null) : RepoResult<Nothing>

    fun getOrNull(): T? = (this as? Ok)?.value

    fun <R> map(transform: (T) -> R): RepoResult<R> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    companion object {
        /**
         * 包裹任何 suspend 块。
         * block 标 suspend 是为了让块内允许 suspend 调用。
         *
         * 注意：捕获 Exception 而非 Throwable，且必须重抛 CancellationException，
         * 否则协程取消会被吞掉（结构化并发的关键约束）。
         */
        suspend fun <T> runCatchingResult(block: suspend () -> T): RepoResult<T> = try {
            Ok(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Err(e.message ?: e::class.java.simpleName, e)
        }
    }
}
