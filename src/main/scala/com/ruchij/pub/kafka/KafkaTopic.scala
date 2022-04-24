package com.ruchij.pub.kafka

import org.apache.avro.specific.SpecificRecord

trait KafkaTopic[A] {
  val name: String

  def toSpecificRecord(value: A): SpecificRecord

  def key(value: A): String
}
