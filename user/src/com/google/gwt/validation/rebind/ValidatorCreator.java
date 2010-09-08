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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;
import com.google.gwt.validation.client.impl.GwtBeanDescriptor;
import com.google.gwt.validation.client.impl.GwtSpecificValidator;
import com.google.gwt.validation.client.impl.GwtValidationContext;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.metadata.BeanDescriptor;

/**
 * Class that creates the validator for the given input class.
 */
public class ValidatorCreator {

  // stash the map in a ThreadLocal, since each GWT module lives in its own
  // thread in DevMode
  private static final ThreadLocal<Map<JClassType, BeanHelper>> threadLocalHelperMap = new ThreadLocal<Map<JClassType, BeanHelper>>() {
    protected synchronized Map<JClassType, BeanHelper> initialValue() {
      return new HashMap<JClassType, BeanHelper>();
    }
  };

  public static BeanHelper getBeanHelper(JClassType beanType) {
    return getBeanHelpers().get(beanType);
  }

  public static Map<JClassType, BeanHelper> getBeanHelpers() {
    return Collections.unmodifiableMap(threadLocalHelperMap.get());
  }

  private final Map<JClassType, BeanHelper> beansToValidate = new HashMap<JClassType, BeanHelper>();
  private final GeneratorContext context;
  private final TreeLogger logger;
  private final JClassType validatorType;

  public ValidatorCreator(JClassType validatorType,
      GwtValidation gwtValidation, TreeLogger logger,
      GeneratorContext context) {
    this.validatorType = validatorType;
    this.logger = logger;
    this.context = context;
    TypeOracle oracle = context.getTypeOracle();

    for (Class<?> clazz : gwtValidation.value()) {
      JClassType jClass = oracle.findType(clazz.getCanonicalName());
      BeanHelper helper = new BeanHelper(jClass);
      beansToValidate.put(jClass, helper);
    }
    threadLocalHelperMap.get().putAll(beansToValidate);
  }

  public String create() {
    SourceWriter sourceWriter = getSourceWriter(logger, context);
    if (sourceWriter != null) {
      writeTypeSupport(sourceWriter);
      writeValidate(sourceWriter);
      writeValidateProperty(sourceWriter);
      writeValidateValue(sourceWriter);
      writeGetConstraintsForClass(sourceWriter);

      sourceWriter.commit(logger);
    }
    return getQaulifiedName();
  }

  protected void writeContext(SourceWriter sourceWriter, BeanHelper bean,
      String objectName) {
    sourceWriter.print(GwtValidationContext.class.getCanonicalName()
        + "<T> context = new " + GwtValidationContext.class.getCanonicalName()
        + "<T>(" + objectName + ",");
    sourceWriter.println(bean.getValidatorInstanceName()
        + ".getConstraints());");
  }

  protected void writeGetConstraintsForClass(SourceWriter sourceWriter) {
    sourceWriter.println("public BeanDescriptor getConstraintsForClass(Class<?> clazz) {");
    sourceWriter.indent();
    sourceWriter.println("return null;");
    sourceWriter.outdent();
    sourceWriter.println("}");
    sourceWriter.println();
  }

  protected void writeIfEqulsBeanType(SourceWriter sourceWriter, BeanHelper bean) {
    sourceWriter.println("if (object.getClass().equals("
        + bean.getTypeCanonicalName() + ".class)) {");
  }

  protected void writeThrowIllegalArgumnet(SourceWriter sourceWriter) {
    sourceWriter.print("throw new IllegalArgumentException(\""
        + this.validatorType.getName() + " can only validate ");
    sourceWriter.print(beansToValidate.toString());
    sourceWriter.println("\");");
  }

  protected void writeTypeSupport(SourceWriter sw) {
    // TODO (nchalko) write these as top level interfaces.
    // As top level interfaces other generated Validators can use them.
    // Without it a gwt application can only have ONE validator.
    for (BeanHelper bean : beansToValidate.values()) {
      sw.println("public interface " + bean.getValidatorName()
          + " extends GwtSpecificValidator<" + bean.getTypeCanonicalName()
          + "> {");
      sw.println("}");

      sw.println("public interface " + bean.getDescriptorName()
          + " extends GwtBeanDescriptor<"
          + bean.getTypeCanonicalName() + "> {");
      sw.println("}");

      sw.print("private final " + bean.getValidatorName() + " ");
      sw.print(bean.getValidatorInstanceName());
      sw.print(" = GWT.create(" + bean.getValidatorName() + ".class);");
      sw.println();
    }
  }

