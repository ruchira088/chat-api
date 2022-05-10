package com.ruchij.web.ws

import enumeratum.{Enum, EnumEntry}

sealed trait MessageType extends EnumEntry

object MessageType extends Enum[MessageType] {
  case object Authentication extends MessageType
  case object OneToOne extends MessageType
  case object GroupMessage extends MessageType
  case object HeartBeat extends MessageType

  override def values: IndexedSeq[MessageType] = findValues
}
