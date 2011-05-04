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
package com.google.gwt.sample.mobilewebapp.server.domain;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * Wrapper around the {@link com.google.appengine.api.users.UserService} GAE
 * api.
 */
public class UserServiceWrapper {

  public static UserServiceWrapper instance = new UserServiceWrapper();

  public static UserServiceWrapper get() {
    return instance;
  }

  private UserServiceWrapper() {
  }

  /**
   * Get the current user.
   * {@link com.google.gwt.sample.gaerequest.server.GaeAuthFilter} should ensure
   * that the user is logged in, but we take precautions here as well.
   * 
   * @return the current user
   * @throws RuntimeException if the user is not logged in
   */
  public User getCurrentUser() {
    User currentUser = UserServiceFactory.getUserService().getCurrentUser();
    if (currentUser == null) {
      throw new RuntimeException("User not logged in");
    }
    return currentUser;
  }

  /**
   * Get the unique ID of the current user.
   * 
   * @return a unique ID
   * @throws RuntimeException if the user is not logged in
   */
  public String getCurrentUserId() {
    return getCurrentUser().getUserId();
  }
}
