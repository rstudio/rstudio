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
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.validation.client.impl.AbstractGwtSpecificValidator;
import com.google.gwt.validation.client.impl.ConstraintDescriptorImpl;
import com.google.gwt.validation.client.impl.GwtBeanDescriptor;
import com.google.gwt.validation.client.impl.GwtValidationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintViolation;
import javax.validation.Payload;
import javax.validation.Validator;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 * Creates a {@link com.google.gwt.validation.client.GwtSpecificValidator}.
 * <p>
 * This class is not thread safe.
 */

public class GwtSpecificValidatorCreator extends AbstractCreator {

  /**
     * Returns the literal value of an object that is suitable for inclusion in
     * Java Source code.
     *
     * <p>
     * Supports all types that {@link Annotation) value can have.
     *
     *
     * @throws IllegalArgumentException if the type of the object does not have a java literal form.
     */
    public static String asLiteral(Object value) throws IllegalArgumentException {
      Class<?> clazz = value.getClass();
      JProgram jProgram = new JProgram();

      if (clazz.isArray()) {
        StringBuilder sb = new StringBuilder();
        Object[] array = (Object[]) value;

        sb.append("new " + clazz.getComponentType().getCanonicalName() + "[] ");
        sb.append("{");
        boolean first = true;
        for (Object object : array) {
          if (first) {
            first = false;
          } else {
            sb.append(",");
          }
          sb.append(asLiteral(object));
        }
        sb.append("}");
        return sb.toString();
      }

      if (value instanceof Class) {
        return ((Class<?>) ((Class<?>) value)).getCanonicalName() + ".class";
      }
      if (value instanceof Double) {
        return jProgram.getLiteralDouble(((Double) value).doubleValue()).toSource();
      }
      if (value instanceof Integer) {
        return jProgram.getLiteralInt(((Integer) value).intValue()).toSource();
      }
      if (value instanceof Long) {
        return jProgram.getLiteralLong(((Long) value).intValue()).toSource();
      }
      if (value instanceof String) {
        return '"' + ((String) value).toString().replace("\"", "\\\"") + '"';
      }
    // TODO(nchalko) handle the rest of the literal types
      throw new IllegalArgumentException(value.getClass()
          + " is can not be represented as a Java Literal.");
    }
  private BeanHelper beanHelper;
  private final JClassType beanType;
  private final TypeOracle oracle;

  private final Validator serverSideValidator;

  public GwtSpecificValidatorCreator(JClassType validatorType,
      JClassType beanType, BeanHelper beanHelper, TreeLogger logger,
      GeneratorContext context, Validator serverSideValidator) {
    super(context, logger, validatorType);
    this.oracle = context.getTypeOracle();
    this.beanType = beanType;
    this.beanHelper = beanHelper;
    this.serverSideValidator = serverSideValidator;
  }

  protected <T> T[] asArray(Collection<?> collection, T[] array) {
    if (collection == null) {
      return null;
    }
    return collection.toArray(array);
  }

  protected String asGetter(PropertyDescriptor p) {
    return "get" + capitalizeFirstLetter(p.getPropertyName());
  }

  protected String capitalizeFirstLetter(String propertyName) {
    if (propertyName == null) {
      return null;
    }
    if (propertyName.length() == 0) {
      return "";
    }
    String cap = propertyName.substring(0, 1).toUpperCase();
    if (propertyName.length() > 1) {
      cap += propertyName.substring(1);
    }
    return cap;
  }

  @Override
  protected void compose(ClassSourceFileComposerFactory composerFactory) {
    addImports(composerFactory, GWT.class, GwtBeanDescriptor.class,
        GwtValidationContext.class, Set.class, HashSet.class,
        ConstraintViolation.class, Annotation.class);
    composerFactory.setSuperclass(AbstractGwtSpecificValidator.class.getCanonicalName()
        + "<" + beanType.getQualifiedSourceName() + ">");
    composerFactory.addImplementedInterface(validatorType.getName());
  }

