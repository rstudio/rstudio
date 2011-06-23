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
package com.google.gwt.resources.ext;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.collect.Maps;
import com.google.gwt.resources.client.ClientBundle.Source;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for building ResourceGenerators.
 */
public final class ResourceGeneratorUtil {

  private static class ClassLoaderLocator implements Locator {
    private final ClassLoader classLoader;

    public ClassLoaderLocator(ClassLoader classLoader) {
      this.classLoader = classLoader;
    }

    public URL locate(String resourceName) {
      return classLoader.getResource(resourceName);
    }
  }

  /**
   * A locator which will use files published via
   * {@link ResourceGeneratorUtil#addNamedFile(String, File)}.
   */
  private static class NamedFileLocator implements Locator {
    public static final NamedFileLocator INSTANCE = new NamedFileLocator();

    private NamedFileLocator() {
    }

    public URL locate(String resourceName) {
      File f = namedFiles.get(resourceName);
      if (f != null && f.isFile() && f.canRead()) {
        try {
          return f.toURI().toURL();
        } catch (MalformedURLException e) {
          throw new RuntimeException("Unable to make a URL for file "
              + f.getName());
        }
      }
      return null;
    }
  }

  /**
   * Wrapper interface around different strategies for loading resource data.
   */
  private interface Locator {
    URL locate(String resourceName);
  }

  private static class ResourceOracleLocator implements Locator {
    private final Map<String, Resource> resourceMap;

    public ResourceOracleLocator(ResourceOracle oracle) {
      resourceMap = oracle.getResourceMap();
    }

    @SuppressWarnings("deprecation")
    public URL locate(String resourceName) {
      Resource r = resourceMap.get(resourceName);
      return (r == null) ? null : r.getURL();
    }
  }

  private static Map<String, File> namedFiles = Maps.create();

  /**
   * These are type names from previous APIs or from APIs with similar
   * functionality that might be confusing.
   *
   * @see #checkForDeprecatedAnnotations
   */
  private static final String[] DEPRECATED_ANNOTATION_NAMES = {
      "com.google.gwt.libideas.resources.client.ImmutableResourceBundle$Resource",
      "com.google.gwt.user.client.ui.ImageBundle$Resource"};

  private static final List<Class<? extends Annotation>> DEPRECATED_ANNOTATION_CLASSES;

  static {
    List<Class<? extends Annotation>> classes = new ArrayList<Class<? extends Annotation>>(
        DEPRECATED_ANNOTATION_NAMES.length);

    for (String name : DEPRECATED_ANNOTATION_NAMES) {
      try {
        Class<?> maybeAnnotation = Class.forName(name, false,
            ResourceGeneratorUtil.class.getClassLoader());

        // Possibly throws ClassCastException
        Class<? extends Annotation> annotationClass = maybeAnnotation.asSubclass(Annotation.class);

        classes.add(annotationClass);

      } catch (ClassCastException e) {
        // If it's not an Annotation type, we don't care about it
      } catch (ClassNotFoundException e) {
        // This is OK; the annotation doesn't exist.
      }
    }

    if (classes.isEmpty()) {
      DEPRECATED_ANNOTATION_CLASSES = Collections.emptyList();
    } else {
      DEPRECATED_ANNOTATION_CLASSES = Collections.unmodifiableList(classes);
    }
  }

  /**
   * Publish or override resources named by {@link Source} annotations. This
   * method is intended to be called by Generators that create ClientBundle
   * instances and need to pass source data to the ClientBundle system that is
   * not accessible through the classpath.
   *
   * @param resourceName the path at which the contents of <code>file</code>
   *          should be made available
   * @param file the File whose contents are to be provided to the ClientBundle
   *          system
   */
  public static void addNamedFile(String resourceName, File file) {
    assert resourceName != null : "resourceName";
    assert file != null : "file";
    assert file.isFile() && file.canRead() : "file does not exist or cannot be read";

    namedFiles = Maps.put(namedFiles, resourceName, file);
  }

