/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.validation.client.impl.GwtSpecificValidator;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * A simple struct for the various values associated with a Bean that can be
 * validated.
 */
public final class BeanHelper {

  public static final Function<BeanHelper, Class<?>> TO_CLAZZ = 
      new Function<BeanHelper, Class<?>>() {
    public Class<?> apply(BeanHelper helper) {
      return helper.getClazz();
    }
  };

  private static final Validator serverSideValidator = 
      Validation.buildDefaultValidatorFactory().getValidator();

  // stash the map in a ThreadLocal, since each GWT module lives in its own
  // thread in DevMode
  private static final ThreadLocal<Map<JClassType, BeanHelper>> threadLocalHelperMap =
        new ThreadLocal<Map<JClassType, BeanHelper>>() {
    @Override
    protected synchronized Map<JClassType, BeanHelper> initialValue() {
      return new HashMap<JClassType, BeanHelper>();
    }
  };

  /**
   * Visible for testing.
   */
  public static void clearBeanHelpersForTests() {
    threadLocalHelperMap.get().clear();
  }

  public static BeanHelper createBeanHelper(Class<?> clazz, TreeLogger logger,
      GeneratorContext context) throws UnableToCompleteException {
    JClassType beanType = context.getTypeOracle().findType(
        clazz.getCanonicalName());
    return createBeanHelper(clazz, beanType, logger, context);
  }

  public static Map<JClassType, BeanHelper> getBeanHelpers() {
    return Collections.unmodifiableMap(threadLocalHelperMap.get());
  }

  protected static BeanHelper createBeanHelper(JClassType jType,
      TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {
    JClassType erasedType = jType.getErasedType();
    try {
      Class<?> clazz = Class.forName(erasedType.getQualifiedBinaryName());
      return createBeanHelper(clazz, erasedType, logger, context);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to create BeanHelper for "
          + erasedType, e);
      throw new UnableToCompleteException();
    }
  }

  protected static boolean isClassConstrained(Class<?> clazz) {
    return serverSideValidator.getConstraintsForClass(clazz).isBeanConstrained();
  }

  static BeanHelper getBeanHelper(JClassType beanType) {
    return getBeanHelpers().get(beanType.getErasedType());
  }

  /**
   * Write an Empty Interface implementing {@link GwtSpecificValidator} with
   * Generic parameter of the bean type.
   */
  static void writeInterface(GeneratorContext context, TreeLogger logger,
      BeanHelper bean) {
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

  private static synchronized void addBeanHelper(BeanHelper helper) {
    threadLocalHelperMap.get().put(helper.getJClass(), helper);
  }

  private static BeanHelper createBeanHelper(Class<?> clazz,
      JClassType beanType, TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {
    BeanHelper helper = getBeanHelper(beanType);
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
      addBeanHelper(helper);
      writeInterface(context, logger, helper);

      // now recurse on all Cascaded elements
      for (PropertyDescriptor p : bean.getConstrainedProperties()) {
        if (p.isCascaded()) {
          createBeanHelper(p, helper, logger, context);
        }
      }
    }
    return helper;
  }

  private static void createBeanHelper(PropertyDescriptor p, BeanHelper parent,
      TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {
    Class<?> elementClass = p.getElementClass();
    if (GwtSpecificValidatorCreator.isIterableOrMap(elementClass)) {
      if (parent.hasField(p)) {
        JClassType type = parent.getAssociationType(p, true);

        createBeanHelper(type.getErasedType(), logger, context);
      }
      if (parent.hasGetter(p)) {
        JClassType type = parent.getAssociationType(p, false);

        createBeanHelper(type.getErasedType(), logger, context);
      }
    } else {
      if (serverSideValidator.getConstraintsForClass(elementClass).isBeanConstrained()) {
        createBeanHelper(elementClass, logger, context);
      }
    }
  }

  private final BeanDescriptor beanDescriptor;

  private final JClassType jClass;

  private Class<?> clazz;

  private BeanHelper(JClassType jClass, Class<?> clazz,
      BeanDescriptor beanDescriptor) {
    super();
    this.beanDescriptor = beanDescriptor;
    this.jClass = jClass;
    this.clazz = clazz;
  }

  public JClassType getAssociationType(PropertyDescriptor p, boolean useField) {
    JType type = this.getElementType(p, useField);
    JArrayType jArray = type.isArray();
    if (jArray != null) {
      return jArray.getComponentType().isClassOrInterface();
    }
    JParameterizedType pType = type.isParameterized();
    JClassType[] typeArgs;
    if (pType == null) {
      JRawType rType = type.isRawType();
      typeArgs = rType.getGenericType().getTypeParameters();
    } else {
      typeArgs = pType.getTypeArgs();
    }
    // it is either a Iterable or a Map use the last type arg.
    return typeArgs[typeArgs.length - 1].isClassOrInterface();
  }

  public BeanDescriptor getBeanDescriptor() {
    return beanDescriptor;
  }

  /*
   * The server side validator needs an actual class.
   */
  public Class<?> getClazz() {
    return clazz;
  }

  public String getDescriptorName() {

    return jClass.getName() + "Descriptor";
  }

  public String getFullyQualifiedValidatorName() {
    return getPackage() + "." + getValidatorName();
  }

  public JClassType getJClass() {
    return jClass;
  }

  public String getPackage() {
    return jClass.getPackage().getName();
  }

  public String getTypeCanonicalName() {
    return jClass.getQualifiedSourceName();
  }

  public String getValidatorInstanceName() {
    return getFullyQualifiedValidatorName() + ".INSTANCE";
  }

  public String getValidatorName() {
    return makeJavaSafe("_" + jClass.getName() + "Validator");
  }

  @Override
  public String toString() {
    return getTypeCanonicalName();
  }

  JType getElementType(PropertyDescriptor p, boolean useField) {
    if (useField) {
      return jClass.findField(p.getPropertyName()).getType();
    } else {
      return jClass.findMethod(GwtSpecificValidatorCreator.asGetter(p),
          GwtSpecificValidatorCreator.NO_ARGS).getReturnType();
    }
  }

  boolean hasField(PropertyDescriptor p) {
    JField field = jClass.findField(p.getPropertyName());
    return field != null;
  }

  boolean hasGetter(PropertyDescriptor p) {
    JType[] paramTypes = new JType[]{};
    try {
      jClass.getMethod(GwtSpecificValidatorCreator.asGetter(p), paramTypes);
      return true;
    } catch (NotFoundException e) {
      return false;
    }
  }

  private String makeJavaSafe(String in) {
    return in.replaceAll("\\.", "_");
  }

}
