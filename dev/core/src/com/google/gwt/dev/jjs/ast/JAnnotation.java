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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.collect.Lists;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Represents a java annotation.
 */
public class JAnnotation extends JNode implements JAnnotationArgument {
  /**
   * Represents a value contained within an annotation. Single-valued and
   * array-valued properties are both represented by this type.
   * 
   * @param <T> the type of JAnnotationValue node that the Property
   *          encapsulates.
   * @see {@link #of}
   */
  public static class Property extends JNode {
    private final String name;
    private List<JAnnotationArgument> values;

    public Property(SourceInfo sourceInfo, String name,
        List<JAnnotationArgument> values) {
      super(sourceInfo);
      this.name = name;
      this.values = Lists.normalize(values);
    }

    public Property(SourceInfo sourceInfo, String name,
        JAnnotationArgument value) {
      this(sourceInfo, name, Lists.create(value));
    }

    public void addValue(JAnnotationArgument value) {
      values = Lists.add(values, value);
    }

    public String getName() {
      return name;
    }

    public JAnnotationArgument getSingleValue() {
      if (values.size() != 1) {
        throw new IllegalStateException(
            "Expecting single-valued property, found " + values.size()
                + " values");
      }
      return values.get(0);
    }

    public List<JAnnotationArgument> getValues() {
      return Lists.normalizeUnmodifiable(values);
    }

    public List<JNode> getValuesAsNodes() {
      // Lists.normalizeUnmodifiable would have allocated a new list anyway
      List<JNode> toReturn = Lists.create();
      for (JAnnotationArgument value : values) {
        toReturn = Lists.add(toReturn, value.annotationNode());
      }
      return toReturn;
    }

    @SuppressWarnings("unchecked")
    public void traverse(JVisitor visitor, Context ctx) {
      if (visitor.visit(this, ctx)) {
        // This is a shady cast to a raw list
        List nodes = visitor.acceptImmutable((List) values);

        // JNode and JAnnotationArgument types have disjoint hierarchies
        if (JAnnotation.class.desiredAssertionStatus()) {
          for (int i = 0, j = nodes.size(); i < j; i++) {
            assert nodes.get(i) instanceof JAnnotationArgument : "Expecting a "
                + "JAnnotationArgument at index " + i + " found a "
                + nodes.get(i).getClass().getCanonicalName();
          }
        }

        // This is a shady assignment
        values = nodes;
      }
      visitor.endVisit(this, ctx);
    }
  }

  /**
   * This runtime exception is thrown when calling a Class-valued annotation
   * method when the referenced class is not available to the JVM.
   */
  public static class SourceOnlyClassException extends RuntimeException {
    private final JClassLiteral literal;

    public SourceOnlyClassException(JClassLiteral literal) {
      super("The type " + literal.getRefType().getName()
          + " is available only in the module source");
      this.literal = literal;
    }

    public JClassLiteral getLiteral() {
      return literal;
    }
  }

  /**
   * This handles reflective dispatch for Proxy instances onto the data stored
   * in the AST.
   * 
   * @param <T> the type of Annotation
   */
  private static class AnnotationInvocationHandler<T> implements
      InvocationHandler {
    private final Class<T> clazz;
    private final JAnnotation annotation;

    private AnnotationInvocationHandler(Class<T> clazz, JAnnotation annotation) {
      this.clazz = clazz;
      this.annotation = annotation;
    }

    /**
     * Handles method invocations.
     */
    public Object invoke(Object instance, Method method, Object[] arguments)
        throws Throwable {

      // Use the proxy object to handle trivial stuff
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(this, arguments);
      }

      Property prop = annotation.getProperty(method.getName());
      if (prop == null) {
        // This works because we're working with real Methods
        return method.getDefaultValue();
      }

      if (method.getReturnType().isArray()) {
        List<JAnnotationArgument> values = prop.getValues();
        Object toReturn = Array.newInstance(
            method.getReturnType().getComponentType(), values.size());
        for (int i = 0, j = values.size(); i < j; i++) {
          Object value = evaluate(values.get(i));
          Array.set(toReturn, i, value);
        }
        return toReturn;
      }

      return evaluate(prop.getSingleValue());
    }

    /**
     * Convert a JLiteral value into an Object the caller can work with.
     */
    private Object evaluate(JAnnotationArgument value)
        throws ClassNotFoundException {

      if (value instanceof JValueLiteral) {
        // Primitives
        return ((JValueLiteral) value).getValueObj();

      } else if (value instanceof JClassLiteral) {
        String clazzName = ((JClassLiteral) value).getRefType().getName();
        try {
          return Class.forName(clazzName, true, clazz.getClassLoader());
        } catch (ClassNotFoundException e) {
          throw new SourceOnlyClassException((JClassLiteral) value);
        }

      } else if (value instanceof JAnnotation) {
        // Determine the synthetic annotation's type
        String clazzName = ((JAnnotation) value).getType().getName();
        // Load the annotation class
        Class<? extends Annotation> annotationType = Class.forName(clazzName,
            true, clazz.getClassLoader()).asSubclass(Annotation.class);
        // Creating the backing annotation
        return createAnnotation(annotationType, (JAnnotation) value);
      }

      // Unhandled type
      throw new RuntimeException("Cannot convert "
          + value.getClass().getCanonicalName() + " into an Object");
    }
  }

  /**
   * Create a synthetic Annotation instance, based on the data in a JAnnatation.
   * 
   * @param clazz the type of Annotation
   * @param annotation the backing data
   * 
   * @param <T> the type of Annotation
   * @return an instance of <code>clazz</code>
   */
  public static <T extends Annotation> T createAnnotation(Class<T> clazz,
      JAnnotation annotation) {
    // Create the annotation as a reflective Proxy instance
    Object o = Proxy.newProxyInstance(clazz.getClassLoader(),
        new Class<?>[] {clazz}, new AnnotationInvocationHandler<T>(clazz,
            annotation));

    return clazz.cast(o);
  }

  /**
   * A utility method to retrieve an annotation of the named type. This method
   * will not search supertypes or superinterfaces if <code>x</code> is a
   * JDeclaredType.
   * 
   * @return the annotation of the requested type, or <code>null</code>
   */
  public static JAnnotation findAnnotation(HasAnnotations x,
      String annotationTypeName) {
    for (JAnnotation a : x.getAnnotations()) {
      if (a.getType().getName().equals(annotationTypeName)) {
        return a;
      }
    }
    return null;
  }

  private final JType type;
  private List<Property> properties = Lists.create();

  public JAnnotation(SourceInfo sourceInfo, JExternalType type) {
    super(sourceInfo);
    this.type = type;
  }

  public JAnnotation(SourceInfo sourceInfo, JInterfaceType type) {
    super(sourceInfo);
    this.type = type;
  }

  public void addValue(Property value) {
    properties = Lists.add(properties, value);
  }

  public JNode annotationNode() {
    return this;
  }

  public List<Property> getProperties() {
    return Lists.normalizeUnmodifiable(properties);
  }

  /**
   * Returns the named property or <code>null</code> if it does not exist.
   */
  public Property getProperty(String name) {
    for (Property p : properties) {
      if (p.getName().equals(name)) {
        return p;
      }
    }
    return null;
  }

  public JType getType() {
    return type;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      properties = visitor.acceptImmutable(properties);
    }
    visitor.endVisit(this, ctx);
  }
}