  /**
   * Returns the base filename of a resource. The behavior is similar to the unix
   * command <code>basename</code>.
   *
   * @param resource the URL of the resource
   * @return the final name segment of the resource
   */
  public static String baseName(URL resource) {
    String path = resource.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  /**
   * Find all resources referenced by a method in a bundle. The method's
   * {@link Source} annotation will be examined and the specified locations will
   * be expanded into URLs by which they may be accessed on the local system.
   * <p>
   * This method is sensitive to the <code>locale</code> deferred-binding
   * property and will attempt to use a best-match lookup by removing locale
   * components.
   * <p>
   * Loading through a ClassLoader with this method is much slower than the
   * other <code>findResources</code> methods which make use of the compiler's
   * ResourceOracle.
   *
   * @param logger a TreeLogger that will be used to report errors or warnings
   * @param context the ResourceContext in which the ResourceGenerator is
   *          operating
   * @param classLoader the ClassLoader to use when locating resources
   * @param method the method to examine for {@link Source} annotations
   * @param defaultSuffixes if the supplied method does not have any
   *          {@link Source} annotations, act as though a Source annotation was
   *          specified, using the name of the method and each of supplied
   *          extensions in the order in which they are specified
   * @return URLs for each {@link Source} annotation value defined on the
   *         method.
   * @throws UnableToCompleteException if ore or more of the sources could not
   *           be found. The error will be reported via the <code>logger</code>
   *           provided to this method
   */
  public static URL[] findResources(TreeLogger logger, ClassLoader classLoader,
      ResourceContext context, JMethod method, String[] defaultSuffixes)
      throws UnableToCompleteException {
    return findResources(logger, new Locator[] {new ClassLoaderLocator(
        classLoader)}, context, method, defaultSuffixes);
  }

  /**
   * Find all resources referenced by a method in a bundle. The method's
   * {@link Source} annotation will be examined and the specified locations will
   * be expanded into URLs by which they may be accessed on the local system.
   * <p>
   * This method is sensitive to the <code>locale</code> deferred-binding
   * property and will attempt to use a best-match lookup by removing locale
   * components.
   * <p>
   * The compiler's ResourceOracle will be used to resolve resource locations.
   * If the desired resource cannot be found in the ResourceOracle, this method
   * will fall back to using the current thread's context ClassLoader. If it is
   * necessary to alter the way in which resources are located, use the overload
   * that accepts a ClassLoader.
   * <p>
   * If the method's return type declares the {@link DefaultExtensions}
   * annotation, the value of this annotation will be used to find matching
   * resource names if the method lacks a {@link Source} annotation.
   *
   * @param logger a TreeLogger that will be used to report errors or warnings
   * @param context the ResourceContext in which the ResourceGenerator is
   *          operating
   * @param method the method to examine for {@link Source} annotations
   * @return URLs for each {@link Source} annotation value defined on the
   *         method.
   * @throws UnableToCompleteException if ore or more of the sources could not
   *           be found. The error will be reported via the <code>logger</code>
   *           provided to this method
   */
  public static URL[] findResources(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {
    JClassType returnType = method.getReturnType().isClassOrInterface();
    assert returnType != null;
    DefaultExtensions annotation = returnType.findAnnotationInTypeHierarchy(DefaultExtensions.class);
    String[] extensions;
    if (annotation != null) {
      extensions = annotation.value();
    } else {
      extensions = new String[0];
    }
    return findResources(logger, context, method, extensions);
  }

  /**
   * Find all resources referenced by a method in a bundle. The method's
   * {@link Source} annotation will be examined and the specified locations will
   * be expanded into URLs by which they may be accessed on the local system.
   * <p>
   * This method is sensitive to the <code>locale</code> deferred-binding
   * property and will attempt to use a best-match lookup by removing locale
   * components.
   * <p>
   * The compiler's ResourceOracle will be used to resolve resource locations.
   * If the desired resource cannot be found in the ResourceOracle, this method
   * will fall back to using the current thread's context ClassLoader. If it is
   * necessary to alter the way in which resources are located, use the overload
   * that accepts a ClassLoader.
   *
   * @param logger a TreeLogger that will be used to report errors or warnings
   * @param context the ResourceContext in which the ResourceGenerator is
   *          operating
   * @param method the method to examine for {@link Source} annotations
   * @param defaultSuffixes if the supplied method does not have any
   *          {@link Source} annotations, act as though a Source annotation was
   *          specified, using the name of the method and each of supplied
   *          extensions in the order in which they are specified
   * @return URLs for each {@link Source} annotation value defined on the
   *         method.
   * @throws UnableToCompleteException if ore or more of the sources could not
   *           be found. The error will be reported via the <code>logger</code>
   *           provided to this method
   */
  public static URL[] findResources(TreeLogger logger, ResourceContext context,
      JMethod method, String[] defaultSuffixes)
      throws UnableToCompleteException {
    Locator[] locators = getDefaultLocators(context.getGeneratorContext());
    URL[] toReturn = findResources(logger, locators, context, method,
        defaultSuffixes);
    return toReturn;
  }

  /**
   * Finds a method by following a dotted path interpreted as a series of no-arg
   * method invocations from an instance of a given root type.
   *
   * @param rootType the type from which the search begins
   * @param pathElements a sequence of no-arg method names
   * @param expectedReturnType the expected return type of the method to locate,
   *          or <code>null</code> if no constraint on the return type is
   *          necessary
   *
   * @return the requested JMethod
   * @throws NotFoundException if the requested method could not be found
   */
  public static JMethod getMethodByPath(JClassType rootType,
      List<String> pathElements, JType expectedReturnType)
      throws NotFoundException {
    if (pathElements.isEmpty()) {
      throw new NotFoundException("No path specified");
    }

    JMethod currentMethod = null;
    JType currentType = rootType;
    for (String pathElement : pathElements) {

      JClassType referenceType = currentType.isClassOrInterface();
      if (referenceType == null) {
        throw new NotFoundException("Cannot resolve member " + pathElement
            + " on type " + currentType.getQualifiedSourceName());
      }

      currentMethod = null;
      searchType : for (JClassType searchType : referenceType.getFlattenedSupertypeHierarchy()) {
        for (JMethod method : searchType.getOverloads(pathElement)) {
          if (method.getParameters().length == 0) {
            currentMethod = method;
            break searchType;
          }
        }
      }

      if (currentMethod == null) {
        throw new NotFoundException("Could not find no-arg method named "
            + pathElement + " in type " + currentType.getQualifiedSourceName());
      }
      currentType = currentMethod.getReturnType();
    }

    if (expectedReturnType != null) {
      JPrimitiveType expectedIsPrimitive = expectedReturnType.isPrimitive();
      JClassType expectedIsClassType = expectedReturnType.isClassOrInterface();
      boolean error = false;

      if (expectedIsPrimitive != null) {
        if (!expectedIsPrimitive.equals(currentMethod.getReturnType())) {
          error = true;
        }
      } else {
        JClassType returnIsClassType = currentMethod.getReturnType().isClassOrInterface();
        if (returnIsClassType == null) {
          error = true;
        } else if (!expectedIsClassType.isAssignableFrom(returnIsClassType)) {
          error = true;
        }
      }

      if (error) {
        throw new NotFoundException("Expecting return type "
            + expectedReturnType.getQualifiedSourceName() + " found "
            + currentMethod.getReturnType().getQualifiedSourceName());
      }
    }

    return currentMethod;
  }

  /**
   * Try to find a resource with the given resourceName.  It will use the default
   * search order to locate the resource as is used by {@link #findResources}.
   * 
   * @param logger
   * @param genContext
   * @param resourceContext
   * @param resourceName
   * @return a URL for the resource, if found
   */
  public static URL tryFindResource(TreeLogger logger,
      GeneratorContext genContext, ResourceContext resourceContext,
      String resourceName) {
    String locale = getLocale(logger, genContext);
    Locator[] locators = getDefaultLocators(genContext);
    for (Locator locator : locators) {
      URL toReturn = tryFindResource(locator, resourceContext, resourceName,
          locale);
      if (toReturn != null) {
        return toReturn;
      }
    }
    return null;
  }

  /**
   * Add the type dependency requirements for a method, to the context.
   * 
   * @param context
   * @param method
   */
  private static void addTypeRequirementsForMethod(ResourceContext context,
      JMethod method) {
    ClientBundleRequirements reqs = context.getRequirements();
    if (reqs != null) {
      reqs.addTypeHierarchy(method.getEnclosingType());
      reqs.addTypeHierarchy((JClassType) method.getReturnType());
    }
  }

  /**
   * We want to warn the user about any annotations from ImageBundle or the old
   * incubator code.
   */
  private static void checkForDeprecatedAnnotations(TreeLogger logger,
      JMethod method) {

    for (Class<? extends Annotation> annotationClass : DEPRECATED_ANNOTATION_CLASSES) {
      if (method.isAnnotationPresent(annotationClass)) {
        logger.log(TreeLogger.WARN, "Deprecated annotation used; expecting "
            + Source.class.getCanonicalName() + " but found "
            + annotationClass.getName() + " instead.  It is likely "
            + "that undesired operation will occur.");
      }
    }
  }

  /**
   * Main implementation of findResources.
   */
  private static URL[] findResources(TreeLogger logger, Locator[] locators,
      ResourceContext context, JMethod method, String[] defaultSuffixes)
      throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG, "Finding resources");

    String locale = getLocale(logger, context.getGeneratorContext());

    checkForDeprecatedAnnotations(logger, method);

    boolean error = false;
    Source resourceAnnotation = method.getAnnotation(Source.class);
    URL[] toReturn;

    if (resourceAnnotation == null) {
      if (defaultSuffixes != null) {
        for (String extension : defaultSuffixes) {
          if (logger.isLoggable(TreeLogger.SPAM)) {
            logger.log(TreeLogger.SPAM, "Trying default extension " + extension);
          }
          for (Locator locator : locators) {
            URL resourceUrl = tryFindResource(locator, context,
                getPathRelativeToPackage(method.getEnclosingType().getPackage(),
                    method.getName() + extension), locale);

            // Take the first match
            if (resourceUrl != null) {
              addTypeRequirementsForMethod(context, method);
              return new URL[] {resourceUrl};
            }
          }
        }
      }

      logger.log(TreeLogger.ERROR, "No " + Source.class.getName()
          + " annotation and no resources found with default extensions");
      toReturn = null;
      error = true;

    } else {
      // The user has put an @Source annotation on the accessor method
      String[] resources = resourceAnnotation.value();

      toReturn = new URL[resources.length];

      int tagIndex = 0;
      for (String resource : resources) {
        // Try to find the resource relative to the package.
        URL resourceURL = null;

        for (Locator locator : locators) {
          resourceURL = tryFindResource(locator, context,
              getPathRelativeToPackage(method.getEnclosingType().getPackage(),
                  resource), locale);

          /*
           * If we didn't find the resource relative to the package, assume it
           * is absolute.
           */
          if (resourceURL == null) {
            resourceURL = tryFindResource(locator, context, resource, locale);
          }

          // If we have found a resource, take the first match
          if (resourceURL != null) {
            break;
          }
        }

        if (resourceURL == null) {
          error = true;
          logger.log(TreeLogger.ERROR, "Resource " + resource
              + " not found. Is the name specified as Class.getResource()"
              + " would expect?");
        }

        toReturn[tagIndex++] = resourceURL;
      }
    }

    if (error) {
      throw new UnableToCompleteException();
    }

    addTypeRequirementsForMethod(context, method);
    return toReturn;
  }

