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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.ValidationGroupsMetadata;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;
import com.google.gwt.validation.client.impl.GwtBeanDescriptor;
import com.google.gwt.validation.client.impl.GwtSpecificValidator;
import com.google.gwt.validation.client.impl.GwtValidationContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.GroupSequence;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Class that creates the validator for the given input class.
 */
public final class ValidatorCreator extends AbstractCreator {

  /**
   * The beans to validate in source declaration order.
   */
  private final ImmutableList<BeanHelper> beansToValidate;
  private final GwtValidation gwtValidation;

  public ValidatorCreator(JClassType validatorType, //
      GwtValidation gwtValidation, //
      TreeLogger logger, //
      GeneratorContext context) throws UnableToCompleteException {
    super(context, logger, validatorType);
    this.gwtValidation = gwtValidation;

    List<BeanHelper> temp = Lists.newArrayList();
    for (Class<?> clazz : gwtValidation.value()) {
      BeanHelper helper = createBeanHelper(clazz);
      temp.add(helper);
    }
    beansToValidate = Util.sortMostSpecificFirst(temp, BeanHelper.TO_CLAZZ);
  }

  @Override
  protected void compose(ClassSourceFileComposerFactory composerFactory) {
   addImports(composerFactory,
       GWT.class,
       GwtBeanDescriptor.class,
       GwtSpecificValidator.class,
       GwtValidationContext.class,
       ValidationGroupsMetadata.class,
       Set.class,
       HashSet.class,
       Map.class,
       HashMap.class,
       Default.class,
       ConstraintViolation.class,
       BeanDescriptor.class);
    composerFactory.setSuperclass(AbstractGwtValidator.class.getCanonicalName());
    composerFactory.addImplementedInterface(this.validatorType.getQualifiedSourceName());
  }

  @Override
  protected void writeClassBody(SourceWriter sourceWriter) {
    writeConstructor(sourceWriter);
    sourceWriter.println();
    writeCreateValidationGroupsMetadata(sourceWriter);
    sourceWriter.println();
    writeValidate(sourceWriter);
    sourceWriter.println();
    writeValidateProperty(sourceWriter);
    sourceWriter.println();
    writeValidateValue(sourceWriter);
    sourceWriter.println();
    writeGetConstraintsForClass(sourceWriter);
    sourceWriter.println();
    writeGwtValidate(sourceWriter);
  }

  private void writeConstructor(SourceWriter sw) {
    // public MyValidator() {
    sw.println("public " + getSimpleName() + "() {");
    sw.indent();

    // super(createValidationGroupsMetadata());
    sw.println("super(createValidationGroupsMetadata());");

    sw.outdent();
    sw.println("}");
  }

  private void writeContext(SourceWriter sw, BeanHelper bean, String objectName) {
    // GwtValidationContext<MyBean> context = new GwtValidationContext<MyBean>(
    sw.print(GwtValidationContext.class.getSimpleName());
    sw.print("<T> context = new ");
    sw.print(GwtValidationContext.class.getSimpleName());
    sw.println("<T>(");
    sw.indent();
    sw.indent();

    // (Class<T>) MyBean.class,
    sw.print("(Class<T>) ");
    sw.println(bean.getTypeCanonicalName() + ".class, ");

    // object,
    sw.println(objectName + ", ");

    // MyBeanValidator.INSTANCE.getConstraints(getValidationGroupsMetadata()),
    sw.print(bean.getFullyQualifiedValidatorName());
    sw.println(".INSTANCE.getConstraints(getValidationGroupsMetadata()), ");

    // getMessageInterpolator(),
    sw.println("getMessageInterpolator(), ");

    // this);
    sw.println("this);");
    sw.outdent();
    sw.outdent();
  }

