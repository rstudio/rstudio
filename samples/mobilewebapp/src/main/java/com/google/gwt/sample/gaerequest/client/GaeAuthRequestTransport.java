/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.sample.gaerequest.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.sample.gaerequest.shared.GaeHelper;
import com.google.gwt.user.client.Window.Location;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.gwt.client.DefaultRequestTransport;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

/**
 * Extends DefaultRequestTransport to handle the authentication failures
 * reported by {@link com.google.gwt.sample.gaerequest.server.GaeAuthFilter}.
 */
public class GaeAuthRequestTransport extends DefaultRequestTransport {
  private final EventBus eventBus;

  public GaeAuthRequestTransport(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  protected void configureRequestBuilder(RequestBuilder builder) {
    super.configureRequestBuilder(builder);

    /*
     * Add the redirect URL in case the user is logged out.
     * 
     * /MobileWebApp.html?parem0=value0&param1=value1#hash
     */
    String redirectUrl = Location.getPath() + Location.getQueryString() + Location.getHash();
    builder.setHeader(GaeHelper.REDIRECT_URL_HTTP_HEADER_NAME, redirectUrl);
  }

  @Override
  protected RequestCallback createRequestCallback(final TransportReceiver receiver) {
    final RequestCallback superCallback = super.createRequestCallback(receiver);

    return new RequestCallback() {
      @Override
      public void onError(Request request, Throwable exception) {
        superCallback.onError(request, exception);
      }

      @Override
      public void onResponseReceived(Request request, Response response) {
        /*
         * The GaeAuthFailure filter responds with Response.SC_UNAUTHORIZED and
         * adds a "login" url header if the user is not logged in. When we
         * receive that combo, post an event so that the app can handle things
         * as it sees fit.
         */
        if (Response.SC_UNAUTHORIZED == response.getStatusCode()) {
          String loginUrl = response.getHeader("login");
          if (loginUrl != null) {
            /*
             * Hand the receiver a non-fatal callback, so that
             * com.google.web.bindery.requestfactory.shared.Receiver will not
             * post a runtime exception.
             */
            receiver
                .onTransportFailure(new ServerFailure("Unauthenticated user", null, null, false));
            eventBus.fireEvent(new GaeAuthenticationFailureEvent(loginUrl));
            return;
          }
        }
        superCallback.onResponseReceived(request, response);
      }
    };
  }
}
