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
 * Domain object for MapValueProxy used to test entities as map values
 */
public class MapValue {
  /**
   * DO NOT USE THIS UGLY HACK DIRECTLY! Call {@link #get} instead.
   */
  private static Map<String, MapValue> jreTestSingleton = new HashMap<String, MapValue>();

  static {
    try {
    reset();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static MapValue findMapValue(String id) {
    return findMapValueById(id);
  }

  /**
   * Returns <code>null</code> if {@link #findFails} is <code>true</code>.
   */
  public static MapValue findMapValueById(String id) {
    return get().get(id);
  }

  @SuppressWarnings("unchecked")
  public static synchronized Map<String, MapValue> get() {
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
      Map<String, MapValue> value = (Map<String, MapValue>) req.getSession().getAttribute(
          MapValue.class.getCanonicalName());
      if (value == null) {
        value = resetImpl();
      }
      return value;
    }
  }

  public static MapValue getSingleton() {
    return findMapValue("1L");
  }

  static void reset() {
    resetImpl();
  }

  public static synchronized Map<String, MapValue> resetImpl() {
    Map<String, MapValue> instance = new HashMap<String, MapValue>();
    // fixtures
    MapValue s1 = new MapValue();
    s1.setId("1L");
    instance.put(s1.getId(), s1);

    MapValue s2 = new MapValue();
    s2.setId("999L");
    instance.put(s2.getId(), s2);

    HttpServletRequest req = RequestFactoryServlet.getThreadLocalRequest();
    if (req == null) {
      jreTestSingleton = instance;
    } else {
      req.getSession().setAttribute(MapValue.class.getCanonicalName(),
          instance);
    }
    return instance;
  }

  private String id;
  private SimpleBar simple;
  Integer version = 1;

  public MapValue() {
    id = "432234";
    simple = SimpleBar.getSingleton();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SimpleBar getSimple() {
    return simple;
  }

  public void setSimple(SimpleBar simple) {
    this.simple = simple;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

}
