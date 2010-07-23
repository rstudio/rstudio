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

import com.google.gwt.requestfactory.shared.UserInformation;

/**
 * A base class for providing authentication related information about the
 * user. This implementation treats the user as constantly signed in, and
 * without any information. Services that want real authentication should
 * subclass this class.
 */
public class UserInformationImpl implements UserInformation {
  public static UserInformationImpl getCurrentUserInformation() {
    return new UserInformationImpl();
  }
  
  private Long id = 0L;
  private Integer version = 0;
  
  public UserInformationImpl() {
  }
  
  public String getEmail() {
    return "";
  }

  public Long getId() {
    return this.id;
  }

  public String getLoginUrl() {
    return "";
  }
  
  public String getLogoutUrl() {
    return "";
  }
  
  public String getName() {
    return "";
  }
  
  public Integer getVersion() {
    return this.version;
  }
  
  public boolean isUserLoggedIn() {
    return true;
  }

  public void setId(Long id) {
    this.id = id;
  }
  
  public void setVersion(Integer version) {
    this.version = version;
  }
}
