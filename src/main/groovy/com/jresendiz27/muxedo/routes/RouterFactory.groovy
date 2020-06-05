package com.jresendiz27.muxedo.routes

import com.jresendiz27.muxedo.exceptions.*
import groovy.transform.ToString
import groovy.util.logging.Log
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
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
        if (!routesDefinition.paths) {
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
                if (!request.getParam(map.name)) {
                    throw new RequestParamNotValidException("Param: ${map.name} was not provided in requests")
                }
                if (!isValidType(request.getParam(map.name), map.type)) {
                    throw new RequestParamNotValidException("Param: ${map.name} doesn't match type. Expected: ${map.type}")
                }
            }
        }

        Set<String> expectedMethods = (validations['methods'] ?: []) as Set<String>
        if (!expectedMethods.contains(request.method().name())) {
            throw new NotExpectedMethodException("Method: ${request.method().name()} was not expected in definition")
        }

        validations['expectedHeaders'].each { String header ->
            if (!request.headers().get(header)) {
                throw new ExpectedHeaderInRequestException("Header: ${header} was not provided in requests.")
            }
        }
    }

    Router routerFromDefinition(List<Map> definition) {
        definition.each { Map map ->
            router.route(map.path as String).handler({ RoutingContext ctx ->
                try {
                    Map currentDefinition = map.clone() as Map
                    HttpServerRequest request = ctx.request()
                    HttpServerResponse response = ctx.response()
                    log.info("Processing request for path: ${request.path()}")

                    executeValidations(request, currentDefinition)
                    setupPossibleResponses(response, currentDefinition)
                } catch (MuxedoException exception) {
                    ctx.fail(500, exception)
                }
            })
        }

        setCurrentConfigRouter()
        setErrorHandler()
        setNotFoundHandler()
        return this.router
    }

    Router baseRouter() {
        this.router.route().handler({ routingContext ->
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
            boolean allRequestsHaveWeight = definition.expectedResponses.every { Map map ->
                map.responseWeight instanceof Integer
            }

            if (!allRequestsHaveWeight) {
                throw new DefinedRequestsWeightsNotValidException("All request must have a valid responseWeight entry")
            }

            if (!areResponsesWeightBalanced(definition['expectedResponses'] as List<Map>)) {
                throw new ResponsesWeightsNotBalancedException("Weight Responses are not balanced!")
            }
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

    void setCurrentConfigRouter() {
        this.router.route("/_muxedo/config").handler({ RoutingContext ctx ->
            JsonObject config = vertx.getOrCreateContext().config()
            ctx.response().setChunked(true)
            ctx.response().headers().add("Content-Type", "application/json")
            ctx.response().end(config.toString())
        })
    }

    void setErrorHandler() {
        this.router.errorHandler(500, { RoutingContext ctx ->
            Map errorMessage = [
                    error : ctx.failure().message,
                    path  : ctx.request().path(),
                    method: ctx.request().method(),
                    params: ctx.request().params().entries(),
            ]
            ctx.failure().printStackTrace()
            log.severe("Exception during requests: ${errorMessage}")

            ctx.response().setStatusCode(500)
            ctx.response().setChunked(true)
            ctx.response().putHeader("Content-Type", "application/json")
            ctx.response().end(JsonObject.mapFrom(errorMessage).toString())
        })
    }

    void setNotFoundHandler() {
        this.router.errorHandler(404, { RoutingContext ctx ->
            JsonObject jsonResponse = new JsonObject()
            jsonResponse.put("NotFound", ctx.request().path())
            ctx.response().setStatusCode(404)
            ctx.response().setChunked(true)
            ctx.response().putHeader("Content-Type", "application/json")
            ctx.response().end(jsonResponse.toString())
        })
    }
}
