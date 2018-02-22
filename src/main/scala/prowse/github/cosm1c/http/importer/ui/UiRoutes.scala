package prowse.github.cosm1c.http.importer.ui

import akka.http.scaladsl.server._

class UiRoutes(uiWebSocketFlow: UiWebSocketFlow) extends Directives {

    val route: Route =
        pathEndOrSingleSlash {
            getFromResource("ui/index.html")
        } ~ path("ws") {
            handleWebSocketMessages(uiWebSocketFlow.clientWebSocketFlow)
        } ~ getFromResourceDirectory("ui")

}