  protected void writeValidate(SourceWriter sw) {
    sw.println("public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {");
    sw.indent();
    for (BeanHelper bean : beansToValidate.values()) {
      writeValidate(sw, bean);
    }
    writeThrowIllegalArgumnet(sw);
    sw.outdent(); // class
    sw.println("}");
    sw.println();
  }

  protected void writeValidate(SourceWriter sw, BeanHelper bean) {
    writeIfEqulsBeanType(sw, bean);
    sw.indent();
    writeContext(sw, bean, "object");
    sw.print("return " + bean.getValidatorInstanceName()
        + ".validate(context, (" + bean.getTypeCanonicalName() + ") object, ");
    sw.println("groups);");
    sw.outdent(); // if
    sw.println("}");
  }

  protected void writeValidateProperty(SourceWriter sw) {
    sw.println("public <T> Set<ConstraintViolation<T>> validateProperty(T object,String propertyName, Class<?>... groups) {");
    sw.indent();
    for (BeanHelper bean : beansToValidate.values()) {
      writeValidateProperty(sw, bean);
    }
    writeThrowIllegalArgumnet(sw);
    sw.outdent();
    sw.println("}");
    sw.println();
  }

  protected void writeValidateProperty(SourceWriter sw, BeanHelper bean) {
    writeIfEqulsBeanType(sw, bean);
    sw.indent();
    writeContext(sw, bean, "object");
    sw.print("return " + bean.getValidatorInstanceName()
        + ".validateProperty(context, (" + bean.getTypeCanonicalName()
        + ") object, propertyName, ");
    sw.println("groups);");
    sw.outdent(); // if
    sw.println("}");
  }

  protected void writeValidateValue(SourceWriter sw) {
    sw.println("public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {");
    sw.indent();
    for (BeanHelper bean : beansToValidate.values()) {
      writeValidateValue(sw, bean);
    }
    writeThrowIllegalArgumnet(sw);
    sw.outdent();
    sw.println("}");
    sw.println();
  }

  protected void writeValidateValue(SourceWriter sw, BeanHelper bean) {
    sw.println("if (beanType.getClass().equals(" + bean.getTypeCanonicalName()
        + ".class)) {");
    sw.indent();
    writeContext(sw, bean, "null");
    sw.println("return " + bean.getValidatorInstanceName()
        + ".validateValue(context, (Class<" + bean.getTypeCanonicalName()
        + ">)beanType, propertyName, value, groups);");
    sw.outdent(); // if
    sw.println("}");
  }

  private String getQaulifiedName() {
    return validatorType.getQualifiedSourceName() + "Impl";
  }

  private String getSimpleName() {
    return validatorType.getSimpleSourceName() + "Impl";
  }

  private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx) {
    JPackage serviceIntfPkg = validatorType.getPackage();
    String packageName = serviceIntfPkg == null ? "" : serviceIntfPkg.getName();
    String simpleName = getSimpleName();
    PrintWriter printWriter = ctx.tryCreate(logger, packageName, simpleName);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, simpleName);

    String[] imports = new String[]{
        GWT.class.getCanonicalName(),
        GwtBeanDescriptor.class.getCanonicalName(),
        GwtSpecificValidator.class.getCanonicalName(),
        GwtValidationContext.class.getCanonicalName(),
        Set.class.getCanonicalName(),
        ConstraintViolation.class.getCanonicalName(),
        BeanDescriptor.class.getCanonicalName()};
    for (String imp : imports) {
      composerFactory.addImport(imp);
    }

    composerFactory.setSuperclass(AbstractGwtValidator.class.getCanonicalName());
    SourceWriter sourceWriter = composerFactory.createSourceWriter(ctx,
        printWriter);

    return sourceWriter;
  }
}
