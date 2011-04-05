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
package com.google.gwt.sample.gaerequest.server;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;

/**
 * Gives a RequestFactory system access to the Google AppEngine UserService.
 */
public class UserServiceLocator implements ServiceLocator {
  public UserServiceWrapper getInstance(Class<?> clazz) {
    final UserService service = UserServiceFactory.getUserService();
    return new UserServiceWrapper() {

      public String createLoginURL(String destinationURL) {
        return service.createLoginURL(destinationURL);
      }

      public String createLogoutURL(String destinationURL) {
        return service.createLogoutURL(destinationURL);
      }

      public User getCurrentUser() {
        return service.getCurrentUser();
      }
    };
  }
}
