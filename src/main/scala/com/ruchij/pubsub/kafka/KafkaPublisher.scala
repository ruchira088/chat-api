package com.ruchij.pubsub.kafka

import cats.effect.kernel.Resource
import cats.effect.{Async, Sync}
import cats.implicits.toFunctorOps
import cats.~>
import com.ruchij.config.KafkaConfiguration
import com.ruchij.pubsub.Publisher
import com.ruchij.types.FunctionKTypes.WrappedFuture
import fs2.Pipe
import io.confluent.kafka.serializers.{AbstractKafkaSchemaSerDeConfig, KafkaAvroSerializer}
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties
import scala.concurrent.Promise

class KafkaPublisher[F[_]: Async, A, B <: SpecificRecord](kafkaProducer: KafkaProducer[String, SpecificRecord], topic: KafkaTopic[A, B])(
  implicit futureUnwrapper: WrappedFuture[F, *] ~> F
) extends Publisher[F, A] {

  override val publish: Pipe[F, A, Unit] =
    data =>
      data.evalMap { value =>
        val producerRecord =
          new ProducerRecord[String, SpecificRecord](topic.name, topic.key(value), topic.toSpecificRecord(value))

        val wrappedFuture: WrappedFuture[F, RecordMetadata] =
          Sync[F].blocking {
            val promise = Promise[RecordMetadata]()

            kafkaProducer.send(producerRecord, new Callback {
              override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit =
                if (exception == null) promise.success(metadata) else promise.failure(exception)
            })

            promise.future
          }

        futureUnwrapper.apply(wrappedFuture).as((): Unit)
    }

}

object KafkaPublisher {
  def createProducer[F[_]: Sync](
    kafkaConfiguration: KafkaConfiguration
  ): Resource[F, KafkaProducer[String, SpecificRecord]] = {
    val properties =
      new Properties() {
        setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.bootstrapServers)
        setProperty(
          AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
          kafkaConfiguration.schemaRegistry.renderString
        )

        setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
        setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)
      }

    Resource.make(Sync[F].blocking(new KafkaProducer[String, SpecificRecord](properties))) { kafkaProducer =>
      Sync[F].blocking {
        kafkaProducer.flush()
        kafkaProducer.close()
      }
    }
  }
}
