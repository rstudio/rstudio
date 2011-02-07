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
package com.google.gwt.user.server;

import java.lang.annotation.Annotation;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * A bunch of useful methods.
 */
public class Util {

  /**
   * Find an instance of the specified annotation, walking up the inheritance
   * tree if necessary. Copied from {@link
   * com.google.gwt.i18n.rebind.AnnotationUtil}.
   *
   * <p>The super chain is walked first, so if an ancestor superclass has the
   * requested annotation, it will be preferred over a directly implemented
   * interface.
   *
   * @param <T> Annotation type to search for
   * @param clazz root class to search, may be null
   * @param annotationClass class object of Annotation subclass to search for
   * @return the requested annotation or null if none
   */
  public static <T extends Annotation> T getClassAnnotation(Class<?> clazz,
      Class<T> annotationClass) {
    if (clazz == null) {
      return null;
    }
    T annot = clazz.getAnnotation(annotationClass);
    if (annot == null) {
      annot = getClassAnnotation(clazz.getSuperclass(), annotationClass);
      if (annot != null) {
        return annot;
      }
      for (Class<?> intf : clazz.getInterfaces()) {
        annot = getClassAnnotation(intf, annotationClass);
        if (annot != null) {
          return annot;
        }
      }
    }
    return annot;
  }

  /**
   * Retrieves named cookie from supplied request. If {@code allowDuplicates} is
   * set to {@code true}, method will throw {@link IllegalStateException} if
   * duplicate cookies are found, which can be a sign of a cookie overwrite
   * attack.
   *
   * @param request HTTP request to retrieve cookie from.
   * @param cookieName Cookie name.
   * @param allowDuplicates if {@code true} duplicate cookies are allowed,
   *        otherwise {@link IllegalStateException} is thrown if duplicate
   *        cookies are detected.
   * @return {@link Cookie} if specified cookie is present, {@code null}
   *         otherwise.
   * @throws IllegalArgumentException if duplicate cookies are detected.
   */
  public static Cookie getCookie(HttpServletRequest request,
      String cookieName, boolean allowDuplicates) {
    Cookie cookieToReturn = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookieName.equals(cookie.getName())) {
          // ensure that it's the only one cookie, since duplicate cookies
          // can be a sign of a cookie overriding attempt.
          if (cookieToReturn == null) {
            if (allowDuplicates) {
              // do not attempt to detect duplicate cookies
              return cookie;
            } else {
              cookieToReturn = cookie;
            }
          } else {
            throw new IllegalArgumentException("Duplicate cookie! " +
                "Cookie override attack?");
          }
        }
      }
    }
    return cookieToReturn;
  }

  private Util() {
  }
}
