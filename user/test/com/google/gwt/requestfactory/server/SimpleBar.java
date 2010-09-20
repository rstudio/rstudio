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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Domain object for SimpleFooRequest.
 */
public class SimpleBar {
  /**
   * DO NOT USE THIS UGLY HACK DIRECTLY! Call {@link #get} instead.
   */
  private static Map<String, SimpleBar> jreTestSingleton = new HashMap<String, SimpleBar>();
 
  private static long nextId = 2L;

  public static Long countSimpleBar() {
      return (long) get().size();
  }

  public static List<SimpleBar> findAll() {
    return new ArrayList<SimpleBar>(get().values());
  }

  public static SimpleBar findSimpleBar(String id) {
    return findSimpleBarById(id);
  }

  public static SimpleBar findSimpleBarById(String id) {
    return get().get(id);
  }

  @SuppressWarnings("unchecked")
  public static synchronized Map<String, SimpleBar> get() {
    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req == null) {
      // May be in a JRE test case, use the the singleton
      return jreTestSingleton;
    } else {
      /*
       * This will not behave entirely correctly unless we have a servlet filter
       * that doesn't allow any requests to be processed unless they're
       * associated with an existing session.
       */
      Map<String, SimpleBar> value = (Map<String, SimpleBar>) req.getSession().getAttribute(
          SimpleBar.class.getCanonicalName());
      if (value == null) {
        value = reset();
      }
      return value;
    }
  }

  public static SimpleBar getSingleton() {
    return findSimpleBar("1L");
  }

  public static synchronized Map<String, SimpleBar> reset() {
    Map<String, SimpleBar> instance = new HashMap<String, SimpleBar>();
    // fixtures
    SimpleBar s1 = new SimpleBar();
    s1.setId("1L");
    s1.isNew = false;
    instance.put(s1.getId(), s1);

    SimpleBar s2 = new SimpleBar();
    s2.setId("999L");
    s2.isNew = false;
    instance.put(s2.getId(), s2);

    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req == null) {
      jreTestSingleton = instance;
    } else {
      req.getSession().setAttribute(SimpleBar.class.getCanonicalName(),
          instance);
    }
    return instance;
  }

  static {
    reset();
  }

  Integer version = 1;

  @Id
  private String id = "999L";

  private String userName;

  private boolean isNew = true;

  public SimpleBar() {
    version = 1;
    userName = "FOO";
  }

  public String getId() {
    return id;
  }

  public String getUserName() {
    return userName;
  }

  public Integer getVersion() {
    return version;
  }

  public void persist() {
    if (isNew) {
      setId(Long.toString(nextId++));
      isNew = false;
      get().put(getId(), this);
    }
  }

  public SimpleBar persistAndReturnSelf() {
    persist();
    return this;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
