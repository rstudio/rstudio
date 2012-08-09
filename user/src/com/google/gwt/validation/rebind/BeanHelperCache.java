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
package com.google.gwt.validation.rebind;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.validation.client.impl.GwtSpecificValidator;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 *
 * <p> A cache containing BeanHelpers created in the current compilation run.
 * (Assumes that each compile happens in a different thread.)
 * Also acts as a factory for BeanHelpers. </p>
 */
public class BeanHelperCache { // public for testing

  // Each GWT module lives in its own thread in DevMode and should have a separate cache
  private static final ThreadLocal<BeanHelperCache> threadLocal = new ThreadLocal<BeanHelperCache>() {
    @Override
    protected BeanHelperCache initialValue() {
      return new BeanHelperCache();
    }
  };

  /**
   * Returns the cache for the current thread.
   * (Public for testing.)
   */
  public static BeanHelperCache getForThread() {
    return threadLocal.get();
  }

  private final Map<JClassType, BeanHelper> cache;
  private final Validator serverSideValidator;

  private BeanHelperCache() {
    cache = new HashMap<JClassType, BeanHelper>();
    serverSideValidator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  /**
   * Clears the cache.
   * (Public for testing.)
   */
  public void clear() {
    cache.clear();
  }

  /**
   * Creates a BeanHelper and writes an interface containing its instance. Also,
   * recursively creates any BeanHelpers on its constrained properties.
   * (Public for testing.)
   */
  public BeanHelper createHelper(Class<?> clazz, TreeLogger logger,
      GeneratorContext context) throws UnableToCompleteException {
    JClassType beanType = context.getTypeOracle().findType(clazz.getCanonicalName());
    return doCreateHelper(clazz, beanType, logger, context);
  }

  /**
   * Creates a BeanHelper and writes an interface containing its instance. Also,
   * recursively creates any BeanHelpers on its constrained properties.
   */
  BeanHelper createHelper(JClassType jType, TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {
    JClassType erasedType = jType.getErasedType();
    try {
      Class<?> clazz = Class.forName(erasedType.getQualifiedBinaryName());
      return doCreateHelper(clazz, erasedType, logger, context);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to create BeanHelper for "
          + erasedType, e);
      throw new UnableToCompleteException();
    }
  }

  List<BeanHelper> getAllBeans() {
    return Util.sortMostSpecificFirst(cache.values(), BeanHelper.TO_CLAZZ);
  }

  BeanHelper getBean(JClassType key) {
    return cache.get(key);
  }

  boolean isClassConstrained(Class<?> clazz) {
    return serverSideValidator.getConstraintsForClass(clazz).isBeanConstrained();
  }

  private BeanHelper doCreateHelper(Class<?> clazz,
      JClassType beanType, TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {
    BeanHelper helper = getBean(beanType);
    if (helper == null) {
      BeanDescriptor bean;
      try {
        bean = serverSideValidator.getConstraintsForClass(clazz);
      } catch (ValidationException e) {
        logger.log(TreeLogger.ERROR,
            "Unable to create a validator for " + clazz.getCanonicalName()
                + " because " + e.getMessage(), e);
        throw new UnableToCompleteException();
      }
      helper = new BeanHelper(beanType, clazz, bean);
      cache.put(helper.getJClass(), helper);

      writeInterface(context, logger, helper);

      // now recurse on all Cascaded elements
      for (PropertyDescriptor p : bean.getConstrainedProperties()) {
        if (p.isCascaded()) {
          doCreateHelperForProp(p, helper, logger, context);
        }
      }
    }
    return helper;
  }

  /**
   * Creates the appropriate BeanHelper for a property on a bean.
   */
  private void doCreateHelperForProp(PropertyDescriptor p, BeanHelper parent,
      TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {
    Class<?> elementClass = p.getElementClass();
    if (GwtSpecificValidatorCreator.isIterableOrMap(elementClass)) {
      if (parent.hasField(p)) {
        JClassType type = parent.getAssociationType(p, true);

        createHelper(type.getErasedType(), logger, context);
      }
      if (parent.hasGetter(p)) {
        JClassType type = parent.getAssociationType(p, false);

        createHelper(type.getErasedType(), logger, context);
      }
    } else {
      if (serverSideValidator.getConstraintsForClass(elementClass).isBeanConstrained()) {
        createHelper(elementClass, logger, context);
      }
    }
  }

  /**
   * Write an Empty Interface implementing
   * {@link com.google.gwt.validation.client.impl.GwtSpecificValidator} with
   * Generic parameter of the bean type.
   */
  private void writeInterface(GeneratorContext context, TreeLogger logger, BeanHelper bean) {
    PrintWriter pw = context.tryCreate(logger, bean.getPackage(),
        bean.getValidatorName());
    if (pw != null) {
      TreeLogger interfaceLogger = logger.branch(TreeLogger.TRACE,
          "Creating the interface for " + bean.getFullyQualifiedValidatorName());

      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          bean.getPackage(), bean.getValidatorName());
      factory.addImplementedInterface(GwtSpecificValidator.class.getCanonicalName()
          + " <" + bean.getTypeCanonicalName() + ">");
      factory.addImport(GWT.class.getCanonicalName());
      factory.makeInterface();
      SourceWriter sw = factory.createSourceWriter(context, pw);

      // static MyValidator INSTANCE = GWT.create(MyValidator.class);
      sw.print("static ");
      sw.print(bean.getValidatorName());
      sw.print(" INSTANCE = GWT.create(");
      sw.print(bean.getValidatorName());
      sw.println(".class);");

      sw.commit(interfaceLogger);
      pw.close();
    }
  }
}
