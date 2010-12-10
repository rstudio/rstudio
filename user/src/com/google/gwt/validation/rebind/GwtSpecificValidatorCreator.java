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
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.primitives.Primitives;
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
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 * Creates a {@link com.google.gwt.validation.client.GwtSpecificValidator}.
 * <p>
 * This class is not thread safe.
 */
public class GwtSpecificValidatorCreator extends AbstractCreator {

  private static enum Stage {
    OBJECT, PROPERTY, VALUE
  }

  private static final JType[] NO_ARGS = new JType[]{};

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
  private Set<BeanHelper> beansToValidate = Sets.newHashSet();
  private final JClassType beanType;

  private final Set<JField> fieldsToWrap = Sets.newHashSet();
  private Set<JMethod> gettersToWrap = Sets.newHashSet();
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
    addImports(composerFactory, GWT.class, GwtBeanDescriptor.class,
        GwtValidationContext.class, Set.class, HashSet.class,
        ConstraintViolation.class, Annotation.class);
    composerFactory.setSuperclass(AbstractGwtSpecificValidator.class.getCanonicalName()
        + "<" + beanType.getQualifiedSourceName() + ">");
    composerFactory.addImplementedInterface(validatorType.getName());
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
    writePropertyValidators(sw);
    sw.println();

    // Write these after we know what is needed
    writeWrappers(sw);
    sw.println();

