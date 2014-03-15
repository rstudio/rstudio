/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.HasTypeParameters;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates stub adapters for GWT reflection using EasyMock.
 * This takes in a real java reflection class and returns a JClassType which
 * forwards all its method calls to the equivalent java reflection methods.
 * <p>
 * Most reflections are done lazily (only when the method is actually called),
 * specially those which potentially require mocking a new class / method /
 * field / parameter / constructor / etc.
 * <p>
 * The support for the typeinfo API is still incomplete, and in fact will
 * always be in some way since Java doesn't support all the reflections that
 * GWT's typeinfo does.
 * <p>
 * TODO: With a bit of generalization, this class should make its way
 * to core/src/com/google/gwt/junit
 *
 * To make it public we need to...
 * <ol>
 * <li>implement the missing parts and TODOs (e.g. generics)
 * <li>add tests to it (even though it's a testing utility, it does need
 * tests, specially for cases like inner and anonymous classes, generic
 * parameters, etc.)
 * <li>decide what to do with the parts of JType reflection that java doesn't
 * have an equivalent for - e.g. parameter names. This may involve making a
 * slightly more complex API to inject those values.
 * </ol>
 */
public class JClassTypeAdapter {

  private final Map<Class<?>, JClassType> adaptedClasses =
    new HashMap<Class<?>, JClassType>();
  private final List<Object> allMocks = new ArrayList<Object>();

  public void verifyAll() {
    EasyMock.verify(allMocks.toArray());
  }

