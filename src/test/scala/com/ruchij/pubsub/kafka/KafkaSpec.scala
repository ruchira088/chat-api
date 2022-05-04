package com.ruchij.pubsub.kafka

import cats.effect.{IO, Resource}
import cats.implicits._
import com.ruchij.avro.chat.OneToOneMessage
import com.ruchij.external.embedded.EmbeddedExternalServiceProvider
import com.ruchij.services.messages.models.UserMessage.OneToOne
import com.ruchij.test.utils.IOUtils.runIO
import com.ruchij.types.FunctionKTypes.ioFutureToIO
import com.ruchij.types.JodaClock
import fs2.Stream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class KafkaSpec extends AnyFlatSpec with Matchers {

  "KafkaPublisher and KafkaSubscriber" should "be able to publish and subscribe to topics" in runIO {
    new EmbeddedExternalServiceProvider[IO].kafkaConfiguration.flatMap {
      kafkaConfiguration =>
        KafkaPublisher.createProducer[IO](kafkaConfiguration)
          .map(producer => new KafkaPublisher[IO, OneToOne, OneToOneMessage](producer, KafkaTopic.OneToOneMessageTopic))
          .product {
            Resource.pure(new KafkaSubscriber[IO, OneToOne, OneToOneMessage](kafkaConfiguration, KafkaTopic.OneToOneMessageTopic))
          }
    }.use {
      case (kafkaPublisher, kafkaSubscriber) =>
        kafkaSubscriber.subscribe("test-consumer").take(10).compile.toList.start
          .productL {
            kafkaPublisher.publish {
              Stream.range[IO, Int](0, 10)
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
          .flatMap { records =>
            IO.delay {
              records.size mustBe 10
              all(records.map(_.data.senderId)) mustBe "my-sender"
              all(records.map(_.data.receiverId)) mustBe "my-receiver"
              records.map(_.data.messageId) mustBe Range(0, 10).map(_.toString)
              records.map(_.data.message) mustBe Range(0, 10).map(index => s"Hello World $index")
              records.map(_.data.sentAt.getMillis) mustBe sorted
            }
          }
    }
  }

}
