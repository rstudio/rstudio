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

import static com.google.gwt.i18n.rebind.AnnotationUtil.getClassAnnotation;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.LocalizableResource.Generate;
import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.rebind.AnnotationsResource.AnnotationsError;
import com.google.gwt.i18n.rebind.format.MessageCatalogFormat;
import com.google.gwt.i18n.rebind.keygen.KeyGenerator;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.MissingResourceException;

/**
 * Represents generic functionality needed for <code>Constants</code> and
 * <code>Messages</code> classes.
 */
abstract class AbstractLocalizableImplCreator extends
    AbstractGeneratorClassCreator {

  static String generateConstantOrMessageClass(TreeLogger logger,
      GeneratorContext context, GwtLocale locale, JClassType targetClass)
      throws UnableToCompleteException {
    TypeOracle oracle = context.getTypeOracle();
    JClassType constantsClass;
    JClassType messagesClass;
    JClassType constantsWithLookupClass;
    boolean seenError = false;
    try {
      constantsClass = oracle.getType(LocalizableGenerator.CONSTANTS_NAME);
      constantsWithLookupClass = oracle.getType(LocalizableGenerator.CONSTANTS_WITH_LOOKUP_NAME);
      messagesClass = oracle.getType(LocalizableGenerator.MESSAGES_NAME);
    } catch (NotFoundException e) {
      // Should never happen in practice.
      throw error(logger, e);
    }

    String name = targetClass.getName();
    String packageName = targetClass.getPackage().getName();

    // Make sure the interface being rebound extends either Constants or
    // Messages.
    boolean assignableToConstants = constantsClass.isAssignableFrom(targetClass);
    boolean assignableToMessages = messagesClass.isAssignableFrom(targetClass);
    if (!assignableToConstants && !assignableToMessages) {
      // Let the implementation generator handle this interface.
      return null;
    }

    // Make sure that they don't try to extend both Messages and Constants.
    if (assignableToConstants && assignableToMessages) {
      throw error(logger, name + " cannot extend both Constants and Messages");
    }

    // Make sure that the type being rebound is in fact an interface.
    if (targetClass.isInterface() == null) {
      throw error(logger, name + " must be an interface");
    }

    ResourceList resourceList = null;
    try {
      resourceList = ResourceFactory.getBundle(logger, targetClass, locale,
          assignableToConstants, context.getResourcesOracle().getResourceMap());
    } catch (MissingResourceException e) {
      throw error(logger,
          "Localization failed; there must be at least one resource accessible through"
              + " the classpath in package '" + packageName
              + "' whose base name is '"
              + ResourceFactory.getResourceName(targetClass) + "'");
    } catch (IllegalArgumentException e) {
      // A bad key can generate an illegal argument exception.
      throw error(logger, e.getMessage());
    }

    // generated implementations for interface X will be named X_, X_en,
    // X_en_CA, etc.
    GwtLocale generatedLocale = resourceList.findLeastDerivedLocale(logger,
        locale);
    String localeSuffix = String.valueOf(ResourceFactory.LOCALE_SEPARATOR);
    localeSuffix += generatedLocale.getAsString();
    // Use _ rather than "." in class name, cannot use $
    String resourceName = targetClass.getName().replace('.', '_');
    String className = resourceName + localeSuffix;
    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, className);
      factory.addImplementedInterface(targetClass.getQualifiedSourceName());
      SourceWriter writer = factory.createSourceWriter(context, pw);
      // Now that we have all the information set up, process the class
      if (constantsWithLookupClass.isAssignableFrom(targetClass)) {
        ConstantsWithLookupImplCreator c = new ConstantsWithLookupImplCreator(
            logger, writer, targetClass, resourceList, context.getTypeOracle());
        c.emitClass(logger, generatedLocale);
      } else if (constantsClass.isAssignableFrom(targetClass)) {
        ConstantsImplCreator c = new ConstantsImplCreator(logger, writer,
            targetClass, resourceList, context.getTypeOracle());
        c.emitClass(logger, generatedLocale);
      } else {
        MessagesImplCreator messages = new MessagesImplCreator(logger, writer,
            targetClass, resourceList, context.getTypeOracle());
        messages.emitClass(logger, generatedLocale);
      }
      context.commit(logger, pw);
    }
    // Generate a translatable output file if requested.
    Generate generate = getClassAnnotation(targetClass, Generate.class);
    if (generate != null) {
      String path = generate.fileName();
      if (Generate.DEFAULT.equals(path)) {
        path = targetClass.getPackage().getName() + "."
            + targetClass.getName().replace('.', '_');
      } else if (path.endsWith(File.pathSeparator)) {
        path = path + targetClass.getName().replace('.', '_');
      }
      String[] genLocales = generate.locales();
      boolean found = false;
      if (genLocales.length != 0) {
        // verify the current locale is in the list
        for (String genLocale : genLocales) {
          if (GwtLocale.DEFAULT_LOCALE.equals(genLocale)) {
            // Locale "default" gets special handling because of property
            // fallbacks; "default" might be mapped to any real locale.
            try {
              SelectionProperty localeProp = 
                  context.getPropertyOracle().getSelectionProperty(logger, "locale");
              String defaultLocale = localeProp.getFallbackValue();
              if (defaultLocale.length() > 0) {
                genLocale = defaultLocale;
              }
            } catch (BadPropertyValueException e) {
              throw error(logger, "Could not get 'locale' property");
            }
          }
          if (genLocale.equals(locale.toString())) {
            found = true;
            break;
          }
        }
      } else {
        // Since they want all locales, this is guaranteed to be one of them.
        found = true;
      }
      if (found) {
        for (String genClassName : generate.format()) {
          MessageCatalogFormat msgWriter = null;
          try {
            Class<? extends MessageCatalogFormat> msgFormatClass = Class.forName(
                genClassName, false,
                MessageCatalogFormat.class.getClassLoader()).asSubclass(
                MessageCatalogFormat.class);
            msgWriter = msgFormatClass.newInstance();
          } catch (InstantiationException e) {
            logger.log(TreeLogger.ERROR, "Error instantiating @Generate class "
                + genClassName, e);
            seenError = true;
            continue;
          } catch (IllegalAccessException e) {
            logger.log(TreeLogger.ERROR, "@Generate class " + genClassName
                + " illegal access", e);
            seenError = true;
            continue;
          } catch (ClassNotFoundException e) {
            logger.log(TreeLogger.ERROR, "@Generate class " + genClassName
                + " not found");
            seenError = true;
            continue;
          }
          // Make generator-specific changes to a temporary copy of the path.
          String genPath = path;
          if (genLocales.length != 1) {
            // If the user explicitly specified only one locale, do not add the
            // locale.
            genPath += '_' + locale.toString();
          }
          genPath += msgWriter.getExtension();
          OutputStream outStr = context.tryCreateResource(logger, genPath);
          if (outStr != null) {
            TreeLogger branch = logger.branch(TreeLogger.INFO, "Generating "
                + genPath + " from " + className + " for locale " + locale,
                null);
            PrintWriter out = null;
            try {
              out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                  outStr, "UTF-8")), false);
            } catch (UnsupportedEncodingException e) {
              throw error(logger, e.getMessage());
            }
            try {
              // TODO(jat): change writer interface to use GwtLocale
              msgWriter.write(branch, locale.toString(), resourceList, out,
                  targetClass);
              out.flush();
              context.commitResource(logger, outStr).setPrivate(true);
            } catch (UnableToCompleteException e) {
              // msgWriter should have already logged an error message.
              // Keep going for now so we can find other errors.
              seenError = true;
            }
          }
        }
      }
    }
    if (seenError) {
      // If one of our generators had a fatal error, don't complete normally.
      throw new UnableToCompleteException();
    }
    return packageName + "." + className;
  }

  /**
   * Generator to use to create keys for messages.
   */
  private KeyGenerator keyGenerator;

  /**
   * The Dictionary/value bindings used to determine message contents.
   */
  private ResourceList resourceList;

  /**
   * True if the class being generated uses Constants-style annotations/quoting.
   */
  private boolean isConstants;

  /**
   * Constructor for <code>AbstractLocalizableImplCreator</code>.
   * 
   * @param writer writer
   * @param targetClass current target
   * @param resourceList backing resource
   */
  public AbstractLocalizableImplCreator(TreeLogger logger, SourceWriter writer,
      JClassType targetClass, ResourceList resourceList, boolean isConstants) {
    super(writer, targetClass);
    this.resourceList = resourceList;
    this.isConstants = isConstants;
    try {
      keyGenerator = AnnotationsResource.getKeyGenerator(targetClass);
    } catch (AnnotationsError e) {
      logger.log(TreeLogger.WARN, "Error getting key generator for "
          + targetClass.getQualifiedSourceName(), e);
    }
  }

  /**
   * Gets the resource associated with this class.
   * 
   * @return the resource
   */
  public ResourceList getResourceBundle() {
    return resourceList;
  }

  @Override
  protected String branchMessage() {
    return "Processing " + this.getTarget();
  }

  /**
   * Find the creator associated with the given method, and delegate the
   * creation of the method body to it.
   * 
   * @param logger TreeLogger instance for logging
   * @param method method to be generated
   * @param locale locale to generate
   * @throws UnableToCompleteException
   */
  protected void delegateToCreator(TreeLogger logger, JMethod method,
      GwtLocale locale) throws UnableToCompleteException {
    AbstractMethodCreator methodCreator = getMethodCreator(logger, method);
    String key = getKey(logger, method);
    if (key == null) {
      logger.log(TreeLogger.ERROR, "Unable to get or compute key for method "
          + method.getName(), null);
      throw new UnableToCompleteException();
    }
    methodCreator.createMethodFor(logger, method, key, resourceList, locale);
  }

  /**
   * Returns a resource key given a method name.
   * 
   * @param logger TreeLogger instance for logging
   * @param method method to get key for
   * @return the key to use for resource lookups or null if unable to get or
   *         compute the key
   */
  protected String getKey(TreeLogger logger, JMethod method) {
    Key key = method.getAnnotation(Key.class);
    if (key != null) {
      return key.value();
    }
    return AnnotationsResource.getKey(logger, keyGenerator, method, isConstants);
  }
}
