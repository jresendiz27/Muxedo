package com.jresendiz27.muxedo

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import io.vertx.config.ConfigChange
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AsyncResult
import io.vertx.core.DeploymentOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentLinkedQueue

@Log
@CompileStatic
class MuxedoApp {
    static String MUXEDO_VERTICLE = "groovy:com.jresendiz27.muxedo.verticles.MuxedoServerVerticle"
    static ConcurrentLinkedQueue<String> DEPLOYED_VERTICLES_ID = []
    static long SCAN_INTERVAL = (System.getenv("SCAN_INTERVAL") ?: "2000") as long
    static int NUMBER_OF_VERTICLES = (System.getenv("NUMBER_OF_VERTICLES") ?: "2") as int
    static String CONFIG_PATH = System.getenv("CONFIG_PATH") ?: "/app/muxedo.json"

    static void main(String[] args) {
        Vertx vertx = Vertx.vertx()

        ConfigStoreOptions jsonStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setOptional(false)
                .setConfig(new JsonObject().put("path", CONFIG_PATH))
        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions()
                .setScanPeriod(SCAN_INTERVAL)
                .addStore(jsonStore)
        ConfigRetriever config = ConfigRetriever.create(vertx, retrieverOptions)

        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setInstances(NUMBER_OF_VERTICLES)

        config.getConfig({ AsyncResult<JsonObject> json ->
            log.info("Deploying MuxedoServer")
            deploymentOptions.setConfig(json.result())
            deployVerticles(vertx, deploymentOptions)
        })

        config.listen({ ConfigChange configChange ->
            deploymentOptions.setConfig(configChange.newConfiguration)
            reloadVerticles(vertx, deploymentOptions)
        })

        vertx.exceptionHandler({ handler ->
            log.severe("Global exception at Muxedo Server ${handler.message}")
            handler.printStackTrace()
        })
    }

    private static void reloadVerticles(Vertx vertx, DeploymentOptions deploymentOptions) {
        while(DEPLOYED_VERTICLES_ID.iterator().hasNext()) {
            String verticleId = DEPLOYED_VERTICLES_ID.poll()
            vertx.undeploy(verticleId, { res ->
                log.info("Undeployed verticle. $verticleId, ${res.result()}")
            })
            Thread.sleep(100)
        }
        deployVerticles(vertx, deploymentOptions)
    }

    private static void deployVerticles(Vertx vertx, DeploymentOptions deploymentOptions) {
        vertx.deployVerticle(MUXEDO_VERTICLE, deploymentOptions, { handler ->
            if (handler.succeeded()) {
                log.info("Muxedo Server successfully deployed. VerticleId: ${handler.result()}")
                DEPLOYED_VERTICLES_ID.add(handler.result())
            } else {
                log.severe("Handler doesn't succeeded")
                handler.cause().printStackTrace()
            }
        })
    }
}