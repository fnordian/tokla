import java.sql.SQLException
import models.db.TokenDb
import org.squeryl.adapters.H2Adapter
import org.squeryl.internals.DatabaseAdapter
import org.squeryl.{Session, SessionFactory}

import play.api._

import play.api.db.DB
import play.api.mvc.{Handler, RequestHeader}
import scala.Some

import play.api.Play.current

object Global extends GlobalSettings {



  override def onStart(app:Application):Unit =
  {
    SessionFactory.concreteFactory = Some(
      () => Session.create(DB.getDataSource().getConnection(),
        dbAdapter)
    );
  }

  val dbAdapter = new H2Adapter();


  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }


  /*
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {

    val ret = super.onRouteRequest(request)
    org.squeryl.Session.currentSessionOption.foreach( session => {
      Logger.info("about to close " + session)
      session.close
      session.unbindFromCurrentThread
    })

    ret
  }

*/

}