    // Write the Validator instance variables after we have collected the
    // ones we need in beansToValidate
    writeValidatorInstances(sw);
  }

  protected void writeUnsafeNativeLongIfNeeded(SourceWriter sw, JType jType) {
    if (JPrimitiveType.LONG.equals(jType)) {
      // @com.google.gwt.core.client.UnsafeNativeLong
      sw.print("@");
      sw.println(UnsafeNativeLong.class.getCanonicalName());
    }
  }

  private <T> T[] asArray(Collection<?> collection, T[] array) {
    if (collection == null) {
      return null;
    }
    return collection.toArray(array);
  }

  private String asGetter(PropertyDescriptor p) {
    return "get" + capitalizeFirstLetter(p.getPropertyName());
  }

  private String capitalizeFirstLetter(String propertyName) {
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

  private String constraintDescriptorVar(String name, int count) {
    String s = name + "_c" + count;
    return s;
  }

  private Annotation getAnnotation(PropertyDescriptor p, boolean useField,
      ConstraintDescriptor<?> constraint) {
    Class<? extends Annotation> expectedAnnotaionClass =
        ((Annotation) constraint.getAnnotation()).annotationType();
    Annotation annotation = null;
    if (useField) {
      JField field = beanType.findField(p.getPropertyName());
      if (field.getEnclosingType().equals(beanType)) {
        annotation = field.getAnnotation(expectedAnnotaionClass);
      }
    } else {
      JMethod method = beanType.findMethod(asGetter(p), NO_ARGS);
      if (method.getEnclosingType().equals(beanType)) {
        annotation = method.getAnnotation(expectedAnnotaionClass);
      }
    }
    return annotation;
  }

  private Class<? extends ConstraintValidator<? extends Annotation, ?>> getValidatorForType(
      ConstraintDescriptor<? extends Annotation> constraint, Class<?> clazz) {
    // TODO(nchalko) implement per spec
    Class<? extends ConstraintValidator<? extends Annotation, ?>> validatorClass =
        constraint.getConstraintValidatorClasses().get(0);
    return validatorClass;
  }

  private boolean hasField(PropertyDescriptor p) {
    JField field = beanType.findField(p.getPropertyName());
    return field != null;
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

  private boolean isPropertyConstrained(BeanHelper helper, PropertyDescriptor p) {
    Set<PropertyDescriptor> propertyDescriptors =
        helper.getBeanDescriptor().getConstrainedProperties();
    Predicate<PropertyDescriptor> nameMatches = newPropertyNameMatches(p);
    return Iterables.any(propertyDescriptors, nameMatches);
  }

  private Predicate<PropertyDescriptor> newPropertyNameMatches(
      final PropertyDescriptor p) {
    return new Predicate<PropertyDescriptor>() {
      public boolean apply(PropertyDescriptor input) {
        return input.getPropertyName().equals(p.getPropertyName());
      }
    };
  }

  private String toWrapperName(JField field) {
    return "_" + field.getName();
  }

  /**
   * @param method
   * @return
   */
  private String toWrapperName(JMethod method) {
    return "_" + method.getName();
  }

  private String validateMethodFieldName(PropertyDescriptor p) {
    return "validateProperty_" + p.getPropertyName();
  }

  private String validateMethodGetterName(PropertyDescriptor p) {
    return "validateProperty_get" + p.getPropertyName();
  }

  private void writeConstraintDescriptor(SourceWriter sw,
      ConstraintDescriptor<? extends Annotation> constraint,
      String constraintDescripotorVar) throws UnableToCompleteException {
    Class<? extends Annotation> annotationType =
        constraint.getAnnotation().annotationType();

    // First list all composing constraints
    int count = 0;
    for (ConstraintDescriptor<?> composingConstraint :
        constraint.getComposingConstraints()) {
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

    for (Map.Entry<String, Object> entry :
        constraint.getAttributes().entrySet()) {
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
    sw.print(Boolean.valueOf(constraint.isReportAsSingleViolation())
        .toString());
    sw.println(")");

    // .build();
    sw.println(".build();");
    sw.outdent();
    sw.outdent();

    sw.outdent();
    sw.outdent();
    sw.println();
  }

  private void writeFields(SourceWriter sw) throws UnableToCompleteException {
    // MyBeanDescriptor beanDescriptor = GWT.create(MyBeanDescriptor);
    sw.println(GwtBeanDescriptor.class.getCanonicalName());
    // TODO(nchalko) implement BeanDescriptor Generator
    sw.println(" beanDescriptor = null; //TODO(nchalko) GWT.create");

    // Create a variable for each constraint of each property
    for (PropertyDescriptor p :
         beanHelper.getBeanDescriptor().getConstrainedProperties()) {
      int count = 0;
      for (ConstraintDescriptor<?> constraint : p.getConstraintDescriptors()) {
        count++; // index starts at one.
        writeConstraintDescriptor(sw, constraint,
            constraintDescriptorVar(p.getPropertyName(), count));
      }
      if (p.isCascaded()) {
        beansToValidate.add(createBeanHelper(p.getElementClass()));
      }
    }

    // Create a variable for each constraint of this class.
    int count = 0;
    for (ConstraintDescriptor<?> constraint :
        beanHelper.getBeanDescriptor().getConstraintDescriptors()) {
      count++; // index starts at one.
      writeConstraintDescriptor(sw, constraint,
          constraintDescriptorVar("this", count));
    }
  }

  private void writeFieldWrapperMethod(SourceWriter sw, JField field) {
    writeUnsafeNativeLongIfNeeded(sw, field.getType());

    // private native fieldType _fieldName(Bean object) /*-{
    sw.print("private native ");

    sw.print(field.getType().getQualifiedSourceName());
    sw.print(" ");
    sw.print(toWrapperName(field));
    sw.print("(");
    sw.print(beanType.getName());
    sw.println(" object) /*-{");
    sw.indent();

    // return object.@com.examples.Bean::myMethod();
    sw.print("return object.@");
    sw.print(field.getEnclosingType().getQualifiedSourceName());
    sw.print("::" + field.getName());
    sw.println(";");

    // }-*/;
    sw.outdent();
    sw.println("}-*/;");
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

  private void writeGetterWrapperMethod(SourceWriter sw, JMethod method) {
    writeUnsafeNativeLongIfNeeded(sw, method.getReturnType());

    // private native fieldType _getter(Bean object) /*={
    sw.print("private native ");
    sw.print(method.getReturnType().getQualifiedSourceName());
    sw.print(" ");
    sw.print(toWrapperName(method));
    sw.print("(");
    sw.print(beanType.getName());
    sw.println(" object) /*-{");
    sw.indent();

    // return object.@com.examples.Bean::myMethod()();
    sw.print("return object.");
    sw.print(method.getJsniSignature());
    sw.println("();");

    // }-*/;
    sw.outdent();
    sw.println("}-*/;");
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
    // Set<ConstraintViolation<T>> violations =
    // new HashSet<ConstraintViolation<T>>();
    sw.println("Set<ConstraintViolation<T>> violations = ");
    sw.indent();
    sw.indent();

    sw.println("new HashSet<ConstraintViolation<T>>();");
    sw.outdent();
    sw.outdent();
  }

  private void writePropertyValidators(SourceWriter sw) {
    for (PropertyDescriptor p :
        beanHelper.getBeanDescriptor().getConstrainedProperties()) {
      if (hasField(p)) {
        writeValidatePropertyMethod(sw, p, true);
        sw.println();
      }
      if (hasGetter(p)) {
        writeValidatePropertyMethod(sw, p, false);
        sw.println();
      }
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

      // See JSR 303 section 3.5
      // all reachable fields
      // all reachable getters (both) at once

      Set<PropertyDescriptor> properties = beanHelper.getBeanDescriptor().getConstrainedProperties();

      for (PropertyDescriptor p : properties) {
        writeValidatePropertyCall(sw, p, false);
      }

      // all class level constraints
      // including super classes
      // including super interfaces

      int count = 0;
      Class<?> clazz = beanHelper.getClazz();
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
      writeValidateInheritance(sw, clazz, Stage.OBJECT, null);

      // all reachable and cascadable associations

      beanType.getFields();
    }
    // return violations;
    sw.println("return violations;");

    sw.outdent();
    sw.println("}");
  }

  private void writeValidateFieldCall(SourceWriter sw, PropertyDescriptor p,
      boolean useValue) {
    String propertyName = p.getPropertyName();

    // validateProperty_<<field>>(context,
    sw.print(validateMethodFieldName(p));
    sw.print("(context, ");
    sw.print("violations, ");

    // null, (MyType) value,
    // or
    // object, object.getLastName(),
    if (useValue) {
      sw.print("null, ");
      sw.print("(");
      sw.print(Primitives.wrap(p.getElementClass()).getCanonicalName());
      sw.print(") value");
    } else {
      sw.print("object, ");
      JField field = beanType.getField(propertyName);
      if (field.isPublic()) {
        sw.print("object.");
        sw.print(propertyName);
      } else {
        fieldsToWrap.add(field);
        sw.print(toWrapperName(field) + "(object)");
      }
    }
    sw.print(", ");

    // groups));
    sw.println("groups);");
  }

  private void writeValidateGetterCall(SourceWriter sw, PropertyDescriptor p,
      boolean useValue) {
    // validateProperty_get<<field>>(context, violations,
    sw.print(validateMethodGetterName(p));
    sw.print("(context, ");
    sw.print("violations, ");

    // object, object.getMyProp(),
    // or
    // null, (MyType) value,
    if (useValue) {
      sw.print("null, ");
      sw.print("(");
      sw.print(Primitives.wrap(p.getElementClass()).getCanonicalName());
      sw.print(") value");
    } else {
      sw.print("object, ");
      JMethod method = beanType.findMethod(asGetter(p), NO_ARGS);
      if (method.isPublic()) {
        sw.print("object.");
        sw.print(asGetter(p));
        sw.print("()");
      } else {
        gettersToWrap.add(method);
        sw.print(toWrapperName(method) + "(object)");
      }
    }
    sw.print(", ");

    // groups);
    sw.println("groups);");
  }

  private void writeValidateInheritance(SourceWriter sw, Class<?> clazz,
      Stage stage, PropertyDescriptor property) {
    writeValidateInterfaces(sw, clazz, stage, property);
    Class<?> superClass = clazz.getSuperclass();
    if (superClass != null) {
      writeValidatorCall(sw, superClass, stage, property);
    }
  }

  private void writeValidateInterfaces(SourceWriter sw, Class<?> clazz,
      Stage stage, PropertyDescriptor p) {
    for (Class<?> type : clazz.getInterfaces()) {
      writeValidatorCall(sw, type, stage, p);
      writeValidateInterfaces(sw, type, stage, p);
    }
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

      writeValidatePropertyCall(sw, property, false);

      // validate all super classes and interfaces
      writeValidateInheritance(sw, beanHelper.getClazz(), Stage.PROPERTY,
          property);

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

    if (!beanHelper.getBeanDescriptor().getConstrainedProperties().isEmpty()) {

      // return violations;
      sw.println("return violations;");
    }

    sw.outdent();
    sw.println("}");
  }

  private void writeValidatePropertyCall(SourceWriter sw,
      PropertyDescriptor property, boolean useValue) {
    if (hasGetter(property)) {
      // validate_getMyProperty
      writeValidateGetterCall(sw, property, useValue);
    }
    if (hasField(property)) {
      // validate_myProperty
      writeValidateFieldCall(sw, property, useValue);
    }
  }

  private void writeValidatePropertyMethod(SourceWriter sw,
      PropertyDescriptor p, boolean useField) {
    // private final <T> void validateProperty_{get}<p>(
    sw.print("private final <T> void ");
    if (useField) {
      sw.print(validateMethodFieldName(p));
    } else {
      sw.print(validateMethodGetterName(p));
    }
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

    // TODO(nchalko) move this out of here to the Validate method
    if (p.isCascaded()) {
      BeanHelper helper = createBeanHelper(p.getElementClass());

      // if(value != null) {
      sw.println("if(value != null) {");
      sw.indent();

      // violations.addAll(myGwtValidator.validate(context, value, groups));
      sw.print("violations.addAll(");
      sw.print(helper.getValidatorInstanceName());
      sw.println(".validate(context, value, groups));");

      // }
      sw.outdent();
      sw.println("}");
    }

    int count = 0;
    for (ConstraintDescriptor<?> constraint : p.getConstraintDescriptors()) {
      count++; // index starts at 1

      Annotation annotation = getAnnotation(p, useField, constraint);
      if (annotation != null) {
        // TODO(nchalko) check for annotation equality

        Class<? extends ConstraintValidator<? extends Annotation, ?>> validatorClass = getValidatorForType(
            constraint, p.getElementClass());
        // TODO(nchalko) handle constraint.isReportAsSingleViolation() and
        // hasComposingConstraints

        // validate(context, violations, object, value, new MyValidator(),
        // constraintDescriptor, groups);
        sw.print("validate(context, violations, object, value, ");
        sw.print("new ");
        sw.print(validatorClass.getCanonicalName());
        sw.print("(), "); // new one each time because validators are not thread
                          // safe
        sw.print(constraintDescriptorVar(p.getPropertyName(), count));
        sw.println(", groups);");
      }
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

    for (PropertyDescriptor property :
        beanHelper.getBeanDescriptor().getConstrainedProperties()) {
      // if (propertyName.equals(myPropety)) {
      sw.print("if (propertyName.equals(\"");
      sw.print(property.getPropertyName());
      sw.println("\")) {");
      sw.indent();

      writeValidatePropertyCall(sw, property, true);

      // validate all super classes and interfaces
      writeValidateInheritance(sw, beanHelper.getClazz(),
          Stage.VALUE, property);

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

    if (!beanHelper.getBeanDescriptor()
        .getConstrainedProperties().isEmpty()) {
      // return violations;
      sw.println("return violations;");
    }

    sw.outdent();
    sw.println("}");
  }

  private void writeValidatorCall(SourceWriter sw, Class<?> type, Stage stage,
      PropertyDescriptor p) {
    if (BeanHelper.isClassConstrained(type)) {
      BeanHelper helper = createBeanHelper(type);
      beansToValidate.add(helper);
      switch (stage) {
        case OBJECT:
          // voilations.addAll(myValidator.validate(context,object,groups));
          sw.print("violations.addAll(");
          sw.print(helper.getValidatorInstanceName());
          sw.println(".validate(context, object, groups));");
          break;
        case PROPERTY:
          if (isPropertyConstrained(helper, p)) {
            // voilations.addAll(myValidator.validateProperty(context,object
            // ,propertyName, groups));
            sw.print("violations.addAll(");
            sw.print(helper.getValidatorInstanceName());
            sw.print(".validateProperty(context, object,");
            sw.println(" propertyName, groups));");
          }
          break;
        case VALUE:
          if (isPropertyConstrained(helper, p)) {
            // voilations.addAll(myValidator.validateProperty(context,beanType
            // ,propertyName, value, groups));
            sw.print("violations.addAll(");
            sw.print(helper.getValidatorInstanceName());
            sw.print(".validateValue(context, ");
            // TODO(nchalko) this seems like an unneeded param
            sw.print(helper.getTypeCanonicalName());
            sw.println(".class, propertyName, value, groups));");
          }
          break;
        default:
          throw new IllegalStateException();
      }
    }
  }

  private void writeValidatorInstances(SourceWriter sw) {
    sw.println("// The validator instance variables are written last ");
    sw.println("// after we have identified all the ones we need.");
    for (BeanHelper helper : beansToValidate) {
      writeValidatorInstance(sw, helper);
    }
  }

  private void writeWrappers(SourceWriter sw) {
    sw.println("// Write the  wrappers after we know which are needed");
    for (JField field : fieldsToWrap) {
      writeFieldWrapperMethod(sw, field);
      sw.println();
    }

    for (JMethod method : gettersToWrap) {
      writeGetterWrapperMethod(sw, method);
      sw.println();
    }
  }
}
