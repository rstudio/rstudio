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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Domain object for MapKeyProxy used to test entities as map keys
 */
public class MapKey {

  /**
   * DO NOT USE THIS UGLY HACK DIRECTLY! Call {@link #get} instead.
   */
  private static Map<String, MapKey> jreTestSingleton = new HashMap<String, MapKey>();

  static {
    try {
      reset();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static MapKey findMapKey(String id) {
    return findMapKeyById(id);
  }

  /**
   * Returns <code>null</code> if {@link #findFails} is <code>true</code>.
   */
  public static MapKey findMapKeyById(String id) {
    return get().get(id);
  }

  @SuppressWarnings("unchecked")
  public static synchronized Map<String, MapKey> get() {
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
      Map<String, MapKey> value = (Map<String, MapKey>) req.getSession().getAttribute(
          MapKey.class.getCanonicalName());
      if (value == null) {
        value = resetImpl();
      }
      return value;
    }
  }

  public static MapKey getSingleton() {
    return findMapKey("1L");
  }

  static void reset() {
    resetImpl();
  }

  public static synchronized Map<String, MapKey> resetImpl() {
    Map<String, MapKey> instance = new HashMap<String, MapKey>();
    // fixtures
    MapKey m1 = new MapKey();
    m1.setId("1L");
    instance.put(m1.getId(), m1);

    MapKey m2 = new MapKey();
    m2.setId("999L");
    instance.put(m2.getId(), m2);

    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req == null) {
      jreTestSingleton = instance;
    } else {
      req.getSession().setAttribute(MapKey.class.getCanonicalName(),
          instance);
    }
    return instance;
  }

  private String id = "999L";
  private SimpleValue simple;
  Integer version = 1;

  public MapKey() {
    simple = new SimpleValue();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SimpleValue getSimple() {
    return simple;
  }

  public void setSimple(SimpleValue simple) {
    this.simple = simple;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

}
