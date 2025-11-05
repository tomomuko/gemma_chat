package com.example.gemmabench.utils

import android.util.Log

/**
 * メモリ監視ユーティリティ
 *
 * モデルロード前にメモリ不足を検出し、OOM（Out of Memory）を防止
 * Android 15 のメモリ管理ポリシー変更に対応
 */
object MemoryMonitor {
    /**
     * 利用可能なメモリが要件を満たしているかチェック
     *
     * @param requiredMB 必要なメモリ量（MB）
     * @return メモリが十分な場合 true、不足している場合 false
     */
    fun checkAvailableMemory(requiredMB: Long): Boolean {
        val runtime = Runtime.getRuntime()

        // 利用可能メモリを計算（MB単位）
        val maxMemoryMB = runtime.maxMemory() / 1024 / 1024
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val availableMemoryMB = maxMemoryMB - usedMemoryMB

        // 20% の安全バッファを確保
        val requiredWithBuffer = requiredMB * 1.2

        Log.i(
            Constants.LOG_TAG,
            "メモリ確認: 利用可能=${availableMemoryMB}MB, 必要=${requiredMB}MB (バッファ込み=${requiredWithBuffer.toLong()}MB)"
        )

        return availableMemoryMB > requiredWithBuffer
    }

    /**
     * 現在のメモリ使用状況をログ出力
     *
     * デバッグやトラブルシューティング用
     */
    fun logMemoryStatus() {
        val runtime = Runtime.getRuntime()

        val maxMemoryMB = runtime.maxMemory() / 1024 / 1024
        val totalMemoryMB = runtime.totalMemory() / 1024 / 1024
        val freeMemoryMB = runtime.freeMemory() / 1024 / 1024
        val usedMemoryMB = totalMemoryMB - freeMemoryMB
        val availableMemoryMB = maxMemoryMB - usedMemoryMB

        Log.d(
            Constants.LOG_TAG,
            """
            |メモリ使用状況:
            |  最大メモリ: ${maxMemoryMB}MB
            |  使用中: ${usedMemoryMB}MB
            |  利用可能: ${availableMemoryMB}MB
            |  使用率: ${(usedMemoryMB * 100 / maxMemoryMB)}%
            """.trimMargin()
        )
    }
}
