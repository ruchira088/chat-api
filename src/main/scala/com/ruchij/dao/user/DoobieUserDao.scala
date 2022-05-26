package com.ruchij.dao.user

import com.ruchij.dao.user.models.{Email, User}
import com.ruchij.dao.doobie.DoobieMappings.{jodaTimeGet, jodaTimePut}
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

object DoobieUserDao extends UserDao[ConnectionIO] {
  private val FindQuery =
    fr"""
      SELECT
          chat_users.id, chat_users.created_at, chat_users.email, chat_users.first_name, chat_users.last_name,
          profile_images.file_id
        FROM chat_users
        LEFT JOIN profile_images ON profile_images.user_id = chat_users.id
    """

  override def insert(user: User): ConnectionIO[Int] =
    sql"""
      INSERT INTO chat_users (id, created_at, email, first_name, last_name)
          VALUES(
              ${user.id},
              ${user.createdAt},
              ${user.email},
              ${user.firstName},
              ${user.lastName}
          )
    """
      .update
      .run

  override def findByUserId(userId: String): ConnectionIO[Option[User]] =
    (FindQuery ++ fr"WHERE id = $userId").query[User].option

  override def findByEmail(email: Email): ConnectionIO[Option[User]] =
    (FindQuery ++ fr"WHERE email = $email").query[User].option

  override def search(searchTerm: String, pageNumber: Int, pageSize: Int): ConnectionIO[Seq[User]] =
    (FindQuery ++
      fr"""
        WHERE
          email = ${s"%$searchTerm%"}
          OR first_name = ${s"%$searchTerm%"}
          OR last_name = ${s"%$searchTerm%"}
        ORDER BY created_at
        LIMIT $pageSize OFFSET ${pageSize * pageNumber}
      """)
      .query[User].to[Seq]
}
