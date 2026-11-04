package com.huanchengfly.tieba.post.ui.page.main.home

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

// A missing first emission or a hung backend must not hold the home skeleton indefinitely.
internal const val HOME_REFRESH_TIMEOUT_MILLIS = 25_000L

internal fun <T : Any> Flow<T>.firstOrFailWithin(timeoutMillis: Long): Flow<T> = flow {
    require(timeoutMillis > 0L)
    val firstValue = withTimeout(timeoutMillis) { firstOrNull() }
        ?: throw IllegalStateException("Home refresh completed without a result")
    emit(firstValue)
}
