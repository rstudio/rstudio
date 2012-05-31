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
package com.google.web.bindery.requestfactory.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * Domain object for SimpleFooRequest.
 */
public class SimpleBar implements HasId {
  /**
   * DO NOT USE THIS UGLY HACK DIRECTLY! Call {@link #get} instead.
   */
  private static Map<String, SimpleBar> jreTestSingleton = new HashMap<String, SimpleBar>();

  private static long nextId = 2L;

  static {
    try {
    reset();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static Long countSimpleBar() {
    return (long) get().size();
  }

  public static List<SimpleBar> findAll() {
    return new ArrayList<SimpleBar>(get().values());
  }

  public static Set<SimpleBar> findAsSet() {
    return new HashSet<SimpleBar>(get().values());
  }

  public static SimpleBar findSimpleBar(String id) {
    return findSimpleBarById(id);
  }

  /**
   * Returns <code>null</code> if {@link #findFails} is <code>true</code>.
   */
  public static SimpleBar findSimpleBarById(String id) {
    SimpleBar toReturn = get().get(id);
    return (toReturn == null || toReturn.findFails) ? null : toReturn;
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
        value = resetImpl();
      }
      return value;
    }
  }

  public static SimpleBar getSingleton() {
    return findSimpleBar("1L");
  }

  public static SimpleBar returnFirst(List<SimpleBar> list) {
    SimpleBar toReturn = list.get(0);
    return toReturn;
  }

  // Called from SimpleFoo.reset()
  static void reset() {
    resetImpl();
  }

  private static synchronized Map<String, SimpleBar> resetImpl() {
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

  Integer version = 1;

  private String id = "999L";
  private boolean findFails;
  private boolean isNew = true;
  private boolean unpersisted;
  private String userName;

  public SimpleBar() {
    version = 1;
    userName = "FOO";
  }

  public void delete() {
    get().remove(getId());
  }

  public Boolean getFindFails() {
    return findFails;
  }

  public String getId() {
    return unpersisted ? null : id;
  }

  public Boolean getUnpersisted() {
    return unpersisted;
  }

  public String getUserName() {
    return userName;
  }

  public Integer getVersion() {
    return unpersisted ? null : version;
  }

  public void persist() {
    if (isNew) {
      setId(Long.toString(nextId++));
      isNew = false;
      get().put(getId(), this);
    }
    version++;
  }

  public SimpleBar persistAndReturnSelf() {
    persist();
    return this;
  }

  public void setFindFails(Boolean fails) {
    this.findFails = fails;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setUnpersisted(Boolean unpersisted) {
    this.unpersisted = unpersisted;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
