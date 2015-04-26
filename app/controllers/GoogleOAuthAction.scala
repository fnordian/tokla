package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import com.ning.http.client.{AsyncHttpClientConfig, ProxyServer, AsyncHttpClient}

import play.api.libs.openid.UserInfo

class GoogleOAuthAction(googleClientId: String, googleClientSecret: String, redirectUrl: String) {
  def googleOpenIdUrl(securityState: String) = {

    "https://accounts.google.com/o/oauth2/auth?client_id=" + googleClientId +
      "&response_type=code" +
      "&scope=email%20profile" +
      "&redirect_uri=" + redirectUrl +
      "&state=" + securityState
  }

  def httpClient = {
    val cf = new AsyncHttpClientConfig.Builder().setProxyServer(new
        ProxyServer("localhost", 8080, "", "")).build();
    new AsyncHttpClient(cf);
  }

  def retrieveGoolgeAccessToken(authCode: String) = {

    def extractAccessToken(jsonString: String) = {
      (Json.parse(jsonString) \ "access_token").asOpt[String]
    }

    val authTokenEndpoint = "https://www.googleapis.com/oauth2/v3/token"

    val holder = WS.url(authTokenEndpoint)


    val req = httpClient.preparePost(authTokenEndpoint)

    val post = req

      .addHeader("Content-Type", "application/x-www-form-urlencoded")
      .addHeader("Charset", "UTF-8")
      .setBody("code=" + authCode +
      "&client_id=" + googleClientId +
      "&client_secret=" + googleClientSecret +
      "&redirect_uri=" + redirectUrl +
      "&grant_type=authorization_code")

    val response = post.execute().get()

    Logger.debug("response: " + response.getResponseBody)

    val accessToken = extractAccessToken(response.getResponseBody)

    Logger.debug("access token: " + accessToken)

    accessToken
  }


  def retrieveGoogleUserInfo(accessToken: String) = {
    def extractFieldFromGoogleUserInfo(jsonString: String, selector: String) = {
      (Json.parse(jsonString) \ selector).as[String]
    }

    val response = httpClient.prepareGet("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + accessToken).execute().get()

    val email = extractFieldFromGoogleUserInfo(response.getResponseBody, "email")

    Some(UserInfo(email, Map("email" -> email,
      "given_name" -> extractFieldFromGoogleUserInfo(response.getResponseBody, "given_name"),
      "family_name" -> extractFieldFromGoogleUserInfo(response.getResponseBody, "family_name")
    )))
  }


  def verifyCallback(authCode: String)(implicit request: play.api.mvc.RequestHeader) = {

    retrieveGoolgeAccessToken(authCode) match {
      case Some(accessToken) => retrieveGoogleUserInfo(accessToken)
      case _ => None
    }
  }


}

object GoogleOAuthAction {

  def guessProtocolFromHost(host: String) = {
    if (host.endsWith(":443")) "https://" else "http://"
  }

  def requestToUrl(request: Request[AnyContent]) = {
    guessProtocolFromHost(request.host) + request.host + request.path
  }

  def callBackdataFromRequest(request: Request[AnyContent]) = {
    (request.queryString("state"), request.queryString("code"))
  }

  def randomSecurityState = {
    String.valueOf(new scala.util.Random().nextLong())
  }

  def validState(request: Request[AnyContent]) = {
    val tuple = (request.getQueryString("state"), request.cookies.get("googleSecurityState"))

    tuple match {
      case (Some(state), Some(cookieState)) if state == cookieState.value => true
      case _ => false
    }
  }

  def codeFromCallbackRequest(request: Request[AnyContent]) = {
    request.getQueryString("code").get
  }


  def apply(googleClientId: String, googleClientSecret: String)(f: Request[AnyContent] => Option[UserInfo] => Result) = {
    Action {
      implicit request =>

        val google = new GoogleOAuthAction(googleClientId, googleClientSecret, requestToUrl(request))

        if (!validState(request)) {
          val googleSecurityState = randomSecurityState
          TemporaryRedirect(google.googleOpenIdUrl(googleSecurityState)).withCookies(Cookie("googleSecurityState", googleSecurityState))
        } else {
          google.retrieveGoolgeAccessToken(codeFromCallbackRequest(request)) match {
            case Some(accessToken) => f(request)(google.retrieveGoogleUserInfo(accessToken))
            case _ => f(request)(None)
          }
        }
    }
  }
}
