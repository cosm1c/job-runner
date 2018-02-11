package prowse.github.cosm1c.http.importer.ui

import akka.http.scaladsl.server._

class UiRoutes(uiWebSocketFlow: UiWebSocketFlow) extends Directives {

    val route: Route =
        pathEndOrSingleSlash {

            getFromResource("index.html")

            // For multiple files see: getFromResourceDirectory

        } ~ path("ws") {
            handleWebSocketMessages(uiWebSocketFlow.clientWebSocketFlow)
        }


}
