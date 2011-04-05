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
package com.google.gwt.sample.gaerequest.shared;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ServiceName;

/**
 * Makes requests of the Google AppEngine UserService.
 */
@ServiceName(value = "com.google.gwt.sample.gaerequest.server.UserServiceWrapper", 
    locator = "com.google.gwt.sample.gaerequest.server.UserServiceLocator")
public interface GaeUserServiceRequest extends RequestContext {
  public Request<String> createLoginURL(String destinationURL);

  public Request<String> createLogoutURL(String destinationURL);

  public Request<GaeUser> getCurrentUser();
}
