/*
 * Copyright 2007 Google Inc.
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

    @Override
    public AbstractPathTree getChild(int i) {
      throw new UnsupportedOperationException(
          "Simple paths have no children, therefore cannot get child: " + i);
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
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
    Class<?> javaInterface;

    ClassPathTree(Class<?> javaInterface) {
      this.javaInterface = javaInterface;
    }

    @Override
    AbstractPathTree getChild(int i) {
      // we expect to do this at most once, so no caching is used.
      return new ClassPathTree(javaInterface.getInterfaces()[i]);
    }

    @Override
    String getPath() {
      return javaInterface.getName();
    }

    @Override
    int numChildren() {
      return javaInterface.getInterfaces().length;
    }
  }

  private static class JClassTypePathTree extends AbstractPathTree {
    JClassType javaInterface;

    JClassTypePathTree(JClassType javaInterface) {
      this.javaInterface = javaInterface;
    }

    @Override
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
    @Override
    String getPath() {
      String name = getResourceName(javaInterface);
      String packageName = javaInterface.getPackage().getName();
      return packageName + "." + name;
    }

    @Override
    int numChildren() {
      return javaInterface.getImplementedInterfaces().length;
    }
  }

  /**
   * Represents default locale.
   */
  public static final String DEFAULT_TOKEN = "default";
  public static final char LOCALE_SEPARATOR = '_';

  public static final AbstractResource NOT_FOUND = new AbstractResource() {

    @Override
    void addToKeySet(Set<String> s) {
      throw new IllegalStateException("Not found resource");
    }

    @Override
    Object handleGetObject(String key) {
      throw new IllegalStateException("Not found resource");
    }
  };

  private static Map<String, AbstractResource> cache = new HashMap<String, AbstractResource>();

  private static List<LocalizedPropertiesResource.Factory> loaders = new ArrayList<LocalizedPropertiesResource.Factory>();
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
   * @param locale locale name
   * @return the resource
   */
  public static AbstractResource getBundle(Class<?> javaInterface, String locale) {
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
   * @param locale locale name
   * @return the resource
   */
  public static AbstractResource getBundle(JClassType javaInterface,
      String locale) {
    return getBundleAux(new JClassTypePathTree(javaInterface), locale, true);
  }

  /**
   * Gets the resource associated with the given path.
   * 
   * @param path the path
   * @param locale locale name
   * @return the resource
   */
  public static AbstractResource getBundle(String path, String locale) {
    return getBundleAux(new SimplePathTree(path), locale, true);
  }

  /**
   * Given a locale name, derives the parent's locale name. For example, if
   * the locale name is "en_US", the parent locale name would be "en". If the
   * locale name is that of a top level locale (i.e. no '_' characters, such
   * as "fr"), then the the parent locale name is that of the default locale.
   * If the locale name is null, the empty string, or is already that of the
   * default locale, then null is returned.
   *
   * @param localeName the locale name
   * @return the parent's locale name
   */
  public static String getParentLocaleName(String localeName) {
    if (localeName == null ||
        localeName.length() == 0 ||
        localeName.equals(DEFAULT_TOKEN)) {
      return null;
    }
    int pos = localeName.lastIndexOf(LOCALE_SEPARATOR);
    if (pos != -1) {
      return localeName.substring(0, pos);
    }
    return DEFAULT_TOKEN;
  }

  public static String getResourceName(JClassType targetClass) {
    String name = targetClass.getName();
    if (targetClass.isMemberType()) {
      name = name.replace('.', '$');
    }
    return name;
  }

  private static List<AbstractResource> findAlternativeParents(
      ResourceFactory.AbstractPathTree tree, String locale) {
    List<AbstractResource> altParents = null;
    if (tree != null) {
      altParents = new ArrayList<AbstractResource>();
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
      ResourceFactory.AbstractPathTree tree, String locale) {

    // If we are not in the default case, calculate parent
    if (!DEFAULT_TOKEN.equals(locale)) {
      return getBundleAux(tree, getParentLocaleName(locale), false);
    }
    return null;
  }

  private static AbstractResource getBundleAux(
      ResourceFactory.AbstractPathTree tree, String locale, boolean required) {
    String targetPath = tree.getPath();
    ClassLoader loader = AbstractResource.class.getClassLoader();

    if (locale == null || locale.length() == 0) {
      // This should never happen, since the only legitimate user of this
      // method traces back to AbstractLocalizableImplCreator. The locale
      // that is passed in from AbstractLocalizableImplCreator is produced
      // by the I18N property provider, which guarantees that the locale
      // will not be of zero length or null. However, we add this check
      // in here in the event that a future user of ResourceFactory does
      // not obey this constraint.
      locale = DEFAULT_TOKEN;
    }

    // Calculate baseName
    String localizedPath = targetPath;
    if (!DEFAULT_TOKEN.equals(locale)) {
      localizedPath = targetPath + LOCALE_SEPARATOR + locale;
    }
    AbstractResource result = cache.get(localizedPath);
    if (result != null) {
      if (result == NOT_FOUND) {
        return null;
      } else {
        return result;
      }
    }
    String partualPath = localizedPath.replace('.', '/');
    AbstractResource parent = findPrimaryParent(tree, locale);
    List<AbstractResource> altParents = findAlternativeParents(tree, locale);

    AbstractResource found = null;
    for (int i = 0; i < loaders.size(); i++) {
      ResourceFactory element = loaders.get(i);
      String path = partualPath + "." + element.getExt();
      InputStream m = loader.getResourceAsStream(path);
      if (m != null) {
        found = element.load(m);
        found.setPath(partualPath);
        found.setPrimaryParent(parent);
        found.setLocaleName(locale);
        for (int j = 0; j < altParents.size(); j++) {
          AbstractResource altParent = altParents.get(j);
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
    }

    cache.put(localizedPath, found);

    if (found == NOT_FOUND) {
      if (required) {
        throw new MissingResourceException(
          "Could not find any resource associated with " + tree.getPath(),
            null, null);
      } else {
        return null;
      }
    }

    // At this point, found cannot be equal to null or NOT_FOUND
    return found;
  }

  abstract String getExt();

  abstract AbstractResource load(InputStream m);
}
