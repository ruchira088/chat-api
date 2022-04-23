package com.ruchij.web.requests.ws

import enumeratum.{Enum, EnumEntry}

sealed trait MessageType extends EnumEntry

object MessageType extends Enum[MessageType] {
  case object Authentication extends MessageType
  case object OneToOne extends MessageType
  case object GroupMessage extends MessageType

  override def values: IndexedSeq[MessageType] = findValues
}