  private void writeCreateValidationGroupsMetadata(SourceWriter sw) {
    // private static ValidationGroupsMetadata createValidationGroupsMetadata() {
    sw.println("private static ValidationGroupsMetadata createValidationGroupsMetadata() {");
    sw.indent();
    
    // return ValidationGroupsMetadata.builder()
    sw.println("return ValidationGroupsMetadata.builder()");
    sw.indent();
    sw.indent();
    for (Class<?> group : gwtValidation.groups()) {
      GroupSequence sequenceAnnotation = group.getAnnotation(GroupSequence.class);
      Class<?>[] groups;
      if (sequenceAnnotation != null) {
        // .addSequence(<<sequence>>
        sw.print(".addSequence(");
        sw.print(group.getCanonicalName() + ".class");
        groups = sequenceAnnotation.value();
      } else {
        // .addGroup(<<group>>
        sw.print(".addGroup(");
        sw.print(group.getCanonicalName() + ".class");
        groups = group.getInterfaces();
      }
      for (Class<?> clazz : groups) {
        // , <<group class>>
        sw.print(", ");
        sw.print(clazz.getCanonicalName() + ".class");
      }
      // )
      sw.println(")");
    }

    // .build();
    sw.println(".build();");
    sw.outdent();
    sw.outdent();

    // }
    sw.outdent();
    sw.println("}");
  }

  private void writeGetConstraintsForClass(SourceWriter sw) {
    // public BeanDescriptor getConstraintsForClass(Class<?> clazz {
    sw.println("public BeanDescriptor getConstraintsForClass(Class<?> clazz) {");
    sw.indent();

    // checkNotNull(clazz, "clazz");
    sw.println("checkNotNull(clazz, \"clazz\");");

    for (BeanHelper bean : beansToValidate) {
      writeGetConstraintsForClass(sw, bean);
    }

    writeThrowIllegalArgumnet(sw, "clazz.getName()");

    // }
    sw.outdent();
    sw.println("}");
  }

  private void writeGetConstraintsForClass(SourceWriter sw,
      BeanHelper bean) {
    // if (clazz.eqals(MyBean.class)) {
    sw.println("if (clazz.equals(" + bean.getTypeCanonicalName() + ".class)) {");
    sw.indent();

    // return MyBeanValidator.INSTANCE.getConstraints(getValidationGroupsMetadata());
    sw.print("return ");
    sw.print(bean.getFullyQualifiedValidatorName());
    sw.println(".INSTANCE.getConstraints(getValidationGroupsMetadata());");

    // }
    sw.outdent();
    sw.println("}");
  }

  private void writeGwtValidate(SourceWriter sw) {
    // public <T> Set<ConstraintViolation<T>> validate(GwtValidationContext<T>
    // context,
    sw.print("public <T> Set<ConstraintViolation<T>> ");
    sw.println("validate(GwtValidationContext<T> context,");
    sw.indent();
    sw.indent();

    // Object object, Class<?>... groups) {
    sw.println("Object object, Class<?>... groups) {");
    sw.outdent();

    sw.println("checkNotNull(context, \"context\");");
    sw.println("checkNotNull(object, \"object\");");
    sw.println("checkNotNull(groups, \"groups\");");
    sw.println("checkGroups(groups);");

    for (BeanHelper bean : Util.sortMostSpecificFirst(
        BeanHelper.getBeanHelpers().values(), BeanHelper.TO_CLAZZ)) {
      writeGwtValidate(sw, bean);
    }

    // TODO(nchalko) log warning instead.
    writeThrowIllegalArgumnet(sw, "object.getClass().getName()");

    sw.outdent();
    sw.println("}");
  }

  private void writeGwtValidate(SourceWriter sw, BeanHelper bean) {
    writeIfInstanceofBeanType(sw, bean);
    sw.indent();

    // return PersonValidator.INSTANCE

    sw.print("return ");
    sw.println(bean.getFullyQualifiedValidatorName() + ".INSTANCE");
    sw.indent();
    sw.indent();
    // .validate(context, (<<MyBean>>) object, groups);
    sw.print(".validate(context, ");
    sw.print("(" + bean.getTypeCanonicalName() + ") object, ");
    sw.println("groups);");
    sw.outdent();
    sw.outdent();

    // }
    sw.outdent();
    sw.println("}");
  }

  private void writeIfInstanceofBeanType(SourceWriter sourceWriter, BeanHelper bean) {
    // if (object instanceof MyBean) {
    sourceWriter.print("if (object instanceof ");
    sourceWriter.print(bean.getTypeCanonicalName());
    sourceWriter.println(") {");
  }

