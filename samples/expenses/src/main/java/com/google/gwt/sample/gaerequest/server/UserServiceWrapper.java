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

/**
 * Service object that reduces the visible api of
 * {@link com.google.appengine.api.users.UserService}. Needed to work around a
 * limitation of RequestFactory, which cannot yet handle overloaded service
 * methods.
 */
public interface UserServiceWrapper {
  public String createLoginURL(String destinationURL);

  public String createLogoutURL(String destinationURL);

  public User getCurrentUser();
}
