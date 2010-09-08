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
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.validation.client.impl.AbstractGwtSpecificValidator;
import com.google.gwt.validation.client.impl.GwtBeanDescriptor;
import com.google.gwt.validation.client.impl.GwtValidationContext;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.metadata.BeanDescriptor;

/**
 * Creates a {@link com.google.gwt.validation.client.GwtSpecificValidator}.
 * <p>
 * This class is not thread safe.
 */

public class GwtSpecificValidatorCreator extends AbstractCreator {

  final JClassType beanType;
  private BeanHelper beanHelper;
  private final TypeOracle oracle;

  public GwtSpecificValidatorCreator(JClassType validatorType,
      JClassType beanType, BeanHelper beanHelper, TreeLogger logger,
      GeneratorContext context) {
    super(context, logger, validatorType);
    this.oracle = context.getTypeOracle();
    this.beanType = beanType;
    this.beanHelper = beanHelper;
  }

  @Override
  protected void compose(ClassSourceFileComposerFactory composerFactory) {
    Class<?>[] imports = new Class<?>[]{
        GWT.class,
        GwtBeanDescriptor.class,
        GwtValidationContext.class,
        Set.class,
        HashSet.class,
        ConstraintViolation.class,
        BeanDescriptor.class,
        };
    for (Class<?> imp : imports) {
      composerFactory.addImport(imp.getCanonicalName());
    }

    composerFactory.setSuperclass(AbstractGwtSpecificValidator.class.getCanonicalName()
        + "<" + beanType.getQualifiedSourceName() + ">");

    composerFactory.addImplementedInterface(validatorType.getName());
  }

  @Override
  protected void writeClassBody(SourceWriter sw) {
    writeFields(sw);
    sw.println();
    writeValidate(sw);
    sw.println();
    writeValidateProperty(sw);
    sw.println();
    writeValidateValue(sw);
    sw.println();
    writeGetDescriptor(sw);
  }

  protected void writeNewViolations(SourceWriter sw) {
    // Set<ConstraintViolation<T>> violations = new
    // HashSet<ConstraintViolation<T>>();
    sw.println("Set<ConstraintViolation<T>> violations = new HashSet<ConstraintViolation<T>>();");
  }

  /**
   * @param sourceWriter
   */
  private void writeFields(SourceWriter sw) {
    // MyBeanDescriptor beanDescriptor = GWT.create(MyBeanDescriptor);
    sw.println(GwtBeanDescriptor.class.getCanonicalName());
    sw.println(" beanDescriptor = null; // GWT.create");
  }

  private void writeGetDescriptor(SourceWriter sw) {
    // public GwtBeanDescriptor<beanType> getConstraints() {
    sw.print("public ");
    sw.print("GwtBeanDescriptor<" + beanHelper.getTypeCanonicalName() + "> ");
    sw.println("getConstraints() {");
    sw.indent();

    //    return beanDescriptor;
    sw.println("return beanDescriptor;");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidate(SourceWriter sw) {
    // public <T> Set<ConstraintViolation<T>> validate(
    sw.println("public <T> Set<ConstraintViolation<T>> validate(");

    // GwtValidationContext<T> context, BeanType object, Class<?>... groups) {
    sw.indent();
    sw.indent();
    sw.println("GwtValidationContext<T> context,");
    sw.println(beanHelper.getTypeCanonicalName() + " object,");
    sw.println("Class<?>... groups) {");
    sw.outdent();

    writeNewViolations(sw);

    // TODO(nchalko) loop over all constraints

    // return violations;
    sw.println("return violations;");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidateProperty(SourceWriter sw) {
    // public <T> Set<ConstraintViolation<T>> validate(
    sw.println("public <T> Set<ConstraintViolation<T>> validateProperty(");

    // GwtValidationContext<T> context, BeanType object, String propertyName,
    // Class<?>... groups) {
    sw.indent();
    sw.indent();
    sw.println("GwtValidationContext<T> context,");
    sw.println(beanHelper.getTypeCanonicalName() + " object,");
    sw.println("String propertyName,");
    sw.println("Class<?>... groups) {");
    sw.outdent();

    writeNewViolations(sw);

    // TODO(nchalko) case statement for propertyName

    // return violations;
    sw.println("return violations;");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidateValue(SourceWriter sw) {
    // public <T> Set<ConstraintViolation<T>> validate(
    sw.println("public <T> Set<ConstraintViolation<T>> validateValue(");

    // GwtValidationContext<T> context, Class<Author> beanType,
    // String propertyName, Object value, Class<?>... groups) {
    sw.indent();
    sw.indent();
    sw.println("GwtValidationContext<T> context,");
    sw.println("Class<" + beanHelper.getTypeCanonicalName() + "> beanType,");
    sw.println("String propertyName,");
    sw.println("Object value,");
    sw.println("Class<?>... groups) {");
    sw.outdent();

    writeNewViolations(sw);

    // TODO(nchalko) case statement for propertyName

    // return violations;
    sw.println("return violations;");

    sw.outdent();
    sw.println("}");
  }
}