  protected String constraintDescriptorVar(String name, int count) {
    String s = name + "_c" + count;
    return s;
  }

  protected Class<? extends ConstraintValidator<? extends Annotation, ?>> getValidatorForType(
      ConstraintDescriptor<? extends Annotation> constraint, Class<?> clazz) {
    // TODO(nchalko) implement per spec
    Class<? extends ConstraintValidator<? extends Annotation, ?>> validatorClass = constraint.getConstraintValidatorClasses().get(
        0);
    return validatorClass;
  }

  @Override
  protected void writeClassBody(SourceWriter sw)
      throws UnableToCompleteException {
    writeFields(sw);
    sw.println();
    writeValidate(sw);
    sw.println();
    writeValidateProperty(sw);
    sw.println();
    writeValidateValue(sw);
    sw.println();
    writeGetDescriptor(sw);
    sw.println();
    writePropertyValidtors(sw);
  }

  protected void writeConstraintDescriptor(SourceWriter sw,
      ConstraintDescriptor<? extends Annotation> constraint,
      String constraintDescripotorVar) throws UnableToCompleteException {
    Class<? extends Annotation> annotationType = constraint.getAnnotation().annotationType();

    // First list all composing constraints
    int count = 0;
    for (ConstraintDescriptor<?> composingConstraint : constraint.getComposingConstraints()) {
      writeConstraintDescriptor(sw, composingConstraint,
          constraintDescripotorVar + "_" + count++);
    }

    // ConstraintDescriptorImpl<MyAnnotation> constraintDescriptor = ;
    sw.print(ConstraintDescriptorImpl.class.getCanonicalName());
    sw.print("<");

    sw.print(annotationType.getCanonicalName());
    sw.print(">");

    sw.println(" " + constraintDescripotorVar + "  = ");
    sw.indent();
    sw.indent();

    // ConstraintDescriptorImpl.<MyConstraint> builder()
    sw.print(ConstraintDescriptorImpl.class.getCanonicalName());
    sw.print(".<");

    sw.print(annotationType.getCanonicalName());
    sw.println("> builder()");
    sw.indent();
    sw.indent();

    // .setAnnotation(new MyAnnotation )
    sw.println(".setAnnotation( ");
    sw.indent();
    sw.indent();
    writeNewAnnotation(sw, constraint);
    sw.println(")");
    sw.outdent();
    sw.outdent();

    // .setAttributes(builder()
    sw.println(".setAttributes(attributeBuilder()");
    sw.indent();

    for (Map.Entry<String, Object> entry : constraint.getAttributes().entrySet()) {
      // .put(key, value)
      sw.print(".put(");
      sw.print(asLiteral(entry.getKey()));
      sw.print(", ");
      sw.print(asLiteral(entry.getValue()));
      sw.println(")");
    }

    // .build())
    sw.println(".build())");
    sw.outdent();

    // .setConstraintValidatorClasses(classes )
    sw.print(".setConstraintValidatorClasses(");
    sw.print(asLiteral(asArray(constraint.getConstraintValidatorClasses(),
        new Class[0])));
    sw.println(")");

    // .getGroups(groups)
    sw.print(".setGroups(");
    Set<Class<?>> groups = constraint.getGroups();
    sw.print(asLiteral(asArray(groups, new Class<?>[0])));
    sw.println(")");

    // .setPayload(payload)
    sw.print(".setPayload(");
    Set<Class<? extends Payload>> payload = constraint.getPayload();
    sw.print(asLiteral(asArray(payload, new Class[0])));
    sw.println(")");

    // .setsetReportAsSingleViolation(boolean )
    sw.print(".setReportAsSingleViolation(");
    sw.print(Boolean.valueOf(constraint.isReportAsSingleViolation()).toString());
    sw.println(")");

    // .build();
    sw.println(".build();");
    sw.outdent();
    sw.outdent();

    sw.outdent();
    sw.outdent();
    sw.println();
  }

