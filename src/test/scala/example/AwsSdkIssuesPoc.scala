package example

import org.reactivestreams.{Subscriber, Subscription}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.diagrams.Diagrams
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.{Logger, LoggerFactory}
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model._

import java.lang
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import java.util.function
import java.util.function.Function
import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.jdk.CollectionConverters._
import scala.util.Random

class AwsSdkIssuesPoc extends AnyFunSuite with Diagrams with BeforeAndAfterAll {

  val log: Logger = LoggerFactory.getLogger(getClass)

  val container: GenericContainer[_] = {
    val c = new GenericContainer(DockerImageName.parse("amazon/dynamodb-local:latest"))
    c.addExposedPorts(8000)
    c.start()
    c
  }

  val client: DynamoDbAsyncClient =
    DynamoDbAsyncClient
      .builder()
      .endpointOverride(URI.create(s"http://localhost:${container.getFirstMappedPort}"))
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
      .region(Region.AP_NORTHEAST_1)
      .build()

  val tableName = "Poc"

  val PKName = "PK"
  val SKName = "SK"

  def key(pk: Int, sk: Int): java.util.Map[String, AttributeValue] = Map(
    PKName -> AttributeValue.builder().n(pk.toString).build(),
    SKName -> AttributeValue.builder().n(sk.toString).build()
  ).asJava

  def makeQueryWithData(pk: Int): QueryRequest = {
    val item1 = key(pk, Random.nextInt(1000))
    client.putItem(PutItemRequest.builder().tableName(tableName).item(item1).build()).get()

    val item2 = key(pk, Random.nextInt(1000))
    client.putItem(PutItemRequest.builder().tableName(tableName).item(item2).build()).get()

    QueryRequest
      .builder()
      .tableName(tableName)
      .keyConditionExpression(s"$PKName = :pk")
      .expressionAttributeValues(Map(":pk" -> AttributeValue.builder().n(pk.toString).build()).asJava)
      .build()
  }

  override def beforeAll(): Unit = {
    val request =
      CreateTableRequest
        .builder()
        .tableName(tableName)
        .attributeDefinitions(
          AttributeDefinition.builder().attributeName(PKName).attributeType(ScalarAttributeType.N).build(),
          AttributeDefinition.builder().attributeName(SKName).attributeType(ScalarAttributeType.N).build()
        )
        .keySchema(
          KeySchemaElement.builder().attributeName(PKName).keyType(KeyType.HASH).build(),
          KeySchemaElement.builder().attributeName(SKName).keyType(KeyType.RANGE).build()
        )
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .build()

    client.createTable(request).get()
  }

  override def afterAll(): Unit = {
    client.close()
    container.close()
  }

  class MyException(throwable: Throwable) extends Exception(throwable)

  def failOnNextSubscriber[E](p: Promise[Unit]): Subscriber[E] =
    new Subscriber[E] {
      override def onSubscribe(s: Subscription): Unit = {
        log.info(s"onSubscribe: $s")
        s.request(Long.MaxValue)
      }

      override def onNext(t: E): Unit = {
        log.info(s"onNext: $t")
        throw new Exception("Error onNext") // An error is occurred
      }

      override def onError(t: Throwable): Unit = {
        log.info(s"onError: ${t.getMessage}")
        p.failure(new MyException(t))
      }

      override def onComplete(): Unit = {
        log.info(s"onComplete")
        p.success(())
      }
    }

  test("The `ResponseSubscription` silently stops when a error has occurred on `Subscriber.onNext`") {
    val query = makeQueryWithData(1)

    val p = Promise[Unit]()
    client
      .queryPaginator(query)
      .subscribe(failOnNextSubscriber[QueryResponse](p))
    assertThrows[MyException](Await.result(p.future, 5.seconds)) // It silently stops, so be timeout.
  }

  test("The `ItemsSubscription silently stops when a error has occurred on `Subscriber.onNext`") {
    val query = makeQueryWithData(2)

    val p = Promise[Unit]()
    client
      .queryPaginator(query)
      .items()
      .map(_.asScala.toMap)
      .subscribe(failOnNextSubscriber[Map[String, AttributeValue]](p))
    assertThrows[MyException](Await.result(p.future, 5.seconds)) // It silently stops, so be timeout.
  }

  test(
    "The `FlatteningSubscriber` and `MappingSubscriber` tells `Subscriber.onError` the error that occurred in `Subscriber.onNext`."
  ) {
    val query = makeQueryWithData(3)

    val p = Promise[Unit]()
    client
      .queryPaginator(query)
      .flatMapIterable(new Function[QueryResponse, java.lang.Iterable[Map[String, AttributeValue]]] {
        override def apply(t: QueryResponse): lang.Iterable[Map[String, AttributeValue]] =
          t.items().asScala.map(_.asScala.toMap).asJava
      })
      .subscribe(failOnNextSubscriber[Map[String, AttributeValue]](p))
    assertThrows[MyException](Await.result(p.future, 5.seconds))
  }

}
