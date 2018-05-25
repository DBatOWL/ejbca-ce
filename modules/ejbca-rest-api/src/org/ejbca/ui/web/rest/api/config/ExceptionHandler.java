/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ui.web.rest.api.config;

import org.apache.log4j.Logger;
import org.cesecore.CesecoreException;
import org.ejbca.core.EjbcaException;
import org.ejbca.ui.web.rest.api.exception.CesecoreExceptionClasses;
import org.ejbca.ui.web.rest.api.exception.EjbcaExceptionClasses;
import org.ejbca.ui.web.rest.api.exception.ExceptionClasses;
import org.ejbca.ui.web.rest.api.exception.RestException;
import org.ejbca.ui.web.rest.api.io.response.ExceptionInfoRestResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status;

/**
 * General JAX-RS Exception handler to catch an Exception and create its appropriate response with error's status and error's message.
 *
 * @version $Id: ExceptionHandler.java 28962 2018-05-21 06:54:45Z andrey_s_helmes $
 */
@Provider
public class ExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger logger = Logger.getLogger(ExceptionHandler.class);

    public static final int DEFAULT_ERROR_CODE = Status.INTERNAL_SERVER_ERROR.getStatusCode();
    public static final String DEFAULT_ERROR_MESSAGE = "General failure.";

    @Override
    public Response toResponse(Exception exception) {
        ExceptionInfoRestResponse exceptionInfoRestResponse = null;
        // Map through EjbcaException
        if (exception instanceof EjbcaException) {
            exceptionInfoRestResponse = mapEjbcaException((EjbcaException) exception);
        }
        // Map through CesecoreException
        else if (exception instanceof CesecoreException) {
            exceptionInfoRestResponse = mapCesecoreException((CesecoreException) exception);
        }
        // Map through WebApplicationException
        else if (exception instanceof WebApplicationException) {
            // TODO Check this, might be target server dependent
            logger.warn("Cannot find a proper mapping for the exception, falling back to default.", exception);
        } else if (exception instanceof RestException) {
            final RestException restException = (RestException) exception;
            exceptionInfoRestResponse = ExceptionInfoRestResponse.builder()
                    .errorCode(restException.getErrorCode())
                    .errorMessage(restException.getMessage())
                    .build();
        }
        // If previous mapping failed, try to map through Standalone Exception
        if (exceptionInfoRestResponse == null) {
            exceptionInfoRestResponse = mapException(exception);
        }
        // Fall back to default if mapping doesn't exist
        if (exceptionInfoRestResponse == null) {
            logger.warn("Cannot find a proper mapping for the exception, falling back to default.", exception);
            exceptionInfoRestResponse = ExceptionInfoRestResponse.builder()
                    .errorCode(DEFAULT_ERROR_CODE)
                    .errorMessage(DEFAULT_ERROR_MESSAGE)
                    .build();
        }
        return getExceptionResponse(exceptionInfoRestResponse);
    }

    // Map EjbcaException extending exceptions
    private ExceptionInfoRestResponse mapEjbcaException(final EjbcaException ejbcaException) {
        switch (EjbcaExceptionClasses.fromClass(ejbcaException.getClass())) {
            // 400
            case ApprovalException:
            case KeyStoreGeneralRaException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.BAD_REQUEST.getStatusCode())
                        .errorMessage(ejbcaException.getMessage())
                        .build();
            // 403
            case AuthLoginException:
            case AuthStatusException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.FORBIDDEN.getStatusCode())
                        .errorMessage(ejbcaException.getMessage())
                        .build();
            // 404
            case NotFoundException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.NOT_FOUND.getStatusCode())
                        .errorMessage(ejbcaException.getMessage())
                        .build();
            // 409
            case AlreadyRevokedException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.CONFLICT.getStatusCode())
                        .errorMessage(ejbcaException.getMessage())
                        .build();
            // 422
            // TODO These exception cannot be found in compilation classpath
//            case WrongTokenTypeException:
//            case CertificateProfileTypeNotAcceptedException:
            case CustomFieldException:
            case EndEntityProfileValidationRaException:
            case RevokeBackDateNotAllowedForProfileException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(422)
                        .errorMessage(ejbcaException.getMessage())
                        .build();
        }
        return null;
    }

    // Map CesecoreException extending exceptions
    private ExceptionInfoRestResponse mapCesecoreException(final CesecoreException cesecoreException) {
        switch (CesecoreExceptionClasses.fromClass(cesecoreException.getClass())) {
            // 400
            case CertificateRevokeException:
            case CertificateSerialNumberException:
            case EndEntityExistsException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.BAD_REQUEST.getStatusCode())
                        .errorMessage(cesecoreException.getMessage())
                        .build();
            // 404
            case CADoesntExistsException:
            case CertificateProfileDoesNotExistException:
            case NoSuchEndEntityException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.NOT_FOUND.getStatusCode())
                        .errorMessage(cesecoreException.getMessage())
                        .build();
            // 422
            case IllegalNameException:
            case IllegalValidityException:
            case InvalidAlgorithmException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(422)
                        .errorMessage(cesecoreException.getMessage())
                        .build();
            // 500
            case CertificateCreateException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
                        .errorMessage(DEFAULT_ERROR_MESSAGE)
                        .build();
            // 503
            case CAOfflineException:
            case CryptoTokenOfflineException:
            case CTLogException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.SERVICE_UNAVAILABLE.getStatusCode())
                        .errorMessage(cesecoreException.getMessage())
                        .build();
        }
        return null;
    }

    private ExceptionInfoRestResponse mapException(final Exception exception) {
        switch (ExceptionClasses.fromClass(exception.getClass())) {
            // 202
            case WaitingForApprovalException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.ACCEPTED.getStatusCode())
                        .errorMessage(exception.getMessage())
                        .build();
            // 400
            case ApprovalRequestExecutionException:
            case ApprovalRequestExpiredException:
            case RoleExistsException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.BAD_REQUEST.getStatusCode())
                        .errorMessage(exception.getMessage())
                        .build();
            // 403
            case AuthenticationFailedException:
            case AuthorizationDeniedException:
            case SelfApprovalException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.FORBIDDEN.getStatusCode())
                        .errorMessage(exception.getMessage())
                        .build();
            // 404
            case EndEntityProfileNotFoundException:
            case RoleNotFoundException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.NOT_FOUND.getStatusCode())
                        .errorMessage(exception.getMessage())
                        .build();
            // 409
            case AdminAlreadyApprovedRequestException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(Status.CONFLICT.getStatusCode())
                        .errorMessage(exception.getMessage())
                        .build();
            // 413
            case StreamSizeLimitExceededException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(413)
                        .errorMessage(exception.getMessage())
                        .build();
            // 422
            case EndEntityProfileValidationException:
            case UserDoesntFullfillEndEntityProfile:
            case CertificateExtensionException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(422)
                        .errorMessage(exception.getMessage())
                        .build();
            // 500
            case CertificateEncodingException:
                return ExceptionInfoRestResponse.builder()
                        .errorCode(DEFAULT_ERROR_CODE)
                        .errorMessage(DEFAULT_ERROR_MESSAGE)
                        .build();
        }
        return null;
    }


    private Response getExceptionResponse(final ExceptionInfoRestResponse exceptionInfoRestResponse) {
        return Response
                .status(exceptionInfoRestResponse.getErrorCode())
                .entity(exceptionInfoRestResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
