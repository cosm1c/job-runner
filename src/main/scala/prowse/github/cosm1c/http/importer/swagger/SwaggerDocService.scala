package prowse.github.cosm1c.http.importer.swagger

import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import prowse.github.cosm1c.http.importer.job.JobRestService

class SwaggerDocService(version: String,
                        override val basePath: String) extends SwaggerHttpService {

    override val apiClasses = Set(
        classOf[JobRestService]
    )

    override val info = Info(
        title = "http-importer",
        version = version,
        description = "An example Job Runner which streams data over WebSocket."
        //contact: Option[Contact] = None,
    )

    override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")

    //override val host = "localhost:12345"
    //override val externalDocs = Some(new ExternalDocs("Core Docs", "http://acme.com/docs"))
    //override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
}
