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
package com.google.gwt.requestfactory.server;

/**
 * This implementation treats the user as constantly signed in, and
 * without any information.
 */
public class UserInformationSimpleImpl extends UserInformation {

  private String id = "";

  public UserInformationSimpleImpl(String redirectUrl) {
    super(redirectUrl);
  }

  @Override
  public String getEmail() {
    return "Dummy Email";
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getLoginUrl() {
    return "";
  }

  @Override
  public String getLogoutUrl() {
    return "";
  }

  @Override
  public String getName() {
    return "Dummy User";
  }

  @Override
  public boolean isUserLoggedIn() {
    return true;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }
}
