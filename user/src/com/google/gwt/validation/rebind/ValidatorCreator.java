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
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.AbstractSourceCreator;
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
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;

/**
 * Class that creates the validator for the given input class.
 */
public class ValidatorCreator extends AbstractCreator {

  // stash the map in a ThreadLocal, since each GWT module lives in its own
  // thread in DevMode
  private static final ThreadLocal<Map<JClassType, BeanHelper>> threadLocalHelperMap = new ThreadLocal<Map<JClassType, BeanHelper>>() {
    @Override
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
  private final GwtValidation gwtValidation;
  private final Validator serverSideValidor = Validation.buildDefaultValidatorFactory().getValidator();

  public ValidatorCreator(JClassType validatorType, //
      GwtValidation gwtValidation, //
      TreeLogger logger, //
      GeneratorContext context) {
    super(context, logger, validatorType);
    this.gwtValidation = gwtValidation;
    TypeOracle oracle = context.getTypeOracle();

    for (Class<?> clazz : gwtValidation.value()) {
      JClassType jClass = oracle.findType(clazz.getCanonicalName());
      BeanHelper helper = new BeanHelper(jClass,
          serverSideValidor.getConstraintsForClass(clazz));
      beansToValidate.put(jClass, helper);
    }
    threadLocalHelperMap.get().putAll(beansToValidate);
  }

  @Override
  protected void compose(ClassSourceFileComposerFactory composerFactory) {
   addImports(composerFactory, 
       GWT.class,
       GwtBeanDescriptor.class,
       GwtSpecificValidator.class,
       GwtValidationContext.class,
       Set.class,
       ConstraintViolation.class,
       BeanDescriptor.class);
    composerFactory.setSuperclass(AbstractGwtValidator.class.getCanonicalName());
    composerFactory.addImplementedInterface(this.validatorType.getQualifiedSourceName());
  }

  @Override
  protected void writeClassBody(SourceWriter sourceWriter) {
      writeTypeSupport(sourceWriter);
      sourceWriter.println();
      writeConstructor(sourceWriter);
      sourceWriter.println();
      writeValidate(sourceWriter);
      sourceWriter.println();
      writeValidateProperty(sourceWriter);
      sourceWriter.println();
      writeValidateValue(sourceWriter);
      sourceWriter.println();
      writeGetConstraintsForClass(sourceWriter);
  }

  private String getSimpleName() {
    return validatorType.getSimpleSourceName() + "Impl";
  }

  private void writeConstructor(SourceWriter sw) {
    // public MyValidator() {
    sw.println("public " + getSimpleName() + "() {");
    sw.indent();

    // super( <<groups>>);
    sw.print("super(");
    boolean first = true;
    for (Class<?> group : gwtValidation.groups()) {
      if (!first) {
        sw.print(", ");
      } else {
        first = false;
      }
      sw.print(group.getCanonicalName() + ".class");
    }
    sw.println(");");

    sw.outdent();
    sw.println("}");
  }


  private void writeContext(SourceWriter sw, BeanHelper bean, String objectName) {
    // GwtValidationContext<T> context =
    // new GwtValidationContext<T>(object,myBeanValidator.getConstraints());
    sw.print(GwtValidationContext.class.getSimpleName());
    sw.print("<T> context =");
    sw.print("    new " + GwtValidationContext.class.getSimpleName());
    sw.print("<T>(" + objectName + ", ");
    sw.print(bean.getValidatorInstanceName());
    sw.print(".getConstraints()");
    sw.println(");");
  }

  private void writeGetConstraintsForClass(SourceWriter sourceWriter) {
    sourceWriter.println("public BeanDescriptor getConstraintsForClass(Class<?> clazz) {");
    sourceWriter.indent();
    sourceWriter.println("return null;");
    sourceWriter.outdent();
    sourceWriter.println("}");
  }

  private void writeIfEqualsBeanType(SourceWriter sourceWriter, BeanHelper bean) {
    sourceWriter.println("if (object.getClass().equals("
        + bean.getTypeCanonicalName() + ".class)) {");
  }

  /**
   * Write an Empty Interface implementing {@link GwtSpecificValidator} with
   * Generic parameter of the bean type.
   * 
   * @param bean
   */
  private void writeInterface(BeanHelper bean) {
    PrintWriter pw = context.tryCreate(logger, bean.getPackage(),
        bean.getValidatorName());
    if (pw != null) {
      TreeLogger interfaceLogger = AbstractSourceCreator.branch(
          logger,
          "Creating the interface for "
              + bean.getFullyQualifiedValidatorName());

      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          bean.getPackage(), bean.getValidatorName());
      factory.addImplementedInterface(GwtSpecificValidator.class.getCanonicalName()
          + " <" + bean.getTypeCanonicalName() + ">");
      factory.makeInterface();
      SourceWriter sw2 = factory.createSourceWriter(context, pw);
      sw2.commit(interfaceLogger);
      pw.close();
    }
  }

