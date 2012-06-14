/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.core.server;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.core.shared.GWTBridge;
import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;
import com.google.gwt.i18n.shared.Localizable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements GWT.* methods for the server.
 */
public class ServerGwtBridge extends GWTBridge {

  /**
   * Something that knows how to provide an instance of a requested class.
   */
  public interface ClassInstantiator {

    /**
     * Create an instance given a base class.  The created class may be a
     * subtype of the requested class.
     * 
     * @param <T>
     * @param baseClass
     * @param properties 
     * @return instance or null if unable to create
     */
    <T> T create(Class<?> baseClass, Properties properties);
  }

  /**
   * Helper class that provides some wrappers for looking up and instantiating
   * a class.
   */
  public abstract static class ClassInstantiatorBase implements ClassInstantiator {

    /**
     * @param <T>
     * @param clazz
     * @return class instance or null
     */
    protected <T> T tryCreate(Class<T> clazz) {
      try {
        T obj = clazz.newInstance();
        return obj;
      } catch (InstantiationException e) {
      } catch (IllegalAccessException e) {
      }
      return null;
    }

    /**
     * @param <T>
     * @param className
     * @return class instance or null
     */
    protected <T> T tryCreate(String className) {
      try {
        Class<?> clazz = Class.forName(className);
        @SuppressWarnings("unchecked")
        T obj = (T) tryCreate(clazz);
        return obj;
      } catch (ClassNotFoundException e) {
      }
      return null;
    }
  }

  /**
   * An interface for accessing property values.
   */
  public interface Properties {

    /**
     * Get the value of a property.
     * 
     * @param name
     * @return property value, or null
     */
    String getProperty(String name);
  }

  /**
   * A node in the tree of registered classes, keeping track of class
   * instantiators for each type.  {@link Object} is at the root of the
   * tree, and children are not related to each other but inherit from
   * their parent.
   */
  private static class Node {
    public final Class<?> type;
    public final ArrayList<Node> children;
    public final ArrayList<ClassInstantiator> instantiators;

    public Node(Class<?> type) {
      this.type = type;
      children = new ArrayList<Node>();
      instantiators = new ArrayList<ClassInstantiator>();
    }
  }

  private static class PropertiesImpl implements Properties {
    private final Object lock = new Object[0];
    private final Map<String, String> map = new HashMap<String, String>();

    @Override
    public String getProperty(String name) {
      synchronized (lock) {
        return map.get(name);
      }
    }

    public void setProperty(String name, String value) {
      synchronized (lock) {
        map.put(name, value);
      }
    }
  }

  /**
   * Lookup a property first in thread-local properties, then in global
   * properties.
   */
  private class PropertyLookup implements Properties {

    @Override
    public String getProperty(String name) {
      String val = threadProperties.get().getProperty(name);
      if (val == null) {
        val = globalProperties.getProperty(name);
      }
      return val;
    }
  }

  private static Object instanceLock = new Object[0];
  private static ServerGwtBridge instance = null;

  private static final GwtLocaleFactory factory = new GwtLocaleFactoryImpl();

  private static final Logger LOGGER = Logger.getLogger(ServerGwtBridge.class.getName());

  /**
   * Get the singleton {@link ServerGwtBridge} instance, creating it if
   * necessary.  The instance will be registered via
   * {@link GWT#setBridge(GWTBridge)} and will have the default instantiators
   * registered on it.
   *  
   * @return the singleton {@link ServerGwtBridge} instance
   */
  public static ServerGwtBridge getInstance() {
    synchronized (instanceLock) {
      if (instance == null) {
        instance = new ServerGwtBridge();
        GWT.setBridge(instance);
      }
      return instance;
    }
  }

  public static GwtLocale getLocale(Properties properties) {
    String propVal = properties.getProperty("locale");
    if (propVal == null) {
      propVal = "default";
    }
    return factory.fromString(propVal);
  }

  /**
   * Root of the tree of registered classes and their instantiators.
   */
  private final Node root = new Node(Object.class);

