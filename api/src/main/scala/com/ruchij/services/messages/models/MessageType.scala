package com.ruchij.services.messages.models

import enumeratum.{Enum, EnumEntry}

sealed trait MessageType extends EnumEntry

object MessageType extends Enum[MessageType] {
  case object Authentication extends MessageType

  override def values: IndexedSeq[MessageType] = findValues
}