  /**
   * Get default list of resource Locators, in the default order.
   * 
   * @param genContext
   * @return an ordered array of Locator[]
   */
  private static Locator[] getDefaultLocators(GeneratorContext genContext) {
    Locator[] locators = {
      NamedFileLocator.INSTANCE,
      new ResourceOracleLocator(genContext.getResourcesOracle()),
      new ClassLoaderLocator(Thread.currentThread().getContextClassLoader())};

    return locators;
  }

  /**
   * Get the current locale string.
   * 
   * @param logger
   * @param genContext
   * @return the current locale
   */
  private static String getLocale(TreeLogger logger, GeneratorContext genContext) {
    String locale;
    try {
      PropertyOracle oracle = genContext.getPropertyOracle();
      SelectionProperty prop = oracle.getSelectionProperty(logger, "locale");
      locale = prop.getCurrentValue();
    } catch (BadPropertyValueException e) {
      locale = null;
    }
    return locale;
  }
 
  /**
   * Converts a package relative path into an absolute path.
   *
   * @param pkg the package
   * @param path a path relative to the package
   * @return an absolute path
   */
  private static String getPathRelativeToPackage(JPackage pkg, String path) {
    return pkg.getName().replace('.', '/') + '/' + path;
  }

  /**
   * This performs the locale lookup function for a given resource name.
   *
   * @param locator the Locator to use to load the resources
   * @param resourceName the string name of the desired resource
   * @param locale the locale of the current rebind permutation
   * @return a URL by which the resource can be loaded, <code>null</code> if one
   *         cannot be found
   */
  private static URL tryFindResource(Locator locator, String resourceName,
      String locale) {
    URL toReturn = null;

    // Look for locale-specific variants of individual resources
    if (locale != null) {
      // Convert language_country_variant to independent pieces
      String[] localeSegments = locale.split("_");
      int lastDot = resourceName.lastIndexOf(".");
      String prefix = lastDot == -1 ? resourceName : resourceName.substring(0,
          lastDot);
      String extension = lastDot == -1 ? "" : resourceName.substring(lastDot);

      for (int i = localeSegments.length - 1; i >= -1; i--) {
        String localeInsert = "";
        for (int j = 0; j <= i; j++) {
          localeInsert += "_" + localeSegments[j];
        }

        toReturn = locator.locate(prefix + localeInsert + extension);
        if (toReturn != null) {
          break;
        }
      }
    } else {
      toReturn = locator.locate(resourceName);
    }

    return toReturn;
  }

  /**
   * Performs the locale lookup function for a given resource name.  Will also
   * add the located resource to the requirements object for the context.
   *
   * @param locator the Locator to use to load the resources
   * @param context the ResourceContext
   * @param resourceName the string name of the desired resource
   * @param locale the locale of the current rebind permutation
   * @return a URL by which the resource can be loaded, <code>null</code> if one
   *         cannot be found
   */
  private static URL tryFindResource(Locator locator, ResourceContext context,
      String resourceName, String locale) {

    URL toReturn = tryFindResource(locator, resourceName, locale);
    if (context != null) {
      ClientBundleRequirements reqs = context.getRequirements();
      if (reqs != null) {
        reqs.addResolvedResource(resourceName, toReturn);
      }
    }

    return toReturn;
  }

  /**
   * Utility class.
   */
  private ResourceGeneratorUtil() {
  }
}