  // lock for instantiators
  private final Object instantiatorsLock = new Object[0];

  private final ThreadLocal<PropertiesImpl> threadProperties;

  private final PropertiesImpl globalProperties = new PropertiesImpl();

  private Properties properties = new PropertyLookup();

  // @VisibleForTesting
  ServerGwtBridge() {
    threadProperties = new ThreadLocal<PropertiesImpl>() {
      @Override
      protected PropertiesImpl initialValue() {
        return new PropertiesImpl();
      }
    };

    // register built-in instantiators
    register(Object.class, new ObjectNew());
    register(Localizable.class, new LocalizableInstantiator());
  }

  @Override
  public <T> T create(Class<?> classLiteral) {
    synchronized (instantiatorsLock) {
      // Start at the root, and find the bottom-most node that our type
      // is assignable to.
      Stack<Node> stack = new Stack<Node>();
      stack.push(root);
      boolean found;
      do {
        found = false;
        Node node = stack.peek();
        for (Node child : node.children) {
          if (child.type.isAssignableFrom(classLiteral)) {
            found = true;
            stack.push(child);
            break;
          }
        }
      } while (found);

      // Try each instantiator until we find one that can create the
      // type, walking up the tree.
      while (!stack.isEmpty()) {
        Node node = stack.pop();
        for (ClassInstantiator inst : node.instantiators) {
          T obj = inst.<T>create(classLiteral, properties);
          if (obj != null) {
            return obj;
          }
        }
      }
      throw new RuntimeException("No instantiator created " + classLiteral.getCanonicalName());
    }
  }

  /**
   * Get the value of the named property, preferring a value specific to this
   * thread (see {@link #setThreadProperty(String, String)}) over one that is
   * set globally (see {@link #setGlobalProperty(String, String)}).
   * 
   * @param property
   * @return the property's value or null if none
   */
  public String getProperty(String property) {
    return properties.getProperty(property);
  }

  @Override
  public String getVersion() {
    return "unknown";
  }

  @Override
  public boolean isClient() {
    return false;
  }

  @Override
  public void log(String message, Throwable e) {
    LOGGER.log(Level.INFO, message, e);
  }

  /**
   * Register an instantiator to be used for any subtypes of a given base class.
   *
   * @param baseClass
   * @param instantiator
   */
  public void register(Class<?> baseClass, ClassInstantiator instantiator) {
    synchronized (instantiatorsLock) {
      // find the deepest node which is baseClass or a supertype
      Node node = root;
      boolean found;
      do {
        found = false;
        for (Node child : node.children) {
          if (child.type.isAssignableFrom(baseClass)) {
            found = true;
            node = child;
          }
        }
      } while (found);
      // add the instantiator to the found node if it is an exact match, or
      // create a new one if not and insert it in the proper place
      Node nodeToAdd = node;
      if (node.type != baseClass) {
        nodeToAdd = new Node(baseClass);
        // check if this node's children extend baseClass, if so we need
        // to insert a new node between them
        boolean needsAdd = true;
        for (Node child : node.children) {
          if (baseClass.isAssignableFrom(child.type)) {
            nodeToAdd.children.add(child);
            int childPosition = node.children.indexOf(child);
            node.children.set(childPosition, nodeToAdd);
            needsAdd = false;
            break;
          }
        }
        if (needsAdd) {
          node.children.add(nodeToAdd);
        }
      }
      nodeToAdd.instantiators.add(0, instantiator);
    }
  }

  /**
   * Set a property value globally.  This value will be overridden by any
   * thread-specific property value of the same name.
   * 
   * @param property
   * @param value
   */
  public void setGlobalProperty(String property, String value) {
    globalProperties.setProperty(property, value);
  }

  /**
   * Set a property value for only the current thread.  This value will override
   * any global property value of the same name.
   * 
   * @param property
   * @param value
   */
  public void setThreadProperty(String property, String value) {
    threadProperties.get().setProperty(property, value);
  }
}
