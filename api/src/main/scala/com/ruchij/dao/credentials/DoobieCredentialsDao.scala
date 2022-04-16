package com.ruchij.dao.credentials

import com.ruchij.dao.credentials.models.Credentials
import com.ruchij.dao.doobie.DoobieMappings.{jodaTimeGet, jodaTimePut}
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

object DoobieCredentialsDao extends CredentialsDao[ConnectionIO] {

  override def insert(credentials: Credentials): ConnectionIO[Int] =
    sql"""
        INSERT INTO credentials (user_id, created_at, last_updated_at, salted_hashed_password)
            VALUES (
                ${credentials.userId},
                ${credentials.createdAt},
                ${credentials.lastUpdatedAt},
                ${credentials.saltedHashedPassword}
            )
    """
      .update
      .run

  override def findByUserId(userId: String): ConnectionIO[Option[Credentials]] =
    sql"""
        SELECT user_id, created_at, last_updated_at, salted_hashed_password
            FROM credentials WHERE user_id = $userId
    """
      .query[Credentials]
      .option
}
