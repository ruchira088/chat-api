package com.ruchij.kv

import cats.Monad
import cats.implicits._
import com.ruchij.kv.codecs.{KVDecoder, KVEncoder}

class KeySpacedKeyValueStore[F[_]: Monad, K: KVEncoder[F, *], V: KVEncoder[F, *]: KVDecoder[F, *]](
  val keyValueStore: KeyValueStore[F],
  keySpace: KeySpace[K, V]
) {
  private def deriveKey(key: K): F[String] =
    KVEncoder[F, K].encode(key).map(encodedKey => s"${keySpace.name}-$encodedKey")

  def insert(key: K, value: V): F[keyValueStore.InsertionResult] =
    deriveKey(key).flatMap(keySpacedKey => keyValueStore.insert(keySpacedKey, value))

  def find(key: K): F[Option[V]] =
    deriveKey(key).flatMap(keySpacedKey => keyValueStore.find(keySpacedKey))

  def delete(key: K): F[keyValueStore.DeletionResult] =
    deriveKey(key).flatMap(keySpacedKey => keyValueStore.delete(keySpacedKey))
}
