package controllers

import play.api.mvc.{Action, Controller}
import scalaoauth2.provider.OAuth2Provider
import oauth.MyDataHandler
import play.api.libs.concurrent.Execution.Implicits._

object OAuth2Controller extends Controller with OAuth2Provider {
  def accessToken = Action.async { implicit request =>
    issueAccessToken(new MyDataHandler)
  }
}
