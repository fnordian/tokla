package controllers.mail

import javax.mail._
import javax.mail.internet._
import java.util.Properties._

import models.Token
import play.api.Play

object MailNotification {

  def _mailSession = {
    // Set up the mail object

    val username = Play.configuration.getString("gmail.username").get
    val password = Play.configuration.getString("gmail.password").get
    val properties = System.getProperties
    properties.put("mail.smtp.host", "smtp.gmail.com")
    properties.put("mail.smtp.user", username);

    properties.put("mail.smtp.starttls.enable", "true");
    properties.put("mail.smtp.auth", "true"); // If you need to authenticate
    // Use the following if you need SSL
    properties.put("mail.smtp.socketFactory.port", "465");
    properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    properties.put("mail.smtp.socketFactory.fallback", "false");


    val session = Session.getInstance(properties, new javax.mail.Authenticator() {
      override def getPasswordAuthentication: PasswordAuthentication = {
        new PasswordAuthentication(username, password);
      }
    })

    session
  }

  def sendTestMail = {
    val session = _mailSession
    val message = new MimeMessage(session)

    message.setFrom(new InternetAddress("test@example.org"))
    message.setRecipients(Message.RecipientType.TO, "marcus.hunger@gmail.com")
    message.setSubject("Greetings from langref.org")
    message.setText("jo jo jo")

    // And send it
    Transport.send(message)

  }

  def sendTokenNotification(token: Token) = {
    val session = _mailSession
    val message = new MimeMessage(session)

    message.setFrom(new InternetAddress("notification@tok.la"))
    message.setRecipients(Message.RecipientType.TO, token.claimedBy)
    message.setSubject(token.name + " is now claimed by you")
    message.setText("Hello,\nthe token " + token.name + " is now owned by you. Please release it when you don't need at anymore at http://tok.la/token/" + token.id + "\n\nCheers, tok.la\n")

    // And send it
    Transport.send(message)
  }
}
