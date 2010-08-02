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
 * A base class for providing authentication related information about the
 * user. Services that want real authentication should subclass this class.
 */
public abstract class UserInformation {
  
  /**
   * This implementation treats the user as constantly signed in, and
   * without any information.
   */
  private static class UserInformationSimpleImpl extends UserInformation {
    private Long id = 0L;
  
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
  
    public boolean isUserLoggedIn() {
      return true;
    }

    public void setId(Long id) {
      this.id = id;
    }
  }
  
  private static String userInformationImplClass = "";
  
  public static UserInformation getCurrentUserInformation() {
    UserInformation userInfo = null;
    if (!userInformationImplClass.isEmpty()) {
      try {
        userInfo = 
          (UserInformation) Class.forName(userInformationImplClass).newInstance();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (userInfo == null) {
      userInfo = new UserInformationSimpleImpl();
    }
    return userInfo;
  }

  public static void setUserInformationImplClass(String clazz) {
    userInformationImplClass = clazz;
  }

  private Integer version = 0;

  public abstract String getEmail();
  public abstract Long getId();
  public abstract String getLoginUrl();
  public abstract String getLogoutUrl();
  public abstract String getName();

  public Integer getVersion() {
    return this.version;
  }
  
  public abstract boolean isUserLoggedIn();
  public abstract void setId(Long id);
  
  public void setVersion(Integer version) {
    this.version = version;
  }
}
