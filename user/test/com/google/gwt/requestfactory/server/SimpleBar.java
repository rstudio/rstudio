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

import com.google.gwt.requestfactory.shared.Id;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Domain object for SimpleFooRequest.
 */
public class SimpleBar {

  /**
   * This is an ugly hack.
   */
  static ThreadLocal<SimpleBar> singleton = new ThreadLocal<SimpleBar>() {
    @Override
    protected SimpleBar initialValue() {
      SimpleBar value = null;
      HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
      // May be in a JRE test case
      if (req != null) {
        value = (SimpleBar) req.getSession().getAttribute(
            SimpleBar.class.getCanonicalName());
      }
      if (value == null) {
        value = reset();
      }
      return value;
    }
  };

  private static Long nextId = 1L;

  public static Long countSimpleBar() {
    return 1L;
  }

  public static List<SimpleBar> findAll() {
    return Collections.singletonList(singleton.get());
  }

  public static SimpleBar findSimpleBar(Long id) {
    return findSimpleBarById(id);
  }

  public static SimpleBar findSimpleBarById(Long id) {
    singleton.get().setId(id);
    return singleton.get();
  }

  public static SimpleBar getSingleton() {
    return singleton.get();
  }

  public static SimpleBar reset() {
    SimpleBar instance = new SimpleBar();
    singleton.set(instance);
    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req != null) {
      req.getSession().setAttribute(SimpleBar.class.getCanonicalName(),
          instance);
    }
    return instance;
  }

  Integer version = 1;

  @Id
  private Long id = 1L;

  private String userName;

  public SimpleBar() {
    version = 1;
    userName = "FOO";
  }

  public Long getId() {
    return id;
  }

  public String getUserName() {
    return userName;
  }

  public Integer getVersion() {
    return version;
  }

  public void persist() {
    setId(nextId++);
    singleton.get().setUserName(userName);
  }

  public SimpleBar persistAndReturnSelf() {
    persist();
    return this;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
