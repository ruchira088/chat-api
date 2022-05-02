package com.ruchij.kv

import cats.MonadThrow
import cats.implicits._
import com.ruchij.kv.codecs.KVEncoder

class KeySpacedKeyValueStore[F[_]: MonadThrow, K, V](
  val keyValueStore: KeyValueStore[F],
  keySpace: KeySpace[K, V]
) {
  import keySpace._

  private def deriveKey(key: K): F[String] =
    KVEncoder[F, K].encode(key).map(encodedKey => s"${keySpace.name}-$encodedKey")

  def insert(key: K, value: V): F[keyValueStore.InsertionResult] =
    deriveKey(key).flatMap(keySpacedKey => keyValueStore.insert(keySpacedKey, value))

  def find(key: K): F[Option[V]] =
    deriveKey(key).flatMap(keySpacedKey => keyValueStore.find(keySpacedKey))

  def delete(key: K): F[keyValueStore.DeletionResult] =
    deriveKey(key).flatMap(keySpacedKey => keyValueStore.delete(keySpacedKey))
}
