package com.jresendiz27.muxedo.verticles

import com.jresendiz27.muxedo.routes.RouterFactory
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

@Log
@CompileStatic
class MuxedoServerVerticle extends AbstractVerticle {
    void start() {
        JsonObject config = vertx.getOrCreateContext().config()

        HttpServerOptions httpServerOptions = new HttpServerOptions()
        httpServerOptions.port = config.getInteger("port") ?: 8081
        httpServerOptions.host = config.getString("host") ?: "0.0.0.0"
        httpServerOptions.logActivity = true
        httpServerOptions.reusePort = true

        HttpServer server = vertx.createHttpServer(httpServerOptions)

        Router router = new RouterFactory(vertx).create(config.mapTo(Map.class))
        server.requestHandler(router)
                .listen({
                    log.info("MuxedoServerVerticle started")
                })
    }

    void stop() {
        log.info("Finishing MuxedoServerVerticle ${this.deploymentID()}")
    }
}
