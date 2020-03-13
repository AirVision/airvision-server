/*
 * AirVision
 *
 * Copyright (c) AirVision <https://www.github.com/AirVision>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package io.github.airvision.logging

import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.pattern.ConverterKeys
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter
import org.apache.logging.log4j.core.pattern.PatternConverter
import org.apache.logging.log4j.io.LoggerPrintStream
import java.io.PrintStream
import java.util.regex.Matcher

@ConverterKeys("loc")
@Plugin(name = "LocationPatternConverter", category = PatternConverter.CATEGORY)
internal class LocationPatternConverter private constructor(private val format: String) :
    LogEventPatternConverter("Location", "location") {

  // Packages that will be ignored
  private val ignoredPackages = arrayOf("java.", "kotlin.io.")
  private val pathRegex = "%path".toRegex()

  override fun format(event: LogEvent, builder: StringBuilder) {
    val element = calculateLocation(event.loggerFqcn)
    if (element != null) {
      // quoteReplacement is required for elements leading to inner class (containing a $ character)
      builder.append(format.replace(pathRegex,
          Matcher.quoteReplacement(element.toString())))
    }
  }

  private fun calculateLocation(fqcn: String): StackTraceElement? {
    val stackTrace = Throwable().stackTrace
    var last: StackTraceElement? = null

    for (i in stackTrace.size - 1 downTo 1) {
      val className = stackTrace[i].className
      // Check if the target logger source should be redirected
      if (redirectFqcns.contains(className) || className == fqcn)
        return last
      // Reaching the printStackTrace method is also the end of the road
      if (className == "java.lang.Throwable" && stackTrace[i].methodName == "printStackTrace")
        return null
      // Ignore Kotlin and Java packages
      if (!ignoredPackages.any { className.startsWith(it) })
        last = stackTrace[i]
    }

    return null
  }

  companion object {

    private val redirectFqcns = setOf(
        PrintStream::class.qualifiedName,
        LoggerPrintStream::class.qualifiedName)

    @JvmStatic
    fun newInstance(options: Array<String>): LocationPatternConverter =
        LocationPatternConverter(if (options.isNotEmpty()) options[0] else "%path")
  }
}
