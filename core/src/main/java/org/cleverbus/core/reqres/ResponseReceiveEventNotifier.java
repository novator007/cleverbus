/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cleverbus.core.reqres;

import java.io.StringWriter;
import java.util.EventObject;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.cleverbus.api.asynch.AsynchConstants;
import org.cleverbus.api.entity.Message;
import org.cleverbus.api.entity.Request;
import org.cleverbus.api.entity.Response;
import org.cleverbus.api.event.EventNotifier;
import org.cleverbus.api.event.EventNotifierBase;
import org.cleverbus.common.log.Log;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.ws.soap.client.SoapFaultClientException;


/**
 * Listens to response receive (event = {@link ExchangeSentEvent}).
 * If endpoint URI is successfully filtered and saving is enabled then response is saved to DB.
 *
 * @author <a href="mailto:petr.juza@cleverlance.com">Petr Juza</a>
 * @see RequestSendingEventNotifier
 * @since 0.4
 */
@EventNotifier
public class ResponseReceiveEventNotifier extends EventNotifierBase<ExchangeSentEvent> {

    /**
     * True for enabling saving requests/responses for filtered endpoints URI.
     */
    @Value("${requestSaving.enable}")
    private boolean enable;

    /**
     * Pattern for filtering endpoints URI which requests/response should be saved.
     */
    @Value("${requestSaving.endpointFilter}")
    private String endpointFilter;

    private Pattern endpointFilterPattern;

    @Autowired
    private RequestResponseService requestResponseService;

    @PostConstruct
    public void compilePattern() {
        endpointFilterPattern = Pattern.compile(endpointFilter);
    }

    @Override
    public boolean isEnabled(EventObject event) {
        return enable && super.isEnabled(event);
    }

    @Override
    protected void doNotify(ExchangeSentEvent event) throws Exception {
        String endpointUri = event.getEndpoint().getEndpointUri();

        if (RequestSendingEventNotifier.filter(endpointUri, endpointFilterPattern)) {
            // get response
            String resStr;
            String failedReason = null;

            Assert.state(event.getSource() instanceof Exchange, "event's source must be type of Exchange");

            Exchange exchange = (Exchange) event.getSource();

            if (exchange.isFailed()) {
                // failed response
                resStr = transformException(exchange.getException());
                failedReason = ExceptionUtils.getStackTrace(exchange.getException());
            } else {
                // "normal" response

                // If the Exchange is using InOnly as the MEP, then we may think that the Exchange has no OUT message.
                // Exchange.getOut creates an out message if there is none. So if we want to check if there is an out
                // message then we should use exchange.hasOut instead
                resStr = exchange.hasOut() ? exchange.getOut().getBody(String.class)
                        : exchange.getIn().getBody(String.class);
            }

            // note: there are Camel components which don't return responses, there are one-way only
            if (StringUtils.isNotEmpty(resStr) || StringUtils.isNotEmpty(failedReason)) {
                // get request
                Request request = getRequest(event.getExchange(), event.getEndpoint());

                // get message for asynchronous request
                Message msg = exchange.getIn().getHeader(AsynchConstants.MSG_HEADER, Message.class);

                Response response = Response.createResponse(request, resStr, failedReason, msg);

                if (request == null) {
                    Log.warn("There is no corresponding request for response " + response.toHumanString());
                }

                try {
                    // save response
                    requestResponseService.insertResponse(response);
                } catch (Exception ex) {
                    Log.error("Response didn't saved.", ex);
                }
            }
        }
    }

    /**
     * Gets request to response.
     *
     * @param exchange the exchange
     * @param endpoint the endpoint from response was sent
     * @return request; sometimes can happen that it's not possible to find request
     *  (e.g. for synchronous request where exchange is changed) and even so it's good to save response
     */
    private @Nullable Request getRequest(Exchange exchange, Endpoint endpoint) {
        // try exchange header
        Request req = exchange.getIn().getHeader(RequestSendingEventNotifier.SAVE_REQ_HEADER, Request.class);

        if (req == null) {
            Log.debug("There is no request in exchange header '" + RequestSendingEventNotifier.SAVE_REQ_HEADER + "'");

            // if it's asynchronous message then use correlation ID
            Message msg = exchange.getIn().getHeader(AsynchConstants.MSG_HEADER, Message.class);
            if (msg != null) {
                req = requestResponseService.findLastRequest(endpoint.getEndpointUri(), msg.getCorrelationId());

                if (req == null) {
                    Log.warn("There is no request for URI=" + endpoint.getEndpointUri()
                            + ", correlationId=" + msg.getCorrelationId());
                }
            } else {
                Log.debug("It's not asynchronous message - no message");

                req = requestResponseService.findLastRequest(endpoint.getEndpointUri(), exchange.getExchangeId());

                if (req == null) {
                    Log.warn("There is no request for URI=" + endpoint.getEndpointUri()
                            + ", exchangeId=" + exchange.getExchangeId());
                }
            }
        }

        return req;
    }

    /**
     * Transforms {@link Exception} to string representation.
     *
     * @param exception the exception that occurred
     * @return string of XML that represents exception of calling external system
     */
    private @Nullable String transformException(Exception exception) {
        Assert.notNull(exception, "the exception must not be null");

        String exceptionString = null;

        if (exception instanceof SoapFaultClientException) {
            // exception from Spring WS = Soap Fault
            SoapFaultClientException ex = (SoapFaultClientException) exception;

            StringWriter writer = new StringWriter();

            try {
                // An identity transformer
                if (ex.getSoapFault() != null && ex.getSoapFault().getSource() != null) {
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    StreamResult result = new StreamResult(writer);
                    transformer.transform(ex.getSoapFault().getSource(), result);
                    StringBuffer sb = writer.getBuffer();
                    exceptionString = sb.toString();
                }
            } catch (TransformerException e) {
                Log.warn("Error occurs during transformation SOAP Fault to XML representation", e);
            }
        }

        return exceptionString;
    }
}
