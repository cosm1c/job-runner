package prowse.github.cosm1c.http.importer

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.typesafe.config.ConfigFactory
import prowse.github.cosm1c.http.importer.job.{JobManagerActor, JobRestService}
import prowse.github.cosm1c.http.importer.swagger.SwaggerDocService
import prowse.github.cosm1c.http.importer.ui.{UiRoutes, UiWebSocketFlow}

import scala.concurrent.{ExecutionContextExecutor, Future}

class AppSupervisorActor extends Actor with ActorLogging {

    private implicit val actorSystem: ActorSystem = context.system
    private implicit val materializer: ActorMaterializer = ActorMaterializer()
    private implicit val executionContext: ExecutionContextExecutor = context.dispatcher

    private val config = ConfigFactory.load()
    private val version = config.getString("build.version")
    private val httpPort = config.getInt("app.httpPort")

    private val uiStreams = new UiWebSocketFlow()
    private val uiRoutes = new UiRoutes(uiStreams)(context.dispatcher, log)
    private val jobManagerActor = context.actorOf(JobManagerActor.props(uiStreams), "JobManagerActor")

    private val route: Route =
        decodeRequest {
            encodeResponse {
                cors() {
                    uiRoutes.route ~
                        new JobRestService(jobManagerActor).route ~
                        new SwaggerDocService(version, "/").routes
                }
            }
        }

    @SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
    private var bindingFuture: Future[ServerBinding] = _

    override def preStart(): Unit = {
        bindingFuture = Http().bindAndHandle(route, "0.0.0.0", httpPort)
        bindingFuture.onComplete(serverBinding => log.info("Server online - {}", serverBinding))
    }

    override def postStop(): Unit = {
        bindingFuture.flatMap { serverBinding =>
            log.info("Server offline - {}", serverBinding)
            serverBinding.unbind()
        }
        ()
    }

    override def receive: Receive = {
        case msg => log.warning("Received unexpected message: {}", msg)
    }

}