  /**
   * Creates a mock GWT class type for the given Java class.
   *
   * @param clazz the java class
   * @return the gwt class
   */
  public JClassType adaptJavaClass(final Class<?> clazz) {
    if (clazz.isPrimitive()) {
      throw new RuntimeException(
          "Only classes can be passed to adaptJavaClass");
    }

    // First try the cache (also avoids infinite recursion if a type references
    // itself).
    JClassType type = adaptedClasses.get(clazz);
    if (type != null) {
      return type;
    }

    // Create and put in the cache
    type = createMock(JClassType.class);
    final JClassType finalType = type;
    adaptedClasses.put(clazz, type);

    // Adds behaviour for annotations and generics
    addAnnotationBehaviour(clazz, type);

    // TODO(rdamazio): Add generics behaviour

    // Add behaviour for getting methods
    expect(type.getMethods()).andStubAnswer(new IAnswer<JMethod[]>() {
      public JMethod[] answer() throws Throwable {
        // TODO(rdamazio): Check behaviour for parent methods
        Method[] realMethods = clazz.getDeclaredMethods();
        JMethod[] methods = new JMethod[realMethods.length];
        for (int i = 0; i < realMethods.length; i++) {
          methods[i] = adaptMethod(realMethods[i], finalType);
        }
        return methods;
      }
    });

    // Add behaviour for getting constructors
    expect(type.getConstructors()).andStubAnswer(new IAnswer<JConstructor[]>() {
      public JConstructor[] answer() throws Throwable {
        Constructor<?>[] realConstructors = clazz.getDeclaredConstructors();
        JConstructor[] constructors = new JConstructor[realConstructors.length];
        for (int i = 0; i < realConstructors.length; i++) {
          constructors[i] = adaptConstructor(realConstructors[i], finalType);
        }
        return constructors;
      }
    });

    // Add behaviour for getting fields
    expect(type.getFields()).andStubAnswer(new IAnswer<JField[]>() {
      public JField[] answer() throws Throwable {
        Field[] realFields = clazz.getDeclaredFields();
        JField[] fields = new JField[realFields.length];
        for (int i = 0; i < realFields.length; i++) {
          fields[i] = adaptField(realFields[i], finalType);
        }
        return fields;
      }
    });

    // Add behaviour for getting names
    expect(type.getName()).andStubReturn(clazz.getName());
    expect(type.getQualifiedSourceName()).andStubReturn(
        clazz.getCanonicalName());
    expect(type.getSimpleSourceName()).andStubReturn(clazz.getSimpleName());

    // Add modifier behaviour
    int modifiers = clazz.getModifiers();
    expect(type.isAbstract()).andStubReturn(Modifier.isAbstract(modifiers));
    expect(type.isFinal()).andStubReturn(Modifier.isFinal(modifiers));
    expect(type.isPublic()).andStubReturn(Modifier.isPublic(modifiers));
    expect(type.isProtected()).andStubReturn(Modifier.isProtected(modifiers));
    expect(type.isPrivate()).andStubReturn(Modifier.isPrivate(modifiers));

    // Add conversion behaviours
    expect(type.isArray()).andStubReturn(null);
    expect(type.isEnum()).andStubReturn(null);
    expect(type.isPrimitive()).andStubReturn(null);
    expect(type.isClassOrInterface()).andStubReturn(type);
    if (clazz.isInterface()) {
      expect(type.isClass()).andStubReturn(null);
      expect(type.isInterface()).andStubReturn(type);
    } else {
      expect(type.isClass()).andStubReturn(type);
      expect(type.isInterface()).andStubReturn(null);
    }
    expect(type.getEnclosingType()).andStubAnswer(new IAnswer<JClassType>() {
      public JClassType answer() throws Throwable {
        Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass == null) {
          return null;
        }

        return adaptJavaClass(enclosingClass);
      }
    });
    expect(type.getSuperclass()).andStubAnswer(new IAnswer<JClassType>() {
      public JClassType answer() throws Throwable {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null) {
          return null;
        }

        return adaptJavaClass(superclass);
      }
    });

    // TODO(rdamazio): Mock out other methods as needed
    // TODO(rdamazio): Figure out what to do with reflections that GWT allows
    //                 but Java doesn't

    EasyMock.replay(type);
    return type;
  }

  /**
   * Creates a mock GWT field for the given Java field.
   *
   * @param realField the java field
   * @param enclosingType the GWT enclosing type
   * @return the GWT field
   */
  public JField adaptField(final Field realField, JClassType enclosingType) {
    JField field = createMock(JField.class);

    addAnnotationBehaviour(realField, field);

    expect(field.getType()).andStubAnswer(new IAnswer<JType>() {
      public JType answer() throws Throwable {
        return adaptType(realField.getType());
      }
    });

    expect(field.getEnclosingType()).andStubReturn(enclosingType);
    expect(field.getName()).andStubReturn(realField.getName());

    EasyMock.replay(field);
    return field;
  }

  /**
   * Creates a mock GWT constructor for the given java constructor.
   *
   * @param realConstructor the java constructor
   * @param enclosingType the type to which the constructor belongs
   * @return the GWT constructor
   */
  private JConstructor adaptConstructor(final Constructor<?> realConstructor,
      JClassType enclosingType) {
    final JConstructor constructor = createMock(JConstructor.class);

    addCommonAbstractMethodBehaviour(realConstructor, constructor,
        enclosingType);
    addAnnotationBehaviour(realConstructor, constructor);

    // Parameters
    expect(constructor.getParameters()).andStubAnswer(
        new IAnswer<JParameter[]>() {
          public JParameter[] answer() throws Throwable {
            return adaptParameters(realConstructor.getParameterTypes(),
                realConstructor.getParameterAnnotations(), constructor);
          }
        });

    // Thrown exceptions
    expect(constructor.getThrows()).andStubAnswer(
        new IAnswer<JClassType[]>() {
          public JClassType[] answer() throws Throwable {
            Class<?>[] realThrows = realConstructor.getExceptionTypes();
            JClassType[] gwtThrows = new JClassType[realThrows.length];
            for (int i = 0; i < realThrows.length; i++) {
              gwtThrows[i] = (JClassType) adaptType(realThrows[i]);
            }
            return gwtThrows;
          }
        });

    EasyMock.replay(constructor);
    return constructor;
  }

  /**
   * Creates a mock GWT method for the given java method.
   *
   * @param realMethod the java method
   * @param enclosingType the type to which the method belongs
   * @return the GWT method
   */
  private JMethod adaptMethod(final Method realMethod,
      JClassType enclosingType) {
    // TODO(rdamazio): ensure a single instance per method per class
    final JMethod method = createMock(JMethod.class);

    addCommonAbstractMethodBehaviour(realMethod, method, enclosingType);
    addAnnotationBehaviour(realMethod, method);
    addGenericsBehaviour(realMethod, method);

    expect(method.isStatic()).andStubReturn(
        Modifier.isStatic(realMethod.getModifiers()));

    // Return type
    expect(method.getReturnType()).andStubAnswer(new IAnswer<JType>() {
      public JType answer() throws Throwable {
        return adaptType(realMethod.getReturnType());
      }
    });

    // Parameters
    expect(method.getParameters()).andStubAnswer(new IAnswer<JParameter[]>() {
      public JParameter[] answer() throws Throwable {
        return adaptParameters(realMethod.getParameterTypes(),
            realMethod.getParameterAnnotations(), method);
      }
    });

    // Thrown exceptions
    expect(method.getThrows()).andStubAnswer(new IAnswer<JClassType[]>() {
      public JClassType[] answer() throws Throwable {
        Class<?>[] realThrows = realMethod.getExceptionTypes();
        JClassType[] gwtThrows = new JClassType[realThrows.length];
        for (int i = 0; i < realThrows.length; i++) {
          gwtThrows[i] = (JClassType) adaptType(realThrows[i]);
        }
        return gwtThrows;
      }
    });

    EasyMock.replay(method);
    return method;
  }

  /**
   * Creates an array of mock GWT parameters for the given array of java
   * parameters.
   *
   * @param parameterTypes the types of the parameters
   * @param parameterAnnotations the list of annotations for each parameter
   * @param method the method or constructor to which the parameters belong
   * @return an array of GWT parameters
   */
  @SuppressWarnings("unchecked")
  protected JParameter[] adaptParameters(Class<?>[] parameterTypes,
      Annotation[][] parameterAnnotations, JAbstractMethod method) {
    JParameter[] parameters = new JParameter[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      final Class<?> realParameterType = parameterTypes[i];
      JParameter parameter = createMock(JParameter.class);
      parameters[i] = parameter;

      // TODO(rdamazio): getName() has no plain java equivalent.
      //                 Perhaps compiling with -g:vars ?

      expect(parameter.getEnclosingMethod()).andStubReturn(method);
      expect(parameter.getType()).andStubAnswer(new IAnswer<JType>() {
        public JType answer() throws Throwable {
          return adaptType(realParameterType);
        }
      });

      // Add annotation behaviour
      final Annotation[] annotations = parameterAnnotations[i];

      expect(parameter.isAnnotationPresent(isA(Class.class))).andStubAnswer(
          new IAnswer<Boolean>() {
            public Boolean answer() throws Throwable {
              Class<? extends Annotation> annotationClass =
                  (Class<? extends Annotation>)
                  EasyMock.getCurrentArguments()[0];
              for (Annotation annotation : annotations) {
                if (annotation.equals(annotationClass)) {
                  return true;
                }
              }
              return false;
            }
          });

      expect(parameter.getAnnotation(isA(Class.class))).andStubAnswer(
          new IAnswer<Annotation>() {
            public Annotation answer() throws Throwable {
              Class<? extends Annotation> annotationClass =
                  (Class<? extends Annotation>)
                  EasyMock.getCurrentArguments()[0];
              for (Annotation annotation : annotations) {
                if (annotation.equals(annotationClass)) {
                  return annotation;
                }
              }
              return null;
            }
          });

      EasyMock.replay(parameter);
    }

    return parameters;
  }

  /**
   * Creates a GWT mock type for the given java type.
   * The type can be a class or a primitive type.
   *
   * @param type the java type
   * @return the GWT type
   */
  private JType adaptType(Class<?> type) {
    if (!type.isPrimitive()) {
      return adaptJavaClass(type);
    } else {
      return adaptPrimitiveType(type);
    }
  }

  /**
   * Returns the GWT primitive type for the given java primitive type.
   *
   * @param type the java primitive type
   * @return the GWT primitive equivalent
   */
  private JType adaptPrimitiveType(Class<?> type) {
    if (boolean.class.equals(type)) { return JPrimitiveType.BOOLEAN; }
    if (int.class.equals(type)) { return JPrimitiveType.INT; }
    if (char.class.equals(type)) { return JPrimitiveType.CHAR; }
    if (byte.class.equals(type)) { return JPrimitiveType.BYTE; }
    if (long.class.equals(type)) { return JPrimitiveType.LONG; }
    if (short.class.equals(type)) { return JPrimitiveType.SHORT; }
    if (float.class.equals(type)) { return JPrimitiveType.FLOAT; }
    if (double.class.equals(type)) { return JPrimitiveType.DOUBLE; }
    if (void.class.equals(type)) { return JPrimitiveType.VOID; }

    throw new IllegalArgumentException(
        "Invalid primitive type: " + type.getName());
  }

  /**
   * Adds expectations common to all method types (methods and constructors).
   *
   * @param realMember the java method
   * @param member the mock GWT method
   * @param enclosingType the type to which the method belongs
   */
  private void addCommonAbstractMethodBehaviour(Member realMember,
      JAbstractMethod member, JClassType enclosingType) {
    // Attributes
    int modifiers = realMember.getModifiers();
    expect(member.isPublic()).andStubReturn(Modifier.isPublic(modifiers));
    expect(member.isProtected()).andStubReturn(Modifier.isProtected(modifiers));
    expect(member.isPrivate()).andStubReturn(Modifier.isPrivate(modifiers));
    expect(member.getName()).andStubReturn(realMember.getName());
    expect(member.getEnclosingType()).andStubReturn(enclosingType);
  }

  /**
   * Adds expectations for getting annotations from elements (methods, classes,
   * parameters, etc.).
   *
   * @param realElement the java element which contains annotations
   * @param element the mock GWT element which contains annotations
   */
  @SuppressWarnings("unchecked")
  private void addAnnotationBehaviour(final AnnotatedElement realElement,
      final HasAnnotations element) {
    expect(element.isAnnotationPresent(isA(Class.class))).andStubAnswer(
        new IAnswer<Boolean>() {
          public Boolean answer() throws Throwable {
            Class<? extends Annotation> annotationClass =
                (Class<? extends Annotation>) EasyMock.getCurrentArguments()[0];
            return realElement.isAnnotationPresent(annotationClass);
          }
        });

    expect(element.getAnnotation(isA(Class.class))).andStubAnswer(
        new IAnswer<Annotation>() {
          public Annotation answer() throws Throwable {
            Class<? extends Annotation> annotationClass =
                (Class<? extends Annotation>) EasyMock.getCurrentArguments()[0];
            return realElement.getAnnotation(annotationClass);
          }
        });
  }

  /**
   * Adds expectations for getting generics types.
   *
   * @param realGeneric the java generic declaration
   * @param generic the mock GWT generic declaration
   */
  private void addGenericsBehaviour(final GenericDeclaration realGeneric,
      final HasTypeParameters generic) {
    // TODO(rdamazio): Implement when necessary
  }

  /**
   * Creates a mock of the given class and adds it to the {@link #allMocks}
   * member list.
   *
   * @param <T> the type of the mock
   * @param clazz the class of the mock
   * @return the mock
   */
  private <T> T createMock(Class<T> clazz) {
    T mock = EasyMock.createMock(clazz);
    allMocks.add(mock);
    return mock;
  }
}
