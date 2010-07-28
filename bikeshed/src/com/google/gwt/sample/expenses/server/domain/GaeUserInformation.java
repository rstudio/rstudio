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

package com.google.gwt.sample.expenses.server.domain;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.requestfactory.server.UserInformationImpl;

/**
 * A user information class that uses the Google App Engine authentication
 * framework.
 */
public class GaeUserInformation extends UserInformationImpl {
  private static UserService userService = UserServiceFactory.getUserService();

  public static GaeUserInformation getCurrentUserInformation() {
    return new GaeUserInformation();
  }
  
  public String getEmail() {
    User user = userService.getCurrentUser();
    if (user == null) {
      return "";
    }
    return user.getEmail();
  }

  public Long getId() {
    User user = userService.getCurrentUser();
    if (user == null) {
      return 0L;
    }
    return new Long(user.hashCode());
  }
  
  public String getLoginUrl() {
    return userService.createLoginURL("RETURN_URL");
  }
  
  public String getLogoutUrl() {
    return userService.createLogoutURL("RETURN_URL");
  }
  
  public String getName() {
    User user = userService.getCurrentUser();
    if (user == null) {
      return "";
    }
    return user.getNickname();
  }
  
  public boolean isUserLoggedIn() {
    return userService.isUserLoggedIn();
  }
  
}
