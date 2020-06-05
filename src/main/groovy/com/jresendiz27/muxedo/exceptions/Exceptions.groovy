package com.jresendiz27.muxedo.exceptions

/**
 * MuxedoException: The global exception for Muxedo server.
 */
class MuxedoException extends Exception {
    MuxedoException(String message) {
        super(message)
    }
}

/**
 * ResponsesWeightsNotBalancedException
 * This exception is raised when all the responses for a given path are not balanced. The sum of all the <b>responseWeight</b>
 * is expected to be equal to 100. You can have the full control of all the responses weight, but it's up to the user
 * to provide valid values. The <b>responseWeight</b> must be an Integer value
 *
 */
class ResponsesWeightsNotBalancedException extends MuxedoException {
    ResponsesWeightsNotBalancedException(String message) {
        super(message)
    }
}

/**
 * RequestParamNotValidException
 * This exception is raised when a defined param is not valid. This exception considers if the param was
 * provided via requests and if it's a valid value (Integer or Float). Otherwise is ignored.
 *
 */
class RequestParamNotValidException extends MuxedoException {
    RequestParamNotValidException(String message) {
        super(message)
    }
}

/**
 * NotExpectedMethodException
 * This exception is raised when the request method is not defined in the <b>expectedMethods</b> array.
 *
 */
class NotExpectedMethodException extends MuxedoException {
    NotExpectedMethodException(String message) {
        super(message)
    }
}

/**
 * ExpectedHeaderInRequestException
 * This exception is raised a defined header was expected in a request. All the expectedHeaders are defined in
 * the <b>expectedHeaders</b> array.
 *
 */
class ExpectedHeaderInRequestException extends MuxedoException {
    ExpectedHeaderInRequestException(String message) {
        super(message)
    }
}

/**
 * DefinedRequestsWeightsNotValidException
 * This exception is raised when any of the expectedResponses
 *
 */
class DefinedRequestsWeightsNotValidException extends MuxedoException {
    DefinedRequestsWeightsNotValidException(String message) {
        super(message)
    }
}