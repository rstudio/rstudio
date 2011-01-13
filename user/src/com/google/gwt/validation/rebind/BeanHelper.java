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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.validation.client.impl.GwtSpecificValidator;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;

/**
 * A simple struct for the various values associated with a Bean that can be
 * validated.
 */
final class BeanHelper {

  private static final Validator serverSideValidor = Validation.buildDefaultValidatorFactory().getValidator();

  // stash the map in a ThreadLocal, since each GWT module lives in its own
  // thread in DevMode
  private static final ThreadLocal<Map<JClassType, BeanHelper>> threadLocalHelperMap =
      new ThreadLocal<Map<JClassType, BeanHelper>>() {
    @Override
    protected synchronized Map<JClassType, BeanHelper> initialValue() {
      return new HashMap<JClassType, BeanHelper>();
    }
  };

  protected static BeanHelper createBeanHelper(Class<?> clazz,
      TreeLogger logger, GeneratorContext context) {
    JClassType beanType = context.getTypeOracle().findType(
        clazz.getCanonicalName());
    BeanHelper helper = getBeanHelper(beanType);
    if (helper == null) {
      helper = new BeanHelper(beanType, clazz,
          serverSideValidor.getConstraintsForClass(clazz));
      addBeanHelper(helper);
      writeInterface(context, logger, helper);
    }
    return helper;
  }

  protected static BeanHelper createBeanHelper(JType jType, TreeLogger logger,
      GeneratorContext context) throws UnableToCompleteException {
    try {
      Class<?> clazz = Class.forName(jType.getQualifiedSourceName());
      return createBeanHelper(clazz, logger, context);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to create BeanHelper for " + jType,
          e);
      throw new UnableToCompleteException();
    }
  }

  protected static boolean isClassConstrained(Class<?> clazz) {
    return serverSideValidor.getConstraintsForClass(clazz).isBeanConstrained();
  }

  static BeanHelper getBeanHelper(JClassType beanType) {
    return getBeanHelpers().get(beanType);
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
      factory.makeInterface();
      SourceWriter sw = factory.createSourceWriter(context, pw);
      sw.commit(interfaceLogger);
      pw.close();
    }
  }

  private static synchronized void addBeanHelper(BeanHelper helper) {
    threadLocalHelperMap.get().put(helper.getJClass(), helper);
  }

  private static Map<JClassType, BeanHelper> getBeanHelpers() {
    return Collections.unmodifiableMap(threadLocalHelperMap.get());
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
    return makeJavaSafe(jClass.getName().toLowerCase() + "Validator");
  }

  public String getValidatorName() {
    return makeJavaSafe("_" + jClass.getName() + "Validator");
  }

  @Override
  public String toString() {
    return getTypeCanonicalName();
  }

  private String makeJavaSafe(String in) {
    return in.replaceAll("\\.", "_");
  }

}
