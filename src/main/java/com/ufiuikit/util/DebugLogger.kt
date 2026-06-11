package com.ufiuikit.util

import android.util.Log

/**
 * UI Kit 内置日志工具。
 * 默认使用 [Log] 输出，宿主项目可替换 [logger] 实现自定义日志。
 */
internal object DebugLogger {

    /** 可替换的日志实现 */
    var logger: Logger = DefaultLogger()

    interface Logger {
        fun w(tag: String, msg: String)
        fun e(tag: String, msg: String)
    }

    private class DefaultLogger : Logger {
        override fun w(tag: String, msg: String) { Log.w("[UIKit] $tag", msg) }
        override fun e(tag: String, msg: String) { Log.e("[UIKit] $tag", msg) }
    }

    fun w(tag: String, msg: String) = logger.w(tag, msg)
    fun logExc(tag: String, msg: String) = logger.e(tag, msg)
}
