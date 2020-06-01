package com.jresendiz27.muxedo

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

@Log
@CompileStatic
class MuxedoServer {
    static void main(String[] args) {
        Vertx vertx = Vertx.vertx()
        HttpServer server = vertx.createHttpServer();
        Map routesDefinition = [
                paths          : [[
                                          path             : '/uri/:param1/:param2',
                                          methods          : ['POST', 'GET', 'PUT'],
                                          uriParams        : [
                                                  [
                                                          name: 'param1',
                                                          type: 'Integer'
                                                  ],
                                          ],
                                          expectedResponses: [
                                                  [
                                                          code           : 200,
                                                          expectedHeaders: [],
                                                          responseWeight : 50,
                                                          body           : "This is a mocked body 1",
                                                          responseHeaders: [
                                                                  'Content-Type': 'text/plain'
                                                          ]
                                                  ],
                                                  [
                                                          code           : 403,
                                                          expectedHeaders: [],
                                                          responseWeight : 50,
                                                          body           : "This is a mocked body 2",
                                                          responseHeaders: [
                                                                  'Content-Type': 'text/plain22222'
                                                          ]
                                                  ]
                                          ]

                                  ],
                                  [
                                          path             : '/hello/world',
                                          methods          : ['GET', 'PUT'],
                                          expectedResponses: [
                                                  [
                                                          code           : 200,
                                                          expectedHeaders: [],
                                                          body           : "This is a mocked body 1",
                                                          responseHeaders: [
                                                                  'Content-Type': 'text/plain'
                                                          ]
                                                  ]
                                          ]
                                  ]],
                errorHandler   : [
                        code           : 500,
                        responseHeaders: [
                                'Content-Type': 'application/json'
                        ],
                        showErrors     : true
                ],
                notFoundHandler: [
                        responseHeaders: [
                                'Content-Type': 'application/json'
                        ],
                        showErrors     : true
                ]
        ]
        Router router = new RouterFactory(vertx).create(routesDefinition)
        router.errorHandler(500, { RoutingContext handler ->
            Map errorMessage = [
                    error : handler.failure().message,
                    path  : handler.request().path(),
                    method: handler.request().method(),
                    params: handler.request().params(),
            ]
            log.severe("Exception during requests: ${errorMessage}")
            handler.failure().printStackTrace()
            handler.response().end("Exception: ${handler.failure().message}")
        })
        vertx.exceptionHandler({ handler ->
            log.severe("Global exception at Muxedo Server ${handler.message}")
            handler.printStackTrace()
        })
        server.requestHandler(router).listen(8081, "0.0.0.0", { handler ->
            log.info("Muxedo Server started at 8081")
        })
    }
}