  private void writeThrowIllegalArgumnet(SourceWriter sourceWriter) {
    sourceWriter.print("throw new IllegalArgumentException(\""
        + this.validatorType.getName() + " can only validate ");
    sourceWriter.print(beansToValidate.toString());
    sourceWriter.println("\");");
  }

  private void writeTypeSupport(SourceWriter sw) {
    for (BeanHelper bean : beansToValidate.values()) {
      writeInterface(bean);
      // private final MyBeanValidator myBeanValidator =
      sw.print("private final " + bean.getFullyQualifiedValidatorName() + " ");
      sw.print(bean.getValidatorInstanceName());
      sw.println(" = ");
      sw.indent();
      sw.indent();

      // GWT.create(MyBeanValidator
      sw.println("GWT.create(" + bean.getFullyQualifiedValidatorName()
          + ".class);");
      sw.outdent();
      sw.outdent();
    }
  }

  private void writeValidate(SourceWriter sw) {
    // public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>...
    // groups) {
    sw.println("public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {");
    sw.indent();

    sw.println("checkNotNull(object, \"object\");");
    sw.println("checkNotNull(groups, \"groups\");");
    sw.println("checkGroups(groups);");

    for (BeanHelper bean : beansToValidate.values()) {
      writeValidate(sw, bean);
    }

    writeThrowIllegalArgumnet(sw);

    sw.outdent();
    sw.println("}");
  }

  private void writeValidate(SourceWriter sw, BeanHelper bean) {
    writeIfEqualsBeanType(sw, bean);
    sw.indent();

    writeContext(sw, bean, "object");

    // return personValidator.validate(context, (<<MyBean>>) object, groups);
    sw.print("return ");
    sw.print(bean.getValidatorInstanceName() + ".validate(");
    sw.print("context, ");
    sw.print("(" + bean.getTypeCanonicalName() + ") object, ");
    sw.println("groups);");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidateProperty(SourceWriter sw) {
    sw.println("public <T> Set<ConstraintViolation<T>> validateProperty(T object,String propertyName, Class<?>... groups) {");
    sw.indent();

    sw.println("checkNotNull(object, \"object\");");
    sw.println("checkNotNull(propertyName, \"propertyName\");");
    sw.println("checkNotNull(groups, \"groups\");");
    sw.println("checkGroups(groups);");

    for (BeanHelper bean : beansToValidate.values()) {
      writeValidateProperty(sw, bean);
    }

    writeThrowIllegalArgumnet(sw);

    sw.outdent();
    sw.println("}");
  }

  private void writeValidateProperty(SourceWriter sw, BeanHelper bean) {
    writeIfEqualsBeanType(sw, bean);
    sw.indent();
    writeContext(sw, bean, "object");
    sw.print("return " + bean.getValidatorInstanceName()
        + ".validateProperty(context, (" + bean.getTypeCanonicalName()
        + ") object, propertyName, ");
    sw.println("groups);");
    sw.outdent(); // if
    sw.println("}");
  }

  private void writeValidateValue(SourceWriter sw) {
    sw.println("public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {");
    sw.indent();

    sw.println("checkNotNull(beanType, \"beanType\");");
    sw.println("checkNotNull(propertyName, \"propertyName\");");
    sw.println("checkNotNull(groups, \"groups\");");
    sw.println("checkGroups(groups);");

    for (BeanHelper bean : beansToValidate.values()) {
      writeValidateValue(sw, bean);
    }

    writeThrowIllegalArgumnet(sw);

    sw.outdent();
    sw.println("}");
  }

  private void writeValidateValue(SourceWriter sw, BeanHelper bean) {
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
}
