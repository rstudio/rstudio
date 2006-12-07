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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.rebind.util.AbstractResource;
import com.google.gwt.i18n.rebind.util.ResourceFactory;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * Represents generic functionality needed for <code>Constants</code> and
 * <code>Messages</code> classes.
 */
abstract class AbstractLocalizableImplCreator extends
    AbstractGeneratorClassCreator {

  static String generateConstantOrMessageClass(TreeLogger logger,
      GeneratorContext context, Locale locale, JClassType targetClass)
      throws UnableToCompleteException {
    TypeOracle oracle = context.getTypeOracle();
    JClassType constantsClass;
    JClassType messagesClass;
    JClassType constantsWithLookupClass;
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

    AbstractResource resource;
    try {
      resource = ResourceFactory.getBundle(targetClass, locale);
    } catch (MissingResourceException e) {
      throw error(
          logger,
          "Localization failed; there must be at least one properties file accessible through the classpath in package '"
              + packageName
              + "' whose base name is '"
              + ResourceFactory.getResourceName(targetClass) + "'");
    } catch (IllegalArgumentException e) {
      // A bad key can generate an illegal argument exception.
      throw error(logger, e.getMessage());
    }

    // generated implementations for interface X will be named X_, X_en,
    // X_en_CA, etc.
    String realLocale = "_";
    if (resource.getLocale() != null) {
      realLocale += resource.getLocale();
    }
    // Use _ rather than "." in class name, cannot use $
    String resourceName = targetClass.getName().replace('.', '_');
    String className = resourceName + realLocale;
    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, className);
      factory.addImplementedInterface(targetClass.getQualifiedSourceName());
      SourceWriter writer = factory.createSourceWriter(context, pw);
      // Now that we have all the information set up, process the class
      if (constantsWithLookupClass.isAssignableFrom(targetClass)) {
        ConstantsWithLookupImplCreator c = new ConstantsWithLookupImplCreator(
            logger, writer, targetClass, resource, context.getTypeOracle());
        c.emitClass(logger);
      } else if (constantsClass.isAssignableFrom(targetClass)) {
        ConstantsImplCreator c = new ConstantsImplCreator(logger, writer,
            targetClass, resource, context.getTypeOracle());
        c.emitClass(logger);
      } else {
        MessagesImplCreator messages = new MessagesImplCreator(logger, writer,
            targetClass, resource, context.getTypeOracle());
        messages.emitClass(logger);
      }
      context.commit(logger, pw);
    }
    return packageName + "." + className;
  }

  /**
   * The Dictionary/value bindings used to determine message contents.
   */
  private AbstractResource messageBindings;

  /**
   * Constructor for <code>AbstractLocalizableImplCreator</code>.
   * 
   * @param writer writer
   * @param targetClass current target
   * @param messageBindings backing resource
   */
  public AbstractLocalizableImplCreator(SourceWriter writer,
      JClassType targetClass, AbstractResource messageBindings) {
    super(writer, targetClass);
    this.messageBindings = messageBindings;
  }

  /**
   * Gets the resource associated with this class.
   * 
   * @return the resource
   */
  public AbstractResource getResourceBundle() {
    return messageBindings;
  }

  protected String branchMessage() {
    return "Processing " + this.getTarget();
  }

  /**
   * Find the creator associated with the given method, and delegate the
   * creation of the method body to it.
   * 
   * @param logger
   * @param method method to be generated
   * @throws UnableToCompleteException
   */
  protected void delegateToCreator(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    AbstractMethodCreator methodCreator = getMethodCreator(logger, method);
    String key = getKey(logger, method);
    String value;
    try {
      value = messageBindings.getString(key);
    } catch (MissingResourceException e) {
      String s = "Could not find requested resource key '" + key + "'";
      TreeLogger child = logger.branch(TreeLogger.ERROR, s, null);
      Set keys = messageBindings.keySet();
      if (keys.size() < AbstractResource.REPORT_KEYS_THRESHOLD) {
        String keyString = "keys found: " + keys;
        throw error(child, keyString);
      } else {
        throw new UnableToCompleteException();
      }
    }
    String localeString;
    if (messageBindings.getLocale() == null
        || messageBindings.getLocale().toString().equals("")) {
      localeString = "default";
    } else {
      localeString = messageBindings.getLocale().toString();
    }
    String info = "When locale is '" + localeString + "', property '" + key
        + "' has the value '" + value + "'";
    TreeLogger branch = logger.branch(TreeLogger.TRACE, info, null);
    methodCreator.createMethodFor(branch, method, value);
  }

  /**
   * Returns a resource key given a method name.
   * 
   * @param logger
   * @param method
   * @return the key
   */
  protected String getKey(TreeLogger logger, JMethod method) {
    String[][] id = method.getMetaData(LocalizableGenerator.GWT_KEY);
    if (id.length > 0) {
      if (id[0].length == 0) {
        logger.log(TreeLogger.WARN, method
            + " had a mislabeled gwt.key, using method name as key", null);
      } else {
        String tag = id[0][0];
        return tag;
      }
    }
    return method.getName();
  }
}