  protected void writeValidateInterfaces(SourceWriter sw, Class<?> clazz) {
    for (Class<?> type : clazz.getInterfaces()) {
      if (serverSideValidator.getConstraintsForClass(type).isBeanConstrained()) {
        // MyInterface_GwtSpecificValidator validator =
        // GWT.create(MyInterface_GwtSpecificValidator);
        // TODO (nchalko) validate interface. This Requires the
        // MyInterface_GwtSpecficValidator
        // interface to already be generated.
        sw.print("//TODO(nchalko) GWT.create(");
        sw.print(type.getCanonicalName());
        sw.println("_GwtSpecificValidator);");

        // voilations.add(validator.validate(context,this,groups));
      }

      writeValidateInterfaces(sw, type);
    }
  }

/**
 * @param beanHelper2
 * @param p
 * @return
 */
private boolean hasGetter(PropertyDescriptor p) {
  JType[] paramTypes = new JType[]{};
  try {
    beanType.getMethod(asGetter(p), paramTypes);
    return true;
  } catch (NotFoundException e) {
    return false;
  }

}

  private String validateMethodName(PropertyDescriptor p) {
    return "validateProperty_" + p.getPropertyName();
  }

  /**
   * @param sourceWriter
   * @throws UnableToCompleteException
   */
  private void writeFields(SourceWriter sw) throws UnableToCompleteException {
    // MyBeanDescriptor beanDescriptor = GWT.create(MyBeanDescriptor);
    sw.println(GwtBeanDescriptor.class.getCanonicalName());
    // TODO(nchalko) implement BeanDescriptor Generator
    sw.println(" beanDescriptor = null; //TODO(nchalko) GWT.create");

    // Create a variable for each constraint of each property
    for (PropertyDescriptor p : beanHelper.getBeanDescriptor().getConstrainedProperties()) {
      int count = 0;
      for (ConstraintDescriptor<?> constraint : p.getConstraintDescriptors()) {
        count++; // index starts at one.
        writeConstraintDescriptor(sw, constraint,
            constraintDescriptorVar(p.getPropertyName(), count));
      }
    }

    // Create a variable for each constraint of this class.
    int count = 0;
    for (ConstraintDescriptor<?> constraint : beanHelper.getBeanDescriptor().getConstraintDescriptors()) {
      count++; // index starts at one.
      writeConstraintDescriptor(sw, constraint,
          constraintDescriptorVar("this", count));
    }
  }

  private void writeGetDescriptor(SourceWriter sw) {
    // public GwtBeanDescriptor<beanType> getConstraints() {
    sw.print("public ");
    sw.print("GwtBeanDescriptor<" + beanHelper.getTypeCanonicalName() + "> ");
    sw.println("getConstraints() {");
    sw.indent();

    // return beanDescriptor;
    sw.println("return beanDescriptor;");

    sw.outdent();
    sw.println("}");
  }

  private void writeNewAnnotation(SourceWriter sw,
      ConstraintDescriptor<? extends Annotation> constraint)
      throws UnableToCompleteException {
    Annotation annotation = constraint.getAnnotation();
    Class<? extends Annotation> annotationType = annotation.annotationType();

    // new MyAnnotation () {
    sw.print("new ");
    sw.print(annotationType.getCanonicalName());
    sw.println("(){");
    sw.indent();
    sw.indent();

    // public Class<? extends Annotation> annotationType() { return
    // MyAnnotation.class; }
    sw.print("public Class<? extends Annotation> annotationType() {  return ");
    sw.print(annotationType.getCanonicalName());
    sw.println(".class; }");

    for (Method method : annotationType.getMethods()) {
      // method.isAbstract would be better
      if (method.getDeclaringClass().equals(annotation.annotationType())) {
        // public returnType method() { return value ;}
        sw.print("public ");
        sw.print(method.getReturnType().getCanonicalName()); // TODO handle
                                                             // generics
        sw.print(" ");
        sw.print(method.getName());
        sw.print("() { return ");

        try {
          Object value = method.invoke(annotation);
          sw.print(asLiteral(value));
        } catch (IllegalArgumentException e) {
          throw error(logger, e);
        } catch (IllegalAccessException e) {
          throw error(logger, e);
        } catch (InvocationTargetException e) {
          throw error(logger, e);
        }
        sw.println(";}");
      }
    }

    sw.outdent();
    sw.outdent();
    sw.println("}");
  }

