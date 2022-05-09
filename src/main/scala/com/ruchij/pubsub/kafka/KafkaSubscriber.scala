package com.ruchij.pubsub.kafka

import cats.effect.kernel.{Resource, Sync}
import cats.~>
import com.ruchij.config.KafkaConfiguration
import com.ruchij.pubsub.Subscriber
import com.ruchij.pubsub.models.CommittableRecord
import com.ruchij.services.messages.models.Message
import com.ruchij.types.FunctionKTypes.WrappedFuture
import fs2.Stream
import io.confluent.kafka.serializers.{AbstractKafkaSchemaSerDeConfig, KafkaAvroDeserializer, KafkaAvroDeserializerConfig}
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, OffsetAndMetadata, OffsetCommitCallback}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer

import java.time.Duration
import java.util
import java.util.Properties
import scala.concurrent.Promise
import scala.jdk.CollectionConverters._

class KafkaSubscriber[F[_]: Sync, A <: Message, B <: SpecificRecord](
  kafkaConfiguration: KafkaConfiguration,
  kafkaTopic: KafkaTopic[A, B]
)(implicit futureUnwrapper: WrappedFuture[F, *] ~> F)
    extends Subscriber[F, A] {

  private def consumerProperties(groupId: String): Properties =
    new Properties() {
      put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.bootstrapServers)
      put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaConfiguration.schemaRegistry.renderString)

      put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
      put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[KafkaAvroDeserializer].getName)

      put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
      put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
      put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    }

  override def subscribe(groupId: String): Stream[F, CommittableRecord[F, A]] =
    Stream
      .resource {
        Resource.make[F, KafkaConsumer[String, B]](
          Sync[F].delay(new KafkaConsumer[String, B](consumerProperties(groupId)))
        ) { kafkaConsumer =>
          Sync[F].blocking(kafkaConsumer.close())
        }
      }
      .evalTap { kafkaConsumer =>
        Sync[F].blocking(kafkaConsumer.subscribe(List(kafkaTopic.name).asJavaCollection))
      }
      .flatMap { kafkaConsumer =>
        Stream
          .eval(Sync[F].blocking(kafkaConsumer.poll(Duration.ofMillis(100))))
          .flatMap { consumerRecords =>
            Stream.emits(consumerRecords.iterator().asScala.toSeq)
          }
          .repeat
          .map { consumerRecord =>
            CommittableRecord(
              kafkaTopic.fromSpecificRecord(consumerRecord.value()),
              futureUnwrapper.apply {
                Sync[F].blocking {
                  val promise = Promise[Unit]()

                  kafkaConsumer.commitAsync(
                    Map(
                      new TopicPartition(consumerRecord.topic(), consumerRecord.partition()) -> new OffsetAndMetadata(
                        consumerRecord.offset()
                      )
                    ).asJava,
                    new OffsetCommitCallback {
                      override def onComplete(
                        offsets: util.Map[TopicPartition, OffsetAndMetadata],
                        exception: Exception
                      ): Unit = {
                        if (exception == null) promise.success((): Unit) else promise.failure(exception)
                      }
                    }
                  )

                  promise.future
                }
              }
            )
          }
      }

}