  private void writeThrowIllegalArgumnet(SourceWriter sourceWriter,
      String getClassName) {
    // throw new IllegalArgumentException("MyValidator can not validate ",
    sourceWriter.print("throw new IllegalArgumentException(\"");
    sourceWriter.print(this.validatorType.getName() + " can not  validate \"");
    sourceWriter.indent();
    sourceWriter.indent();

    // + object.getClass().getName() +". "
    sourceWriter.print("+ ");
    sourceWriter.print(getClassName);
    sourceWriter.println("+ \". \"");

    // + "Valid values are {Foo.clas, Bar.class}
    sourceWriter.print("+ \"Valid types are ");
    sourceWriter.print(beansToValidate.toString());
    sourceWriter.println("\");");
    sourceWriter.outdent();
    sourceWriter.outdent();
  }

  private void writeValidate(SourceWriter sw) {
    // public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>...
    // groups) {
    sw.println("public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {");
    sw.indent();

    sw.println("checkNotNull(object, \"object\");");
    sw.println("checkNotNull(groups, \"groups\");");
    sw.println("checkGroups(groups);");

    for (BeanHelper bean : beansToValidate) {
      writeValidate(sw, bean);
    }

    writeThrowIllegalArgumnet(sw, "object.getClass().getName()");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidate(SourceWriter sw, BeanHelper bean) {
    writeIfInstanceofBeanType(sw, bean);
    sw.indent();

    writeContext(sw, bean, "object");

    // return PersonValidator.INSTANCE
    sw.print("return ");
    sw.println(bean.getFullyQualifiedValidatorName() + ".INSTANCE");
    sw.indent();
    sw.indent();

    // .validate(context, (<<MyBean>>) object, groups);
    sw.print(".validate(context, ");
    sw.print("(" + bean.getTypeCanonicalName() + ") object, ");
    sw.println("groups);");
    sw.outdent();
    sw.outdent();

    // }
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

    for (BeanHelper bean : beansToValidate) {
      writeValidateProperty(sw, bean);
    }

    writeThrowIllegalArgumnet(sw, "object.getClass().getName()");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidateProperty(SourceWriter sw, BeanHelper bean) {
    writeIfInstanceofBeanType(sw, bean);
    sw.indent();
    writeContext(sw, bean, "object");

    // return PersonValidator.INSTANCE
    sw.print("return ");
    sw.println(bean.getFullyQualifiedValidatorName() + ".INSTANCE");
    sw.indent();
    sw.indent();

    // .validateProperty(context, (MyBean) object, propertyName, groups);
    sw.print(".validateProperty(context, (");
    sw.print(bean.getTypeCanonicalName());
    sw.print(") object, propertyName, ");
    sw.println("groups);");
    sw.outdent();
    sw.outdent();

    // }
    sw.outdent();
    sw.println("}");
  }

  private void writeValidateValue(SourceWriter sw) {
    sw.println("public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {");
    sw.indent();

    sw.println("checkNotNull(beanType, \"beanType\");");
    sw.println("checkNotNull(propertyName, \"propertyName\");");
    sw.println("checkNotNull(groups, \"groups\");");
    sw.println("checkGroups(groups);");

    for (BeanHelper bean : beansToValidate) {
      writeValidateValue(sw, bean);
    }

    writeThrowIllegalArgumnet(sw, "beanType.getName()");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidateValue(SourceWriter sw, BeanHelper bean) {
    sw.println("if (beanType.equals(" + bean.getTypeCanonicalName()
        + ".class)) {");
    sw.indent();
    writeContext(sw, bean, "null");

    // return PersonValidator.INSTANCE
    sw.print("return ");
    sw.println(bean.getFullyQualifiedValidatorName() + ".INSTANCE");
    sw.indent();
    sw.indent();

    // .validateValue(context, (Class<MyBean> beanType, propertyName, value,
    // groups);
    sw.print(".validateValue(context, (Class<");
    sw.print(bean.getTypeCanonicalName());
    sw.println(">)beanType, propertyName, value, groups);");
    sw.outdent();
    sw.outdent();

    // }
    sw.outdent();
    sw.println("}");
  }
}
