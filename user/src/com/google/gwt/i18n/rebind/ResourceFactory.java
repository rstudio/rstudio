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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.i18n.rebind.AnnotationsResource.AnnotationsError;

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
    
    JClassType getJClassType(JClassType clazz) {
      return clazz;
    }

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

    @Override
    JClassType getJClassType(JClassType clazz) {
      return javaInterface;
    }
    
    /**
     * Path is equivalent to javaInterface.getQualifiedName() except for inner
     * classes.
     * 
     * @see com.google.gwt.i18n.rebind.ResourceFactory.AbstractPathTree#getPath()
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
    public Object handleGetObject(String key) {
      throw new IllegalStateException("Not found resource");
    }

    @Override
    void addToKeySet(Set<String> s) {
      throw new IllegalStateException("Not found resource");
    }
  };

  private static Map<String, AbstractResource> cache = new HashMap<String, AbstractResource>();

  private static List<ResourceFactory> loaders = new ArrayList<ResourceFactory>();
  static {
    loaders.add(new LocalizedPropertiesResource.Factory());
  }

  /**
   * Clears the resource cache.
   */
  public static void clearCache() {
    cache.clear();
  }

  public static AbstractResource getAnnotations(TreeLogger logger, JClassType targetClass,
      String locale, boolean isConstants) throws UnableToCompleteException {
    Map<String, JClassType> matchingClasses
        = LocalizableLinkageCreator.findDerivedClasses(logger, targetClass);
    matchingClasses.put(ResourceFactory.DEFAULT_TOKEN, targetClass);
    String localeSuffix = locale;
    JClassType currentClass = null;
    AnnotationsResource previous = null;
    AbstractResource result = null;
    while (true) {
      currentClass = matchingClasses.get(localeSuffix);
      if (currentClass != null) {
        AnnotationsResource resource;
        try {
          resource = new AnnotationsResource(logger, currentClass, isConstants);
        } catch (AnnotationsError e) {
          logger.log(TreeLogger.ERROR, e.getMessage(), e);
          throw new UnableToCompleteException();
        }
        if (resource.notEmpty()) {
          if (result == null) {
            result = resource;
          }
          if (previous != null) {
            previous.setParentResource(resource);
          }
          previous = resource;
        }
      }
      if (localeSuffix.equals(ResourceFactory.DEFAULT_TOKEN)) {
        return result;
      }
      
      localeSuffix = ResourceFactory.getParentLocaleName(localeSuffix);
    }
  }

  public static AbstractResource getBundle(Class<?> clazz, String locale, boolean isConstants) {
    return getBundle(TreeLogger.NULL, clazz, locale, isConstants);
  }

  public static AbstractResource getBundle(String path, String locale, boolean isConstants) {
    return getBundle(TreeLogger.NULL, path, locale, isConstants);
  }

  /**
   * Gets the resource associated with the given interface.
   * 
   * @param javaInterface interface
   * @param locale locale name
   * @return the resource
   */
  public static AbstractResource getBundle(TreeLogger logger, Class<?> javaInterface,
      String locale, boolean isConstants) {
    if (javaInterface.isInterface() == false) {
      throw new IllegalArgumentException(javaInterface
          + " should be an interface.");
    }
    ClassPathTree path = new ClassPathTree(javaInterface);
    return getBundleAux(logger, path, null, locale, true, isConstants);
  }

  /**
   * Gets the resource associated with the given interface.
   * 
   * @param javaInterface interface
   * @param locale locale name
   * @return the resource
   */
  public static AbstractResource getBundle(TreeLogger logger, JClassType javaInterface,
      String locale, boolean isConstants) {
    return getBundleAux(logger, new JClassTypePathTree(javaInterface), javaInterface, locale,
        true, isConstants);
  }

  /**
   * Gets the resource associated with the given path.
   * 
   * @param path the path
   * @param locale locale name
   * @return the resource
   */
  public static AbstractResource getBundle(TreeLogger logger, String path, String locale,
      boolean isConstants) {
    return getBundleAux(logger, new SimplePathTree(path), null, locale, true, isConstants);
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

  private static List<AbstractResource> findAlternativeParents(TreeLogger logger,
      ResourceFactory.AbstractPathTree tree, JClassType clazz, String locale,
      boolean isConstants) {
    List<AbstractResource> altParents = null;
    if (tree != null) {
      altParents = new ArrayList<AbstractResource>();
      for (int i = 0; i < tree.numChildren(); i++) {
        ResourceFactory.AbstractPathTree child = tree.getChild(i);
        AbstractResource altParent = getBundleAux(logger, child, child.getJClassType(clazz),
            locale, false, isConstants);
        if (altParent != null) {
          altParents.add(altParent);
        }
      }
    }
    return altParents;
  }

  private static AbstractResource findPrimaryParent(TreeLogger logger,
      ResourceFactory.AbstractPathTree tree, JClassType clazz, String locale,
      boolean isConstants) {

    // If we are not in the default case, calculate parent
    if (!DEFAULT_TOKEN.equals(locale)) {
      return getBundleAux(logger, tree, clazz, getParentLocaleName(locale), false, isConstants);
    }
    return null;
  }

  private static AbstractResource getBundleAux(TreeLogger logger,
      ResourceFactory.AbstractPathTree tree, JClassType clazz, String locale, boolean required,
      boolean isConstants) {
    String targetPath = tree.getPath();
    ClassLoader loader = AbstractResource.class.getClassLoader();
    Map<String, JClassType> matchingClasses = null;
    if (clazz != null) {
      try {
        matchingClasses = LocalizableLinkageCreator.findDerivedClasses(logger, clazz);
        /* 
         * In this case, we specifically want to be able to look at the interface
         * instead of just implementations.
         */
        matchingClasses.put(ResourceFactory.DEFAULT_TOKEN, clazz);
      } catch (UnableToCompleteException e) {
        // ignore error, fall through
      }
    }
    if (matchingClasses == null) {
      // empty map
      matchingClasses = new HashMap<String, JClassType>();
    }

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
    String partialPath = localizedPath.replace('.', '/');
    AbstractResource parent = findPrimaryParent(logger, tree, clazz, locale, isConstants);
    List<AbstractResource> altParents = findAlternativeParents(logger, tree, clazz, locale, isConstants);

    AbstractResource found = null;
    JClassType currentClass = matchingClasses.get(locale);
    if (currentClass != null) {
      AnnotationsResource resource;
      try {
        resource = new AnnotationsResource(logger, currentClass, isConstants);
        if (resource.notEmpty()) {
          found = resource;
          found.setPath(currentClass.getQualifiedSourceName());
        }
      } catch (AnnotationsError e) {
        logger.log(TreeLogger.ERROR, e.getMessage(), e);
      }
    }
    for (int i = 0; found == null && i < loaders.size(); i++) {
      ResourceFactory element = loaders.get(i);
      String path = partialPath + "." + element.getExt();
      InputStream m = loader.getResourceAsStream(path);
      if (m != null) {
        found = element.load(m);
        found.setPath(path);
      }
    }
    if (found == null) {
      if (parent != null) {
        found = parent;
      } else {
        found = NOT_FOUND;
      }
    } else {
      found.setPrimaryParent(parent);
      found.setLocaleName(locale);
      for (int j = 0; j < altParents.size(); j++) {
        AbstractResource altParent = altParents.get(j);
        found.addAlternativeParent(altParent);
      }
      found.checkKeys();
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
