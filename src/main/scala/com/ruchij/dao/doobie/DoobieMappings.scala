package com.ruchij.dao.doobie

import doobie.implicits.javasql.TimestampMeta
import doobie.util.{Get, Put}
import org.joda.time.DateTime

import java.sql.Timestamp

object DoobieMappings {

  implicit val jodaTimeGet: Get[DateTime] =
    Get[Timestamp].tmap { timestamp => new DateTime(timestamp) }

  implicit val jodaTimePut: Put[DateTime] =
    Put[Timestamp].tcontramap { dateTime => new Timestamp(dateTime.getMillis) }

}
