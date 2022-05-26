package com.ruchij.dao.profile

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

object DoobieProfileImageDao extends ProfileImageDao[ConnectionIO] {

  override def insert(userId: String, fileId: String): ConnectionIO[Int] =
    sql"INSERT INTO profile_images (user_id, file_id) VALUES ($userId, $fileId)".update.run

}
