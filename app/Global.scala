import models.db.TokenDb
import org.squeryl.adapters.H2Adapter
import org.squeryl.internals.DatabaseAdapter
import org.squeryl.{Session, SessionFactory}

import play.api._

import play.api.db.DB
import play.api.mvc.{Handler, RequestHeader}
import scala.Some

object Global extends GlobalSettings {

  def getSession (adapter: H2Adapter, application: Application) : Session = {
    import play.api.Play.current

    //Logger.info(DB.getDataSource().getConnection.toString)

    val session = new org.squeryl.Session(DB.getDataSource().getConnection(), adapter);

    session
  }


  override def onStart(app: Application) {


    SessionFactory.concreteFactory = app.configuration.getString("db.default.driver") match {
      case Some("org.h2.Driver") => Some(() => getSession(new H2Adapter, app))

      case _ => sys.error("Database driver must be either org.h2.Driver")
    }



    Logger.info("Application has started")
  }



  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {

    val ret = super.onRouteRequest(request)
    org.squeryl.Session.currentSessionOption.foreach( session => {
      Logger.info("about to close " + session)
      session.close
      session.unbindFromCurrentThread
    })

    Logger.info("after routing")

    ret
  }



}
