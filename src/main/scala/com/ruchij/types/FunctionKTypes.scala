package com.ruchij.types

import cats.effect.IO
import cats.{Applicative, ApplicativeError, ~>}

import scala.concurrent.Future

object FunctionKTypes {
  type WrappedFuture[F[_], A] = F[Future[A]]

  implicit class FunctionK2TypeOps[F[+ _, + _], A, B](value: F[B, A]) {
    def toType[G[_], C >: B](implicit functionK: F[C, *] ~> G): G[A] = functionK(value)
  }

  implicit def eitherToF[L, F[_]: ApplicativeError[*[_], L]]: Either[L, *] ~> F =
    new ~>[Either[L, *], F] {
      override def apply[A](either: Either[L, A]): F[A] =
        either.fold(ApplicativeError[F, L].raiseError, Applicative[F].pure)
    }

  implicit val ioFutureToIO: WrappedFuture[IO, *] ~> IO = new ~>[WrappedFuture[IO, *], IO] {
    override def apply[A](fa: WrappedFuture[IO, A]): IO[A] = IO.fromFuture(fa)
  }

  implicit def identityFunctionK[F[_]]: F ~> F = new ~>[F, F] {
    override def apply[A](fa: F[A]): F[A] = fa
  }
}
