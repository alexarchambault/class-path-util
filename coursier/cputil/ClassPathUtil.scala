//> using scala "2.12.17", "2.13.10", "3.2.2"
//> using jvm "8"

package coursier.cputil

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.regex.{Matcher, Pattern}

import scala.collection.JavaConverters._

object ClassPathUtil {

  private implicit class CustomStringOps(private val str: String) extends AnyVal {
    def endsWithIgnoreCase(suffix: String): Boolean =
      str.length >= suffix.length &&
        suffix.compareToIgnoreCase(str.substring(str.length - suffix.length)) == 0
  }

  private val propertyRegex = Pattern.compile(
    Pattern.quote("${") + "[^" + Pattern.quote("{[()]}") + "]*" + Pattern.quote("}")
  )

  def classPath(input: String): Seq[Path] =
    classPath(input, sys.props.get)

  def classPath(input: String, getProperty: String => Option[String]): Seq[Path] =
    input.split(File.pathSeparator).filter(_.nonEmpty).flatMap { elem =>
      val processedElem = {
        var value            = elem
        var matcher: Matcher = null

        while ({
          matcher = propertyRegex.matcher(value)
          matcher.find()
        }) {
          val start    = matcher.start(0)
          val end      = matcher.end(0)
          val subKey   = value.substring(start + 2, end - 1)
          val subValue = getProperty(subKey).getOrElse("")
          value = value.substring(0, start) + subValue + value.substring(end)
        }

        value
      }
      def allJarsOf(dir: Path): Seq[Path] =
        Files.list(dir)
          .iterator
          .asScala
          .filter(_.toString.endsWithIgnoreCase(".jar"))
          .toVector
          .sortBy(_.getFileName)
      val allJarsSuffixes = Seq("/", File.separator)
        .distinct
        .flatMap(sep => Seq("*", "*.jar").map(sep + _))
      allJarsSuffixes.find(processedElem.endsWithIgnoreCase) match {
        case Some(suffix) =>
          val dir = Paths.get(processedElem.substring(0, processedElem.length - suffix.length))
          allJarsOf(dir)
        case None =>
          Seq(Paths.get(processedElem))
      }
    }

}
