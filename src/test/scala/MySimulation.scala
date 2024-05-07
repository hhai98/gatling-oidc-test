import scala.concurrent.duration._

import java.nio.file.Paths
import java.security._
import javax.crypto.spec._
import scala.util.Random
import java.security.MessageDigest
import java.util.Base64
import java.net.URLEncoder

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class MySimulation extends Simulation {
  val codeVerifier = Random.alphanumeric.filter(c => c.isLetterOrDigit || "-._~".contains(c)).take(43).mkString
  val codeChallenge = Base64.getUrlEncoder.withoutPadding.encodeToString(MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes("UTF-8")))
  val clientId = "9bb7af57-e5f0-437f-9744-b05d103e7a1f"
  val redirectUri = "http://localhost/auth/callback"
  val host = "http://demo-htb-one-id-453949642.ap-southeast-1.elb.amazonaws.com"
  // val host = "http://localhost:6785"
  // val host = "http://54.179.114.251"

  val params = Map(
    "client_id" -> clientId,
    "redirect_uri" -> redirectUri,
    "response_type" -> "code",
    "state" -> codeVerifier,
    "code_challenge" -> codeChallenge,
    "code_challenge_method" -> "S256"
  )

  val query = params.map { case (key, value) =>
    URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
  }.mkString("&")

  private val httpProtocol = http
    .baseUrl(host)
    .inferHtmlResources(AllowList(), DenyList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*\.svg""", """.*detectportal\.firefox\.com.*"""))
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .upgradeInsecureRequestsHeader("1")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:124.0) Gecko/20100101 Firefox/124.0")

  private val usersCount = 1

  private val headers = Map(
    "Accept" -> "application/json",
  )

  // get user email from json file
  val userFeeder = jsonFile("./user_email.json")

  // add khoảng nghỉ giữa các request pause(10)

  private val scn = scenario("Oauth2 Flow PKCE")
    .feed(userFeeder)
    .exec { session =>
      // println(host + "/oauth/authorize?" + query)
      session
    }
    .exec(
      http("oauth/authorize")
      .get("/oauth/authorize?" + query)
      .disableFollowRedirect
      .check(status.is(302).saveAs("GOOD"))
    )
    .stopInjectorIf(session => "Error oauth/authorize", session => !session.attributes.contains("GOOD"))
    .doIf(session => session.attributes.contains("GOOD")) {
      exec(session => {
        session.remove("GOOD")
      })
    }
    .pause(10)
    .exec(
      http("Login")
      .post("/login")
      .disableFollowRedirect
      .formParam("email", "#{email}")
      .formParam("password", "password")
      .check(header("Location").saveAs("redirect_1"))
      .check(status.is(302).saveAs("GOOD"))
    )
    .stopInjectorIf(session => "Error Login", session => !session.attributes.contains("GOOD"))
    .doIf(session => session.attributes.contains("GOOD")) {
      exec(session => {
        session.remove("GOOD")
      })
    }
    .exec(
      http("Callback redirect")
      .get("#{redirect_1}")
      .disableFollowRedirect
      .check(header("Location").saveAs("redirect_2"))
      .check(status.is(302).saveAs("GOOD"))
    )
    .stopInjectorIf(session => "Error Callback redirect", session => !session.attributes.contains("GOOD"))
    .doIf(session => session.attributes.contains("GOOD")) {
      exec(session => {
        session.remove("GOOD")
      })
    }
    .exec { session =>
      val redirect2Url = session("redirect_2").as[String]
      val codeRegex = "code=([^&]+)".r
      val stateRegex = "state=([^&]+)".r
      val code = codeRegex.findFirstMatchIn(redirect2Url).map(_.group(1))
      val state = stateRegex.findFirstMatchIn(redirect2Url).map(_.group(1))

      session.set("code", code.getOrElse(""))
        .set("code_verifier", state.getOrElse(""))
    }
    .pause(2)
    .exec(
      http("Exchange Token")
        .post(host + "/oauth/token-2")
        .formParam("grant_type", "authorization_code")
        .formParam("client_id", clientId)
        .formParam("redirect_uri", redirectUri)
        .formParam("code", "#{code}")
        .formParam("code_verifier", "#{code_verifier}")
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("GOOD"))
    )
    .stopInjectorIf(session => "Error Exchange Token", session => !session.attributes.contains("GOOD"))
    .doIf(session => session.attributes.contains("GOOD")) {
      exec(session => {
        session.remove("GOOD")
      })
    }
    // .stopInjectorIf(session => "Error Exchange Token", session => true)
    // .exitHereIfFailed
    // .exec { session =>
    //   // println("redirect_1:")
    //   // println(session("redirect_1").as[String])
    //   // println("redirect_2:")
    //   // println(session("redirect_2").as[String])
    //   // println("code:")
    //   // println(session("code").as[String])
    //   // println("code_verifier:")
    //   // println(session("code_verifier").as[String])
    //   // println("access_token:")
    //   // println(session("access_token").as[String])
    //   session
    // }

  setUp(
    scn.inject(rampUsers(9000).during(60))
  )
  
  .protocols(httpProtocol)
}