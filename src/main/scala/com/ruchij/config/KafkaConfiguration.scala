package com.ruchij.config

import org.http4s.Uri

case class KafkaConfiguration(bootstrapServers: String, schemaRegistry: Uri)