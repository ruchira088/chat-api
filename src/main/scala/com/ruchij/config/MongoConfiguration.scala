package com.ruchij.config

case class MongoConfiguration(hosts: String, user: Option[String], password: Option[String], database: String) {
  val connectionUrl: Either[IllegalArgumentException, String] =
    (user, password) match {
      case (None, None) => Right(s"mongodb://$hosts")
      case (Some(userValue), Some(passwordValue)) => Right(s"mongodb://$userValue:$passwordValue@$hosts")

      case _ =>
        Left(new IllegalArgumentException(s"Only the ${if (user.isEmpty) "password" else "user"} is specified"))
    }
}
