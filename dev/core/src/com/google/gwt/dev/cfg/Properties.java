/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.cfg;

import java.lang.reflect.InvocationTargetException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A typed map of deferred binding properties.
 */
public class Properties {

  private final SortedSet<BindingProperty> bindingProps = new TreeSet<BindingProperty>();

  private final SortedSet<ConfigurationProperty> configProps = new TreeSet<ConfigurationProperty>();

  private final SortedMap<String, Property> map = new TreeMap<String, Property>();

  /**
   * Adds a previously created deferred-binding property.
   */
  public void addBindingProperty(BindingProperty bindingProperty) {
    bindingProps.add(bindingProperty);
  }

  /**
   * Creates the specified deferred-binding property, or returns an existing one
   * by the specified name if present.
   */
  public BindingProperty createBinding(String name) {
    BindingProperty prop = create(name, BindingProperty.class);
    bindingProps.add(prop);
    return prop;
  }

  /**
   * Creates the specified configuration property, or returns an existing one by
   * the specified name if present.
   */
  public ConfigurationProperty createConfiguration(String name,
      boolean allowMultipleValues) {
    ConfigurationProperty prop = create(name, allowMultipleValues,
        ConfigurationProperty.class);
    configProps.add(prop);
    return prop;
  }

  public Property find(String name) {
    return map.get(name);
  }

  /**
   * Returns the property if (and only if) it's a BindingProperty, otherwise null.
   */
  public BindingProperty findBindingProp(String propName) {
    Property p = map.get(propName);
    if (p instanceof BindingProperty) {
      return (BindingProperty) p;
    } else {
      return null;
    }
  }

  /**
   * Returns the property if (and only if) it's a ConfigurationProperty, otherwise null.
   */
  public ConfigurationProperty findConfigProp(String propName) {
    Property p = map.get(propName);
    if (p instanceof ConfigurationProperty) {
      return (ConfigurationProperty) p;
    } else {
      return null;
    }
  }

  /**
   * Gets all deferred binding properties in sorted order.
   */
  public SortedSet<BindingProperty> getBindingProperties() {
    return bindingProps;
  }

  public SortedSet<ConfigurationProperty> getConfigurationProperties() {
    return configProps;
  }

  private <T extends Property> T create(String name, boolean flag,
      boolean useFlagArgument, Class<T> clazz) {
    if (clazz == null) {
      throw new NullPointerException("clazz");
    } else if (name == null) {
      throw new NullPointerException("name");
    }

    Property property = find(name);
    if (property != null) {
      try {
        return clazz.cast(property);
      } catch (ClassCastException e) {
        throw new IllegalArgumentException("Cannot create property " + name
            + " because one of another type ("
            + property.getClass().getSimpleName() + ") already exists.");
      }
    }

    Exception ex;
    try {
      T newInstance;
      if (useFlagArgument) {
        newInstance = clazz.getConstructor(String.class, boolean.class).newInstance(
            name, flag);
      } else {
        newInstance = clazz.getConstructor(String.class).newInstance(name);
      }
      map.put(name, newInstance);
      return newInstance;
    } catch (NoSuchMethodException e) {
      ex = e;
    } catch (InstantiationException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    } catch (InvocationTargetException e) {
      ex = e;
    }

    throw new RuntimeException("Unable to create Property instance", ex);
  }

  private <T extends Property> T create(String name, boolean flag,
      Class<T> clazz) {
    return create(name, flag, true, clazz);
  }

  private <T extends Property> T create(String name, Class<T> clazz) {
    return create(name, false, false, clazz);
  }
}
