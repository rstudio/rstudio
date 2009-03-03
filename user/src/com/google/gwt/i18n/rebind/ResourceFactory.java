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
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.rebind.AnnotationsResource.AnnotationsError;
import com.google.gwt.i18n.shared.GwtLocale;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  public static final char LOCALE_SEPARATOR = '_';

  private static Map<String, ResourceList> cache = new HashMap<String, ResourceList>();

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

  public static ResourceList getBundle(Class<?> clazz, GwtLocale locale,
      boolean isConstants) {
    assert locale != null;
    return getBundle(TreeLogger.NULL, clazz, locale, isConstants);
  }

  public static ResourceList getBundle(String path, GwtLocale locale,
      boolean isConstants) {
    assert locale != null;
    return getBundle(TreeLogger.NULL, path, locale, isConstants);
  }

  /**
   * Gets the resource associated with the given interface.
   * 
   * @param javaInterface interface
   * @param locale locale name
   * @return the resource
   */
  public static ResourceList getBundle(TreeLogger logger, Class<?> javaInterface,
      GwtLocale locale, boolean isConstants) {
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
  public static ResourceList getBundle(TreeLogger logger,
      JClassType javaInterface, GwtLocale locale, boolean isConstants) {
    return getBundleAux(logger, new JClassTypePathTree(javaInterface),
        javaInterface, locale, true, isConstants);
  }

  /**
   * Gets the resource associated with the given path.
   * 
   * @param path the path
   * @param locale locale name
   * @return the resource
   */
  public static ResourceList getBundle(TreeLogger logger, String path,
      GwtLocale locale, boolean isConstants) {
    return getBundleAux(logger, new SimplePathTree(path), null, locale, true,
        isConstants);
  }

  public static String getResourceName(JClassType targetClass) {
    String name = targetClass.getName();
    if (targetClass.isMemberType()) {
      name = name.replace('.', '$');
    }
    return name;
  }

  private static void addAlternativeParents(TreeLogger logger,
      ResourceFactory.AbstractPathTree tree, JClassType clazz, GwtLocale locale,
      boolean useAlternativeParents, boolean isConstants,
      ResourceList resources, Set<String> seenPaths) {
    if (tree != null) {
      for (int i = 0; i < tree.numChildren(); i++) {
        ResourceFactory.AbstractPathTree child = tree.getChild(i);
        addResources(logger, child, child.getJClassType(clazz),
            locale, useAlternativeParents, isConstants, resources, seenPaths);
      }
    }
  }

  private static void addPrimaryParent(TreeLogger logger,
      ResourceFactory.AbstractPathTree tree, JClassType clazz, GwtLocale locale,
      boolean isConstants, ResourceList resources, Set<String> seenPaths) {
    for (GwtLocale search : locale.getCompleteSearchList()) {
      // This relies on addResources exiting if it has seen a given path
      // (derived from the locale) to avoid infinite loops, since the search
      // list includes this locale as well as aliases which will refer back
      // to this locale.
      addResources(logger, tree, clazz, search, false, isConstants, resources,
          seenPaths);
    }
  }

  private static void addResources(TreeLogger logger,
      ResourceFactory.AbstractPathTree tree, JClassType clazz, GwtLocale locale,
      boolean useAlternateParents, boolean isConstants,
      ResourceList resources, Set<String> seenPaths) {
    String targetPath = tree.getPath();
    String localizedPath = targetPath;
    if (!locale.isDefault()) {
      localizedPath = targetPath + LOCALE_SEPARATOR + locale.getAsString();
    }
    if (seenPaths.contains(localizedPath)) {
      return;
    }
    seenPaths.add(localizedPath);
    ClassLoader loader = AbstractResource.class.getClassLoader();
    Map<String, JClassType> matchingClasses = null;
    if (clazz != null) {
      try {
        matchingClasses = LocalizableLinkageCreator.findDerivedClasses(logger,
            clazz);
        /* 
         * In this case, we specifically want to be able to look at the interface
         * instead of just implementations.
         */
        String defLocaleValue = DefaultLocale.DEFAULT_LOCALE;
        DefaultLocale defLocale = clazz.getAnnotation(DefaultLocale.class);
        if (defLocale != null) {
          defLocaleValue = defLocale.value();
          if (!GwtLocale.DEFAULT_LOCALE.equals(defLocaleValue)) {
            matchingClasses.put(defLocaleValue, clazz);
          }
        }
        matchingClasses.put(GwtLocale.DEFAULT_LOCALE, clazz);
      } catch (UnableToCompleteException e) {
        // ignore error, fall through
      }
    }
    if (matchingClasses == null) {
      // empty map
      matchingClasses = new HashMap<String, JClassType>();
    }

    // Check for file-based resources.
    String partialPath = localizedPath.replace('.', '/');
    for (int i = 0; i < loaders.size(); i++) {
      ResourceFactory element = loaders.get(i);
      String ext = "." + element.getExt();
      String path = partialPath + ext;
      InputStream m = loader.getResourceAsStream(path);
      if (m == null && partialPath.contains("$")) {
        // Also look for A_B for inner classes, as $ in path names
        // can cause issues for some build tools.
        path = partialPath.replace('$', '_') + ext;
        m = loader.getResourceAsStream(path);
      }
      if (m != null) {
        AbstractResource found = element.load(m, locale);
        found.setPath(path);
        resources.add(found);
      }
    }

    // Check for annotations
    JClassType currentClass = matchingClasses.get(locale.toString());
    if (currentClass != null) {
      AnnotationsResource resource;
      try {
        resource = new AnnotationsResource(logger, currentClass, locale,
            isConstants);
        if (resource.notEmpty()) {
          resource.setPath(currentClass.getQualifiedSourceName());
          resources.add(resource);
        }
      } catch (AnnotationsError e) {
        logger.log(TreeLogger.ERROR, e.getMessage(), e);
      }
    }
    
    // Add our parent, if any
    addPrimaryParent(logger, tree, clazz, locale, isConstants, resources,
        seenPaths);

    // Add our alternate parents
    if (useAlternateParents) {
      addAlternativeParents(logger, tree, clazz, locale, useAlternateParents,
          isConstants, resources, seenPaths);
    }
  }

  private static ResourceList getBundleAux(TreeLogger logger,
      ResourceFactory.AbstractPathTree tree, JClassType clazz, GwtLocale locale,
      boolean required, boolean isConstants) {
    String cacheKey = tree.getPath() + "_" + locale.getAsString();
    if (cache.containsKey(cacheKey)) {
      return cache.get(cacheKey);
    }
    Set<String> seenPaths = new HashSet<String>();
    final ResourceList resources = new ResourceList();
    addResources(logger, tree, clazz, locale, true, isConstants, resources,
        seenPaths);
    String className = tree.getPath();
    if (clazz != null) {
      className = clazz.getQualifiedSourceName();
    }
    TreeLogger branch = logger.branch(TreeLogger.SPAM, "Resource search order for "
        + className + ", locale " + locale);
    for (AbstractResource resource : resources) {
      branch.log(TreeLogger.SPAM, resource.toString());
    }
    cache.put(cacheKey, resources);
    return resources;
  }

  abstract String getExt();

  abstract AbstractResource load(InputStream m, GwtLocale locale);
}
