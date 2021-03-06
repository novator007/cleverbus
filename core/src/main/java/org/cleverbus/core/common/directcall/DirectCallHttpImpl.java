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

package org.cleverbus.core.common.directcall;

import java.io.IOException;

import org.cleverbus.core.common.route.RouteConstants;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;


/**
 * Implementation of {@link DirectCall} interface with HTTP client that calls {@link DirectCallWsRoute}.
 *
 * @author <a href="mailto:petr.juza@cleverlance.com">Petr Juza</a>
 * @see DirectCallWsRoute
 */
public class DirectCallHttpImpl implements DirectCall {

    /**
     * URI of this localhost application, including port number.
     */
    @Value("${contextCall.localhostUri}")
    private String localhostUri;

    @Override
    public String makeCall(String callId) throws IOException {
        Assert.hasText(callId, "callId must not be empty");

        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpGet httpGet = new HttpGet(localhostUri + RouteConstants.HTTP_URI_PREFIX
                    + DirectCallWsRoute.SERVLET_URL + "?" + DirectCallWsRoute.CALL_ID_HEADER + "=" + callId);

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        // successful response
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new IOException(EntityUtils.toString(response.getEntity()));
                    }
                }
            };

            return httpClient.execute(httpGet, responseHandler);
        } finally {
            httpClient.close();
        }
    }
}
