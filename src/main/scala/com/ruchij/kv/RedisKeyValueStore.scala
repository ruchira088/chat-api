package com.ruchij.kv

import cats.Monad
import cats.data.OptionT
import cats.implicits._
import com.ruchij.kv.codecs.{KVDecoder, KVEncoder}
import dev.profunktor.redis4cats.RedisCommands

class RedisKeyValueStore[F[_]: Monad](redisCommands: RedisCommands[F, String, String]) extends KeyValueStore[F] {
  type InsertionResult = Unit
  type DeletionResult = Long

  def insert[K: KVEncoder[F, *], V: KVEncoder[F, *]](key: K, value: V): F[InsertionResult] =
    for {
      encodedKey <- KVEncoder[F, K].encode(key)
      encodedValue <- KVEncoder[F, V].encode(value)
      result <- redisCommands.set(encodedKey, encodedValue)
    } yield result

  def find[K: KVEncoder[F, *], V: KVDecoder[F, *]](key: K): F[Option[V]] =
    for {
      encodedKey <- KVEncoder[F, K].encode(key)
      result <- OptionT(redisCommands.get(encodedKey)).semiflatMap(KVDecoder[F, V].decode).value
    } yield result

  def delete[K: KVEncoder[F, *]](key: K): F[DeletionResult] =
    for {
      encodedKey <- KVEncoder[F, K].encode(key)
      result <- redisCommands.del(encodedKey)
    } yield result
}
