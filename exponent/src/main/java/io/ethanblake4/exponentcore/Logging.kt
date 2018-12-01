package io.ethanblake4.exponentcore

import io.ethanblake4.exponentcore.model.internal.LogLevel

 object Logging {
     @JvmStatic @JvmOverloads fun d(message: String, err: Throwable? = null) = Exponent.logger(message, LogLevel.DEBUG, err)
     @JvmStatic @JvmOverloads fun i(message: String, err: Throwable? = null) = Exponent.logger(message, LogLevel.INFO, err)
     @JvmStatic @JvmOverloads fun w(message: String, err: Throwable? = null) = Exponent.logger(message, LogLevel.WARN, err)
     @JvmStatic @JvmOverloads fun e(message: String, err: Throwable? = null) = Exponent.logger(message, LogLevel.ERROR, err)
}