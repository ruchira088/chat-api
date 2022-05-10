package com.ruchij.pubsub.kafka

import cats.effect.{IO, Resource}
import cats.implicits._
import com.ruchij.avro.chat.OneToOneMessage
import com.ruchij.external.embedded.EmbeddedExternalServiceProvider
import com.ruchij.pubsub.models.CommittableRecord
import com.ruchij.services.messages.models.Message.OneToOne
import com.ruchij.test.utils.IOUtils.runIO
import com.ruchij.types.FunctionKTypes.ioFutureToIO
import com.ruchij.types.JodaClock
import fs2.Stream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class KafkaSpec extends AnyFlatSpec with Matchers {

  "KafkaPublisher and KafkaSubscriber" should "be able to publish and subscribe to topics" in runIO {
    new EmbeddedExternalServiceProvider[IO].kafkaConfiguration
      .flatMap { kafkaConfiguration =>
        KafkaPublisher
          .createProducer[IO](kafkaConfiguration)
          .map(producer => new KafkaPublisher[IO, OneToOne, OneToOneMessage](producer, KafkaTopic.OneToOneMessageTopic))
          .product {
            Resource.pure(
              new KafkaSubscriber[IO, OneToOne, OneToOneMessage](kafkaConfiguration, KafkaTopic.OneToOneMessageTopic)
            )
          }
      }
      .use {
        case (kafkaPublisher, kafkaSubscriber) =>
          kafkaSubscriber
            .subscribe("test-consumer")
            .zip(Stream.range[IO, Int](0, 10))
            .take(10)
            .evalMap {
              case (CommittableRecord(oneToOne, commit), index) =>
                IO.delay {
                    oneToOne.senderId mustBe "my-sender"
                    oneToOne.receiverId mustBe "my-receiver"
                    oneToOne.content mustBe s"Hello World $index"
                    oneToOne.messageId mustBe index.toString
                  }
                  .product(commit)
                  .as(oneToOne)
            }
            .compile
            .toList
            .flatMap { oneToOneMessages =>
              IO.delay {
                oneToOneMessages.size mustBe 10
                oneToOneMessages.map(_.sentAt.getMillis) mustBe sorted
              }
            }
            .start
            .productL {
              kafkaPublisher
                .publish {
                  Stream
                    .range[IO, Int](0, 10)
                    .evalMap { index =>
                      JodaClock[IO].timestamp.map { timestamp =>
                        OneToOne(index.toString, "my-sender", timestamp, "my-receiver", s"Hello World $index")
                      }
                    }
                }
                .compile
                .drain
            }
            .flatMap(_.joinWithNever)
      }
  }
}
