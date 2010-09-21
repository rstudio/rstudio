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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p> <span style="color:red">Experimental API: This class is still under rapid development, and is
 * very likely to be deleted. Use it at your own risk. </span> </p> Represents one piece in a
 * property reference sequence.
 */
public class RequestProperty implements Iterable<RequestProperty> {

  /**
   * Merge two property chains.
   */
  public static RequestProperty coalesce(RequestProperty... properties) {
    assert properties.length > 0;
    RequestProperty root = new RequestProperty("");
    for (RequestProperty prop : properties) {
      if ("".equals(prop.getPropertyName())) {
        for (RequestProperty p : prop) {
          root.mergeProperty(p);
        }
      } else {
        root.mergeProperty(prop);
      }
    }
    return root;
  }

  public static RequestProperty parse(String selectors) {
    String parts[] = selectors.split("\\s*,\\s*");
    RequestProperty props[] = new RequestProperty[parts.length];
    for (int i = 0; i < parts.length; i++) {
      RequestProperty newProp = new RequestProperty("");
      newProp.parseInternal(parts[i]);
      props[i] = newProp;
    }
    return props.length == 1 ? props[0] : coalesce(props);
  }

  private String propertyName;
  private Map<String, RequestProperty> subProperties;

  private RequestProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  public RequestProperty add(RequestProperty propertyRef) {
    if (subProperties == null) {
      subProperties = new HashMap<String, RequestProperty>();
    }
    subProperties.put(propertyRef.getPropertyName(), propertyRef);
    return this;
  }

  public RequestProperty getProperty(String propName) {
    return subProperties == null ? null : subProperties.get(propName);
  }

  public String getPropertyName() {
    return propertyName;
  }

  public boolean hasProperty(String name) {
    return subProperties == null ? false : subProperties.containsKey(name);
  }

  public Iterator<RequestProperty> iterator() {
    return subProperties == null ? emptyIterator()
        : subProperties.values().iterator();
  }

  public RequestProperty mergeProperty(RequestProperty property) {
    RequestProperty foundProp = getProperty(property.getPropertyName());
    if (foundProp == null && !"".equals(property.getPropertyName())) {
      add(property);
    } else {
      for (RequestProperty p : property) {
        if (foundProp == null) {
          add(p);
        } else {
          foundProp.mergeProperty(p);
        }
      }
    }
    return foundProp;
  }

  @SuppressWarnings({"cast", "unchecked"})
  private Iterator<RequestProperty> emptyIterator() {
    return (Iterator<RequestProperty>) Collections.EMPTY_MAP.values().iterator();
  }

  private RequestProperty getOrCreate(String part) {
    RequestProperty prop = getProperty(part);
    if (prop == null) {
      prop = new RequestProperty(part);
      add(prop);
    }
    return prop;
  }

  private RequestProperty parseInternal(String sequence) {
    int dotIndex = sequence.indexOf('.');
    String part = dotIndex > -1 ? sequence.substring(0, dotIndex) : sequence;
    RequestProperty prop = getOrCreate(part);
    add(prop);

    if (dotIndex > -1) {
      if (dotIndex < sequence.length() - 1) {
        String next = sequence.substring(dotIndex + 1);
        if ("".equals(next)) {
          throw new IllegalArgumentException("Empty property name '..' not allowed in " + sequence);
        }
        if (next.length() > 0) {
          return prop.parseInternal(next);
        }
      }
      throw new IllegalArgumentException("Trailing '.' in with() call " + sequence);
    }
    return prop;
  }
}
