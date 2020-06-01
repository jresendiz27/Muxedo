package com.jresendiz27.muxedo

import groovy.transform.ToString
import groovy.util.logging.Log
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

@ToString
@Log
class RouterFactory {
    Router router
    Vertx vertx
    Random random

    RouterFactory(Vertx vertx) {
        this.vertx = vertx
        this.router = Router.router(this.vertx)
        this.random = new Random()
    }

    Router create(Map routesDefinition = [:]) {
        if (!routesDefinition) {
            return baseRouter()
        } else {
            return routerFromDefinition(routesDefinition.paths as List<Map>)
        }
    }

    boolean isValidType(String param, String type) {
        try {
            switch (type) {
                case 'integer':
                    param as Integer
                    break;
                case 'float':
                    param as Float
                    break;
                default:
                    break;
            }
            return true
        } catch (NumberFormatException ignored) {
            return false
        }
    }

    void executeValidations(HttpServerRequest request, Map validations) {
        if (!request.params().isEmpty()) {
            validations['uriParams'].each { Map<String, String> map ->
                assert request.getParam(map.name)
                assert isValidType(request.getParam(map.name), map.type)
            }
        }

        Set<String> expectedMethods = validations['methods'] as Set<String>
        assert expectedMethods.contains(request.method().name())

        validations['expectedHeaders'].each { String header, String _ignored ->
            assert request.headers().contains(header)
        }
    }

    Router routerFromDefinition(List<Map> definition) {
        definition.stream().forEach(map -> {
            router.route(map['path'] as String).handler({ RoutingContext ctx ->
                List<String> errors = []
                Map currentDefinition = map.clone() as Map
                HttpServerRequest request = ctx.request()
                HttpServerResponse response = ctx.response()
                log.info("Processing request for path: ${request.path()}")

                executeValidations(request, currentDefinition)
                setupPossibleResponses(response, currentDefinition)
            })
        })
        return this.router
    }

    Router baseRouter() {
        this.router.route().handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader('Content-Type', 'text/plain')
            response.end("Muxedo says hello!")
        });
        return this.router;
    }

    boolean responsesWeightAreValid(Map definition) {
        boolean areValid = definition.expectedResponses.every { Map map ->
            map.responseWeight != null
        }
        if (areValid) {
            assert definition.expectedResponses.every { Map map ->
                map.responseWeight instanceof Integer
            }
            assert areResponsesWeightBalanced(definition['expectedResponses'] as List<Map>)
            return areValid
        } else {
            log.warning("Not all responses have weight, using the first one")
            return false
        }
    }

    boolean areResponsesWeightBalanced(List<Map> responses) {
        int totalWeight = (responses*.responseWeight as List).sum()
        totalWeight == 100
    }

    void setupPossibleResponses(HttpServerResponse response, Map definition) {
        if (responsesWeightAreValid(definition)) {
            int randomWeight = random.nextInt(100)
            int availableWeight = 100
            boolean hasBeenWritten = false
            for (Map resp : definition.expectedResponses) {
                availableWeight = availableWeight - (resp.responseWeight as int)
                if ((availableWeight <= randomWeight) && !hasBeenWritten) {
                    response.statusCode = resp['code'] as int
                    resp['responseHeaders'].each { String k, String v ->
                        response.putHeader(k, v)
                    }
                    response.setChunked(true)
                    response.write(resp['body'] as String)
                    break
                }
            }
            response.end()
        } else {
            Map resp = definition.expectedResponses.first()
            response.statusCode = resp['code'] as int
            resp['responseHeaders'].each { String k, String v ->
                response.putHeader(k, v)
            }
            response.end(resp['body'] as String)
        }
    }
}
