package com.ruchij.kv.codecs

trait KVDecoder[F[_], +A] {
  def decode[C >: A](value: String): F[C]
}

object KVDecoder {
  def apply[F[_], A](implicit kvDecoder: KVDecoder[F, A]): KVDecoder[F, A] = kvDecoder
}