package oauth

import scalaoauth2.provider.{AuthInfo, DataHandler}
import java.util.Date
import java.sql.Timestamp
import util.Crypto
import models._
import scala.concurrent.Future
import scalaoauth2.provider.ClientCredential

class MyDataHandler extends DataHandler[models.User] {
  def validateClient(clientCredential: ClientCredential, grantType: String): Future[Boolean] = {
    Future.successful {
      Clients.validate(clientCredential.clientId, clientCredential.clientSecret, grantType)
    }
  }

  def findUser(username: String, password: String): Future[Option[User]] = {
    Future.successful {
      Users.findUser(username, password)
    }
  }

  def createAccessToken(authInfo: AuthInfo[User]): Future[scalaoauth2.provider.AccessToken] = {
    val accessTokenExpiresIn = 60 * 60 // 1 hour
    val now = new Date()
    val createdAt = new Timestamp(now.getTime)
    val refreshToken = Some(Crypto.generateToken())
    val accessToken = Crypto.generateToken()

    val tokenObject = models.AccessToken(accessToken, refreshToken, authInfo.user.id.toInt, authInfo.scope, accessTokenExpiresIn, createdAt, authInfo.clientId)
    AccessTokens.deleteExistingAndCreate(tokenObject, authInfo.user.id.toInt, authInfo.clientId)
    Future.successful {
      scalaoauth2.provider.AccessToken(accessToken, refreshToken, authInfo.scope, Some(accessTokenExpiresIn.toLong), now)
    }
  }

  def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[scalaoauth2.provider.AccessToken]] = {
    Future.successful {
      AccessTokens.findToken(authInfo.user.id, authInfo.clientId) map { a =>
        scalaoauth2.provider.AccessToken(a.accessToken, a.refreshToken, a.scope, Some(a.expiresIn.toLong), a.createdAt)
      }
    }
  }

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[scalaoauth2.provider.AccessToken] = {
    createAccessToken(authInfo)
  }

  def findClientUser(clientCredential: ClientCredential, scope: Option[String]): Future[Option[User]] = {
    Future.successful {
      None // Not implemented yet
    }
  }

  def findAccessToken(token: String): Future[Option[scalaoauth2.provider.AccessToken]] = {
    Future.successful {
      AccessTokens.findAccessToken(token) map { a =>
        scalaoauth2.provider.AccessToken(a.accessToken, a.refreshToken, a.scope, Some(a.expiresIn.toLong), a.createdAt)
      }
    }
  }

  def findAuthInfoByAccessToken(accessToken: scalaoauth2.provider.AccessToken): Future[Option[AuthInfo[User]]] = {
    Future.successful {
      AccessTokens.findAccessToken(accessToken.token) map { a =>
        val user = Users.getById(a.userId).get
        AuthInfo(user, a.clientId, a.scope, Some(""))
      }
    }
  }

  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] = {
    Future.successful {
      AccessTokens.findRefreshToken(refreshToken) map { a =>
        val user = Users.getById(a.userId).get
        AuthInfo(user, a.clientId, a.scope, Some(""))
      }
    }
  }

  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] = {
    Future.successful {
      AuthCodes.find(code) map { a =>
        val user = Users.getById(a.userId).get
        AuthInfo(user, a.clientId, a.scope, a.redirectUri)
      }
    }
  }
}
