package com.ruchij.dao.doobie

import doobie.implicits.javatimedrivernative.JavaZonedDateTimeMeta
import doobie.util.{Get, Put}
import org.joda.time.{DateTime, DateTimeZone}

import java.time.{Instant, ZoneId, ZonedDateTime}

object DoobieMappings {

  implicit val jodaTimeGet: Get[DateTime] =
    Get[ZonedDateTime].map { zonedDateTime =>
      new DateTime(zonedDateTime.toEpochSecond, DateTimeZone.forID(zonedDateTime.getZone.getId))
    }

  implicit val jodaTimePut: Put[DateTime] =
    Put[ZonedDateTime].tcontramap { dateTime =>
      ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateTime.getMillis), ZoneId.of(dateTime.getZone.getID))
    }

}
