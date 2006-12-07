/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.i18n.rebind.util;

import com.google.gwt.core.ext.typeinfo.JClassType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * Creates resources.
 */
public abstract class ResourceFactory {
  static class SimplePathTree extends AbstractPathTree {
    String path;

    SimplePathTree(String path) {
      this.path = path;
    }

    public AbstractPathTree getChild(int i) {
      throw new UnsupportedOperationException(
          "Simple paths have no children, therefore cannot get child: " + i);
    }

    public String getPath() {
      return path;
    }

    public int numChildren() {
      return 0;
    }
  }

  private abstract static class AbstractPathTree {
    abstract AbstractPathTree getChild(int i);

    abstract String getPath();

    abstract int numChildren();
  }

  private static class ClassPathTree extends AbstractPathTree {
    Class javaInterface;

    ClassPathTree(Class javaInterface) {
      this.javaInterface = javaInterface;
    }

    AbstractPathTree getChild(int i) {
      // we expect to do this at most once, so no caching is used.
      return new ClassPathTree(javaInterface.getInterfaces()[i]);
    }

    String getPath() {
      return javaInterface.getName();
    }

    int numChildren() {
      return javaInterface.getInterfaces().length;
    }
  }

  private static class JClassTypePathTree extends AbstractPathTree {
    JClassType javaInterface;

    JClassTypePathTree(JClassType javaInterface) {
      this.javaInterface = javaInterface;
    }

    AbstractPathTree getChild(int i) {
      // we expect to do this at most once, so no caching is used.
      return new JClassTypePathTree(javaInterface.getImplementedInterfaces()[i]);
    }

    /**
     * Path is equivalent to javaInterface.getQualifiedName() except for inner
     * classes.
     * 
     * @see com.google.gwt.i18n.rebind.util.ResourceFactory.AbstractPathTree#getPath()
     */
    String getPath() {
      String name = getResourceName(javaInterface);
      String packageName = javaInterface.getPackage().getName();
      return packageName + "." + name;
    }

    int numChildren() {
      return javaInterface.getImplementedInterfaces().length;
    }
  }

  public static final AbstractResource NOT_FOUND = new AbstractResource() {

    void addToKeySet(Set s) {
      throw new IllegalStateException("Not found resource");
    }

    Object handleGetObject(String key) {
      throw new IllegalStateException("Not found resource");
    }
  };

  private static Map cache = new HashMap();

  private static List loaders = new ArrayList();
  static {
    loaders.add(new LocalizedPropertiesResource.Factory());
  }

  /**
   * Clears the resource cache.
   */
  public static void clearCache() {
    cache.clear();
  }

  /**
   * Gets the resource associated with the given interface.
   * 
   * @param javaInterface interface
   * @param locale locale
   * @return the resource
   */
  public static AbstractResource getBundle(Class javaInterface, Locale locale) {
    if (javaInterface.isInterface() == false) {
      throw new IllegalArgumentException(javaInterface
          + " should be an interface.");
    }
    ClassPathTree path = new ClassPathTree(javaInterface);
    return getBundleAux(path, locale, true);
  }

  /**
   * Gets the resource associated with the given interface.
   * 
   * @param javaInterface interface
   * @param locale locale
   * @return the resource
   */
  public static AbstractResource getBundle(JClassType javaInterface,
      Locale locale) {
    return getBundleAux(new JClassTypePathTree(javaInterface), locale, true);
  }

  /**
   * Gets the resource associated with the given path.
   * 
   * @param path the path
   * @param locale locale
   * @return the resource
   */
  public static AbstractResource getBundle(String path, Locale locale) {
    return getBundleAux(new SimplePathTree(path), locale, true);
  }

  public static String getResourceName(JClassType targetClass) {
    String name = targetClass.getName();
    if (targetClass.isMemberType()) {
      name = name.replace('.', '$');
    }
    return name;
  }

  private static List findAlternativeParents(
      ResourceFactory.AbstractPathTree tree, Locale locale) {
    List altParents = null;
    if (tree != null) {
      altParents = new ArrayList();
      for (int i = 0; i < tree.numChildren(); i++) {
        ResourceFactory.AbstractPathTree child = tree.getChild(i);
        AbstractResource altParent = getBundleAux(child, locale, false);
        if (altParent != null) {
          altParents.add(altParent);
        }
      }
    }
    return altParents;
  }

  private static AbstractResource findPrimaryParent(
      ResourceFactory.AbstractPathTree tree, Locale locale) {
    // Create bundle
    AbstractResource parent = null;

    // If we are not in the default case, calculate parent
    if (locale != null) {
      if (!("".equals(locale.getVariant()))) {
        // parents, by default, share the list of alternativePaths;
        parent = getBundleAux(tree, new Locale(locale.getLanguage(),
            locale.getCountry()), false);
      } else if (!"".equals(locale.getCountry())) {
        parent = getBundleAux(tree, new Locale(locale.getLanguage()), false);
      }

      // If either no country was defined or no bundle was found, use null
      // locale instead.
      if (parent == null) {
        parent = getBundleAux(tree, null, false);
      }
    }
    return parent;
  }

  private static AbstractResource getBundleAux(
      ResourceFactory.AbstractPathTree tree, Locale locale, boolean required) {
    String targetPath = tree.getPath();
    ClassLoader loader = AbstractResource.class.getClassLoader();
    // Calculate baseName
    String localizedPath = targetPath;
    if (locale != null) {
      localizedPath = targetPath + "_" + locale;
    }
    AbstractResource result = (AbstractResource) cache.get(localizedPath);
    if (result != null) {
      if (result == NOT_FOUND) {
        return null;
      } else {
        return result;
      }
    }
    String partualPath = localizedPath.replace('.', '/');
    AbstractResource parent = findPrimaryParent(tree, locale);
    List altParents = findAlternativeParents(tree, locale);

    AbstractResource found = null;
    for (int i = 0; i < loaders.size(); i++) {
      ResourceFactory element = (ResourceFactory) loaders.get(i);
      String path = partualPath + "." + element.getExt();
      InputStream m = loader.getResourceAsStream(path);
      if (m != null) {
        found = element.load(m);
        found.setPath(partualPath);
        found.setPrimaryParent(parent);
        found.setLocale(locale);
        for (int j = 0; j < altParents.size(); j++) {
          AbstractResource altParent = (AbstractResource) altParents.get(j);
          found.addAlternativeParent(altParent);
        }
        found.checkKeys();
        break;
      }
    }
    if (found == null) {
      if (parent != null) {
        found = parent;
      } else {
        found = NOT_FOUND;
      }
      found = parent;
    }

    cache.put(localizedPath, found);
    if (found == null && required) {
      throw new MissingResourceException(
          "Could not find any resource associated with " + tree.getPath(),
          null, null);
    }
    return found;
  }

  abstract String getExt();

  abstract AbstractResource load(InputStream m);
}
