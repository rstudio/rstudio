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

  private static String userInformationImplClass = "";

  /**
   * Returns the current user information for a given redirect URL. If
   * {@link #setUserInformationImplClass(String)} has been called with a class
   * name, that class is used to gather the information by calling a (String)
   * constructor. If the impl class name is "", or if the class cannont be
   * instantiated, dummy user info is returned.
   * 
   * @param redirectUrl the redirect URL as a String
   * @return a {@link UserInformation} instance
   */
  public static UserInformation getCurrentUserInformation(String redirectUrl) {
    UserInformation userInfo = null;
    if (!"".equals(userInformationImplClass)) {
      try {
        userInfo = (UserInformation) Class.forName(
            userInformationImplClass).getConstructor(
                String.class).newInstance(redirectUrl);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (userInfo == null) {
      userInfo = new UserInformationSimpleImpl(redirectUrl);
    }
    return userInfo;
  }

  /**
   * Sets the implementation class to be used to gather user information
   * in {@link #getCurrentUserInformation(String)}.
   *
   * @param clazz a class name
   */
  public static void setUserInformationImplClass(String clazz) {
    userInformationImplClass = clazz;
  }

  /**
   * The redirect URL as a String.
   */
  protected String redirectUrl = "";
  private Integer version = 0;
  
  /**
   * Constructs a new {@link UserInformation} instance.
   *
   * @param redirectUrl the redirect URL as a String
   */
  public UserInformation(String redirectUrl) {
    if (redirectUrl != null) {
      this.redirectUrl = redirectUrl;
    }
  }

  /**
   * Returns the user's email address.
   *
   * @return the user's email address as a String
   */
  public abstract String getEmail();
  
  /**
   * Returns the user's id.
   *
   * @return the user's id as a Long
   * @see #setId(Long)
   */
  public abstract Long getId();
  
  /**
   * Returns the user's login URL.
   *
   * @return the user's login URL as a String
   */
  public abstract String getLoginUrl();
  
  /**
   * Returns the user's logout URL.
   *
   * @return the user's logout URL as a String
   */
  public abstract String getLogoutUrl();
  
  /**
   * Returns the user's name.
   *
   * @return the user's name as a String
   */
  public abstract String getName();

  /**
   * Returns the version of this instance.
   *
   * @return an Integer version number
   * @see #setVersion(Integer)
   */
  public Integer getVersion() {
    return this.version;
  }
  
  /**
   * Returns whether the user is logged in.
   *
   * @return {@code true} if the user is logged in
   */
  public abstract boolean isUserLoggedIn();

  /**
   * Sets the id for this user.
   *
   * @param id a String id
   * @see #getId()
   */
  public abstract void setId(Long id);
  
  /**
   * Sets the version of this instance.
   *
   * @param version an Integer version number
   * @see #getVersion()
   */
  public void setVersion(Integer version) {
    this.version = version;
  }
}
