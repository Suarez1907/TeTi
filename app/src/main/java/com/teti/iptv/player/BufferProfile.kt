package com.teti.iptv.player

enum class BufferProfile(
    val title: String,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
) {
    LOW_LATENCY(
        title = "Düşük Gecikme",
        minBufferMs = 2_000,
        maxBufferMs = 5_000,
        bufferForPlaybackMs = 500,
        bufferForPlaybackAfterRebufferMs = 1_000
    ),
    BALANCED(
        title = "Dengeli",
        minBufferMs = 15_000,
        maxBufferMs = 50_000,
        // Kanal değiştirmede ilk görüntü daha hızlı gelsin.
        bufferForPlaybackMs = 1_500,
        bufferForPlaybackAfterRebufferMs = 5_000
    ),
    HIGH_STABILITY(
        title = "Yüksek Kararlılık",
        minBufferMs = 30_000,
        maxBufferMs = 120_000,
        bufferForPlaybackMs = 5_000,
        bufferForPlaybackAfterRebufferMs = 10_000
    )
}
