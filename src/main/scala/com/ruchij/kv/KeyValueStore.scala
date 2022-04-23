package com.ruchij.kv

import com.ruchij.kv.codecs.{KVDecoder, KVEncoder}

trait KeyValueStore[F[_]] {
  type InsertionResult
  type DeletionResult

  def insert[K: KVEncoder[F, *], V: KVEncoder[F, *]](key: K, value: V): F[InsertionResult]

  def find[K: KVEncoder[F, *], V: KVDecoder[F, *]](key: K): F[Option[V]]

  def delete[K: KVEncoder[F, *]](key: K): F[DeletionResult]
}