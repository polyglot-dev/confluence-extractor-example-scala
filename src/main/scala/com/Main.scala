package com

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.{JsonFraming, Source}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.async.Async._
import scala.concurrent.Future
//import org.json4s.JsonDSL

import java.nio.file.Paths
import java.nio.file.StandardOpenOption._

import akka.stream.scaladsl.{FileIO, Sink}

case class ScrapedPage(//id: String,
                       title: String,
                       // content: String,
                       url: String
                      )

case class Attachment(//id: String,
                      title: String,
                      // content: String,
                      url: String
                     )

object Main
  extends App {

  implicit class RichString(string: String) {
    def parseToJson: JValue = parse(string)
  }

  val token = ""
  val domain = ""
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val confluenceContentType = "page"
  // val confluenceContentType = "blogpost"

  import org.json4s.JsonDSL._

  def prepareRequest(url: String): HttpRequest = HttpRequest(method = HttpMethods.GET, uri = url)

  def mkRequest(request: HttpRequest) =
    Http().singleRequest(request.withHeaders(RawHeader("Authorization", s"Basic $token")))

  val ms: Sink[(JValue, JValue), Future[(JValue, JValue)]] = Sink.head[(JValue, JValue)]
  var nextPage: JValue = _
  var nurl = s"https://$domain.atlassian.net/wiki/rest/api/content?type=$confluenceContentType&limit=12&expand=body.view,children.attachment,children.comment,metadata.labels"
  val results = scala.collection.mutable.ArrayBuffer[Attachment]()
  async {
    do {
      // println(s"Iterando con: $nurl")

      val response: HttpResponse = await(mkRequest(prepareRequest(nurl)))
      val rdata: Source[JValue, Any] = response.entity.dataBytes
        .via(JsonFraming.objectScanner(10000000)).map(_.decodeString("UTF8").parseToJson)

      def valuesSelector(e: JValue) =
        (e \ "_links" \ "next",
          JArray(for {
            JObject(child) <- e
            JField("title", JString(title)) <- child
            JField("id", JString(id)) <- child
            JField("body", JObject(body)) <- child
            JField("view", JObject(view)) <- body
            JField("value", JString(value)) <- view
            JField("_links", JObject(links)) <- child
            JField("children", JObject(children)) <- child
            JField("attachment", JObject(attachment)) <- children
            JField("results", JArray(results)) <- attachment
            JField("self", JString(url)) <- links
          } yield JObject(
            //"title" -> title
            "id" -> id
            , "attachments" -> JArray(for {
              rr <- results
              JObject(oattachment) <- rr
              JField("id", id_att) <- oattachment
              JField("title", title_att) <- oattachment
              JField("_links", JObject(attachments_links)) <- oattachment
              JField("download", attachments_url) <- attachments_links
            } yield JObject("id" -> id_att, "title" -> title_att, "url" -> attachments_url))
            //            , "value" -> value
            //            , "url" -> url
          ))
        )

      val firstElement = rdata.take(1)
      val firstValueFuture = firstElement.map(valuesSelector).runWith(ms)

      val firstValue = await(firstValueFuture)
      nextPage = firstValue._1
      // println(firstValue._2)
      // println(nextPage)

      for {
        e <- firstValue._2.asInstanceOf[JArray].values
        aa <- e.asInstanceOf[Map[String, List[Map[String, String]]]].get("attachments")
        fff <- aa
        title <- fff.get("title")
        url <- fff.get("url")
      } {
        results += Attachment(title, url)
      }

      nurl = s"https://$domain.atlassian.net/wiki${nextPage.values.toString}"

    } while (nextPage.values != None)

    var i = 0
    val total = results.size
    while (i < total) {
      println(s"Downloading attachment #$i")
      val sink: Sink[ByteString, Future[IOResult]] =
        FileIO.toPath(Paths.get(s"tmp/${results(i).title}"), Set(CREATE, WRITE, APPEND))

      val redReq = await(mkRequest(prepareRequest(s"https://skyesearch.atlassian.net/wiki${results(i).url}")))
      val file = await(mkRequest(prepareRequest(redReq.getHeader("Location").get.value)))
      await(file.entity.dataBytes.runWith(sink))

      i += 1
    }

    // println(results)

    system.terminate()

  }

  //.runWith(Sink.foreach(println))

  //.map(transformEachLine)
  // .runWith(FileIO.toPath(new File("example.json").toPath))
  //system.terminate()


}
