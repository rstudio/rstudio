/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.requestfactory.client;

import com.google.gwt.http.client.Response;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.RequestEvent.State;
import com.google.gwt.user.client.Window.Location;

/**
 * A request event handler which listens to every request and reacts if there
 * is an authentication problem. Note that the server side code is responsible
 * for making sure that no sensitive information is returned in case of
 * authentication issues. This handler is just responsible for making such
 * failures user friendly.
 */
public class AuthenticationFailureHandler implements RequestEvent.Handler {
  private String lastSeenUser = null;
  
  public void onRequestEvent(RequestEvent requestEvent) {
    if (requestEvent.getState() == State.RECEIVED) {
      Response response = requestEvent.getResponse();
      if (response == null) {
        // We should only get to this state if the RPC failed, in which
        // case we went through the RequestCallback.onError() code path
        // already and we don't need to do any additional error handling
        // here, but we don't want to throw further exceptions.
        return;
      }
      if (Response.SC_UNAUTHORIZED == response.getStatusCode()) {
        String loginUrl = response.getHeader("login");
        Location.replace(loginUrl);
      }
      String newUser = response.getHeader("userId");
      if (lastSeenUser == null) {
        lastSeenUser = newUser;
      } else if (!lastSeenUser.equals(newUser)) {
        // A new user has logged in, just reload the app and start over
        Location.reload();
      }
    }
  }

}