  private void writeNewViolations(SourceWriter sw) {
    // Set<ConstraintViolation<T>> violations = new
    // HashSet<ConstraintViolation<T>>();
    sw.println("Set<ConstraintViolation<T>> violations = new HashSet<ConstraintViolation<T>>();");
  }

  /**
   * @param sw
   */
  private void writePropertyValidtors(SourceWriter sw) {
    for (PropertyDescriptor p : beanHelper.getBeanDescriptor().getConstrainedProperties()) {
      writeValidatePropertyMethod(sw, p);
      sw.println();
    }
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

    if (beanHelper.getBeanDescriptor().isBeanConstrained()) {

      // /// For each group

      // TODO(nchalko) handle the sequence in the AbstractValidator
      // Let the GwtSpecificValidators only take a single group.

      // See JSR 303 section 3.5
      // all reachable fields
      // all reachable getters (both) at once

      Set<PropertyDescriptor> properties = beanHelper.getBeanDescriptor().getConstrainedProperties();

      for (PropertyDescriptor p : properties) {
        writeValidatePropertyCall(sw, p);
      }

      // all class level constraints
      // including super classes
      // including super interfaces

      int count = 0;
      Class<?> clazz = null;
      try {
        clazz = beanHelper.getClazz();
      } catch (ClassNotFoundException e) {
        error(logger, e);
      }
      for (ConstraintDescriptor<?> constraint : beanHelper.getBeanDescriptor().findConstraints().getConstraintDescriptors()) {
        count++; // index starts at 1
        Class<? extends ConstraintValidator<? extends Annotation, ?>> validatorClass = getValidatorForType(
            constraint, clazz);
        // TODO(nchalko) handle constraint.isReportAsSingleViolation() and
        // hasComposingConstraints

        // validate(context, violations, object, value, validator,
        // constraintDescriptor, groups);
        sw.print("validate(context, violations, null, object, ");
        // new MyValidtor();
        sw.print("new ");
        sw.print(validatorClass.getCanonicalName());
        sw.print("(), "); // new one each time because validators are not thread
                          // safe
        sw.print(constraintDescriptorVar("this", count));
        sw.println(", groups);");
      }

      // validate all super classes and interfaces

      Class<?> superClass = clazz.getSuperclass();
      writeValidateInterfaces(sw, clazz);
      while (superClass != null) {
        if (serverSideValidator.getConstraintsForClass(superClass).isBeanConstrained()) {
          // MySuper_GwtSpecificValidator validator =
          // GWT.create(MySuper_GwtSpecificValidator);
          sw.print("//  GWT.create(");
          sw.print(superClass.getCanonicalName());
          sw.println("_GwtSpecificValidator);");
        }
        writeValidateInterfaces(sw, superClass);
        superClass = superClass.getSuperclass();
      }

      // all reachable and cascadable associations

      // TODO(nchalko) validationg the object graph will require top level
      // interfaces for the classes
      // that already generated

      beanType.getFields();
    }
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

    // TODO(nchalko) check if it is a valid propertyName

    for (PropertyDescriptor property : beanHelper.getBeanDescriptor().getConstrainedProperties()) {
      // if (propertyName.equals(myPropety)) {
      sw.print("if (propertyName.equals(\"");
      sw.print(property.getPropertyName());
      sw.println("\")) {");
      sw.indent();

      // validate_myProperty
      writeValidatePropertyCall(sw, property);

      // }
      sw.outdent();
      sw.print("} else ");
    }

    // {
    sw.println("{");
    sw.indent();

    // throw new IllegalArgumentException(propertyName
    // +"is not a valid property of myClass");
    sw.print("throw new ");
    sw.print(IllegalArgumentException.class.getCanonicalName());
    sw.print("( propertyName +\" is not a valid property of ");
    sw.print(beanType.getQualifiedSourceName());
    sw.println("\");");

    // }
    sw.outdent();
    sw.println("}");

    // return violations;
    sw.println("return violations;");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidatePropertyCall(SourceWriter sw, PropertyDescriptor p) {
    String propertyName = p.getPropertyName();
    // validateProperty_<<field>>(context, object, object.getLastName(),
    // groups));
    sw.print(validateMethodName(p));
    sw.print("(context, ");
    sw.print("violations, ");
    sw.print("object, ");
    sw.print("object.");
    if (hasGetter(p)) {
      sw.print(asGetter(p) + "()");
    } else {
      sw.print(propertyName);
    }
    sw.print(", ");
    sw.println("groups);");
  }

  private void writeValidatePropertyMethod(SourceWriter sw, PropertyDescriptor p) {
    // private final <T> void validateProperty_<p>(
    sw.print("private final <T> void ");
    sw.print(validateMethodName(p));
    sw.println("(");

    // GwtValidationContext<T> context, Set<ConstraintViolation<T>> violations,
    // BeanType object, <Type> value, Class<?>... groups) {
    sw.indent();
    sw.indent();
    sw.println("GwtValidationContext<T> context,");
    sw.println("Set<ConstraintViolation<T>> violations,");
    sw.println(beanHelper.getTypeCanonicalName() + " object,");
    sw.print(p.getElementClass().getCanonicalName());
    sw.println(" value,");
    sw.println("Class<?>... groups) {");
    sw.outdent();
    int count = 0;
    for (ConstraintDescriptor<?> constraint : p.getConstraintDescriptors()) {
      count++; // index starts at 1
      Class<? extends ConstraintValidator<? extends Annotation, ?>> validatorClass = getValidatorForType(
          constraint, p.getElementClass());
      // TODO(nchalko) handle constraint.isReportAsSingleViolation() and
      // hasComposingConstraints

      // validate(context, violations, object, value, validator,
      // constraintDescriptor, groups);
      sw.print("validate(context, violations, object, value, ");
      // new MyValidtor();
      sw.print("new ");
      sw.print(validatorClass.getCanonicalName());
      sw.print("(), "); // new one each time because validators are not thread
                        // safe
      sw.print(constraintDescriptorVar(p.getPropertyName(), count));
      sw.println(", groups);");
    }
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

    // TODO(nchalko) check if it is a valid propertyName

    for (PropertyDescriptor property : beanHelper.getBeanDescriptor().getConstrainedProperties()) {
      // if (propertyName.equals(myPropety)) {
      sw.print("if (propertyName.equals(\"");
      sw.print(property.getPropertyName());
      sw.println("\")) {");
      sw.indent();

      // validateProperty_<<field>>(context, (MyType) null,
      // object.getLastName(),
      // groups));
      sw.print(validateMethodName(property));
      sw.print("(context, ");
      sw.print("violations, ");
      sw.print("null, ");
      sw.print("(");
      sw.print(property.getElementClass().getCanonicalName());
      sw.print(") value, ");
      sw.println("groups);");

      // }
      sw.outdent();
      sw.print("} else ");
    }

    // {
    sw.println("{");
    sw.indent();

    // throw new IllegalArgumentException(propertyName
    // +"is not a valid property of myClass");
    sw.print("throw new ");
    sw.print(IllegalArgumentException.class.getCanonicalName());
    sw.print("( propertyName +\" is not a valid property of ");
    sw.print(beanType.getQualifiedSourceName());
    sw.println("\");");

    // }
    sw.outdent();
    sw.println("}");

    // return violations;
    sw.println("return violations;");

    sw.outdent();
    sw.println("}");
  }
}
