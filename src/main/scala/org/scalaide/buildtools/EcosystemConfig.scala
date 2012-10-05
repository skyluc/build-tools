package org.scalaide.buildtools

import java.io.File
import scala.io.Source
import scala.collection.Iterator
import scala.collection.immutable.HashMap
import scala.annotation.tailrec

object EcosystemConfig {
  import Ecosystem._

  // regex for the config file
  private val ConfigCategory = "category.([^=]*)=(.*)".r
  private val ConfigRepository = "baseRepository.([^=]*)=(.*)".r

  def apply(configFile: File): Either[String, EcosystemConfig] = {
    for {
      content <- getContent(configFile).right
      parsedContent <- parseContent(content).right
    } yield new EcosystemConfig(parsedContent._1, parsedContent._2)
  }

  private def parseContent(lines: Iterator[String]): Either[String, (Map[String, String], List[EcosystemRepository])] = {
    @tailrec
    def loop(categories: Map[String, String], repositories: List[EcosystemRepository]): Either[String, (Map[String, String], List[EcosystemRepository])] = {
      if (lines.hasNext) {
        lines.next match {
          case ConfigComment =>
            loop(categories, repositories)
          case ConfigRepository(id, location) =>
            loop(categories, repositories :+ EcosystemRepository(id, location))
          case ConfigCategory(id, name) =>
            loop(categories + (id -> name), repositories)
          case l =>
            Left("'%s' is an invalid ecosystem config line".format(l))
        }
      } else {
        Right((categories, repositories))
      }
    }
    // TODO: check if any repository defined
    loop(Map(), Nil)
  }

}

case class EcosystemConfig(categories: Map[String, String], repositories: List[EcosystemRepository])

case class EcosystemRepository(id: String, location: String) {
  def getRepository() = Repositories(location)
}