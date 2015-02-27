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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

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

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    when(type.getMethods()).thenAnswer(new Answer<JMethod[]>() {
      @Override
      public JMethod[] answer(InvocationOnMock invocation) throws Throwable {
        Method[] realMethods = clazz.getDeclaredMethods();
        JMethod[] methods = new JMethod[realMethods.length];
        for (int i = 0; i < realMethods.length; i++) {
          methods[i] = adaptMethod(realMethods[i], finalType);
        }
        return methods;
      }
    });

    // Add behaviour for getting constructors
    when(type.getConstructors()).thenAnswer(new Answer<JConstructor[]>() {
      @Override
      public JConstructor[] answer(InvocationOnMock invocation) throws Throwable {
        Constructor<?>[] realConstructors = clazz.getDeclaredConstructors();
        JConstructor[] constructors = new JConstructor[realConstructors.length];
        for (int i = 0; i < realConstructors.length; i++) {
          constructors[i] = adaptConstructor(realConstructors[i], finalType);
        }
        return constructors;
      }
    });

    // Add behaviour for getting fields
    when(type.getFields()).thenAnswer(new Answer<JField[]>() {
      @Override
      public JField[] answer(InvocationOnMock invocation) throws Throwable {
        Field[] realFields = clazz.getDeclaredFields();
        JField[] fields = new JField[realFields.length];
        for (int i = 0; i < realFields.length; i++) {
          fields[i] = adaptField(realFields[i], finalType);
        }
        return fields;
      }
    });

    // Add behaviour for getting names
    when(type.getName()).thenReturn(clazz.getName());
    when(type.getQualifiedSourceName()).thenReturn(
        clazz.getCanonicalName());
    when(type.getSimpleSourceName()).thenReturn(clazz.getSimpleName());

    // Add modifier behaviour
    int modifiers = clazz.getModifiers();
    when(type.isAbstract()).thenReturn(Modifier.isAbstract(modifiers));
    when(type.isFinal()).thenReturn(Modifier.isFinal(modifiers));
    when(type.isPublic()).thenReturn(Modifier.isPublic(modifiers));
    when(type.isProtected()).thenReturn(Modifier.isProtected(modifiers));
    when(type.isPrivate()).thenReturn(Modifier.isPrivate(modifiers));

    // Add conversion behaviours
    when(type.isArray()).thenReturn(null);
    when(type.isEnum()).thenReturn(null);
    when(type.isPrimitive()).thenReturn(null);
    when(type.isClassOrInterface()).thenReturn(type);
    if (clazz.isInterface()) {
      when(type.isClass()).thenReturn(null);
      when(type.isInterface()).thenReturn(type);
    } else {
      when(type.isClass()).thenReturn(type);
      when(type.isInterface()).thenReturn(null);
    }
    when(type.getEnclosingType()).thenAnswer(new Answer<JClassType>() {
      @Override
      public JClassType answer(InvocationOnMock invocation) throws Throwable {
        Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass == null) {
          return null;
        }

        return adaptJavaClass(enclosingClass);
      }
    });
    when(type.getSuperclass()).thenAnswer(new Answer<JClassType>() {
      @Override
      public JClassType answer(InvocationOnMock invocation) throws Throwable {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null) {
          return null;
        }

        return adaptJavaClass(superclass);
      }
    });
    when(type.getImplementedInterfaces()).thenAnswer(new Answer<JClassType[]>() {
      @Override
      public JClassType[] answer(InvocationOnMock invocation) throws Throwable {
        Class<?>[] interfaces = clazz.getInterfaces();
        if ((interfaces == null) || (interfaces.length == 0)) {
          return null;
        }

        JClassType[] adaptedInterfaces = new JClassType[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
          adaptedInterfaces[i] = adaptJavaClass(interfaces[i]);
        }
        return adaptedInterfaces;
      }
    });
    when(type.getFlattenedSupertypeHierarchy()).thenAnswer(new Answer<Set<JClassType>>() {
      @Override
      public Set<JClassType> answer(InvocationOnMock invocation) throws Throwable {
        return flatten(clazz);
      }

      private Set<JClassType> flatten(Class<?> clazz) {
        Set<JClassType> flattened = new LinkedHashSet<JClassType>();
        flattened.add(adaptJavaClass(clazz));

        for (Class<?> intf : clazz.getInterfaces()) {
          flattened.addAll(flatten(intf));
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
          flattened.addAll(flatten(superClass));
        }

        return flattened;
      }
    });
    when(type.getInheritableMethods()).thenAnswer(new Answer<JMethod[]>() {
      @Override
      public JMethod[] answer(InvocationOnMock invocation) throws Throwable {
        Map<String, Method> methodsBySignature = new TreeMap<String, Method>();
        getInheritableMethodsOnSuperinterfacesAndMaybeThisInterface(clazz, methodsBySignature);
        if (!clazz.isInterface()) {
          getInheritableMethodsOnSuperclassesAndThisClass(clazz, methodsBySignature);
        }
        int size = methodsBySignature.size();
        if (size == 0) {
          return new JMethod[0];
        } else {
          Iterator<Method> leafMethods = methodsBySignature.values().iterator();
          JMethod[] jMethods = new JMethod[size];
          for (int i = 0; i < size; i++) {
            Method method = leafMethods.next();
            jMethods[i] = adaptMethod(method, adaptJavaClass(method.getDeclaringClass()));
          }
          return jMethods;
        }
      }

      protected void getInheritableMethodsOnSuperinterfacesAndMaybeThisInterface(
          Class<?> clazz,
          Map<String, Method> methodsBySignature) {

        // Recurse first so that more derived methods will clobber less derived
        // methods.
        Class<?>[] superIntfs = clazz.getInterfaces();
        for (Class<?> superIntf : superIntfs) {
          getInheritableMethodsOnSuperinterfacesAndMaybeThisInterface(
              superIntf,
              methodsBySignature);
        }

        Method[] declaredMethods = clazz.getMethods();
        for (Method method : declaredMethods) {
          String sig = computeInternalSignature(method);
          Method existing = methodsBySignature.get(sig);
          if (existing != null) {
            Class<?> existingType = existing.getDeclaringClass();
            Class<?> thisType = method.getDeclaringClass();
            if (thisType.isAssignableFrom(existingType)) {
              // The existing method is in a more-derived type, so don't replace it.
              continue;
            }
          }
          methodsBySignature.put(sig, method);
        }
      }

      protected void getInheritableMethodsOnSuperclassesAndThisClass(
          Class<?> clazz,
          Map<String, Method> methodsBySignature) {

        // Recurse first so that more derived methods will clobber less derived
        // methods.
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
          getInheritableMethodsOnSuperclassesAndThisClass(
              superClass,
              methodsBySignature);
        }

        Method[] declaredMethods = clazz.getMethods();
        for (Method method : declaredMethods) {
          // Ensure that this method is inheritable.
          if (Modifier.isPrivate(method.getModifiers())
              || Modifier.isStatic(method.getModifiers())) {
            // We cannot inherit this method, so skip it.
            continue;
          }

          // We can override this method, so record it.
          String sig = computeInternalSignature(method);
          methodsBySignature.put(sig, method);
        }
      }

      private String computeInternalSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append(method.getName());
        Class<?>[] params = method.getParameterTypes();
        for (Class<?> param : params) {
          sb.append("/");
          sb.append(param.getName());
        }
        return sb.toString();
      }
    });

    // TODO(rdamazio): Mock out other methods as needed
    // TODO(rdamazio): Figure out what to do with reflections that GWT allows
    //                 but Java doesn't

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

    when(field.getType()).thenAnswer(new Answer<JType>() {
      @Override
      public JType answer(InvocationOnMock invocation) throws Throwable {
        return adaptType(realField.getType());
      }
    });

    when(field.getEnclosingType()).thenReturn(enclosingType);
    when(field.getName()).thenReturn(realField.getName());

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
    when(constructor.getParameters()).thenAnswer(
        new Answer<JParameter[]>() {
          @Override
          public JParameter[] answer(InvocationOnMock invocation) throws Throwable {
            return adaptParameters(realConstructor.getParameterTypes(),
                realConstructor.getParameterAnnotations(), constructor);
          }
        });

    // Thrown exceptions
    when(constructor.getThrows()).thenAnswer(
        new Answer<JClassType[]>() {
          @Override
          public JClassType[] answer(InvocationOnMock invocation) throws Throwable {
            Class<?>[] realThrows = realConstructor.getExceptionTypes();
            JClassType[] gwtThrows = new JClassType[realThrows.length];
            for (int i = 0; i < realThrows.length; i++) {
              gwtThrows[i] = (JClassType) adaptType(realThrows[i]);
            }
            return gwtThrows;
          }
        });

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

    when(method.isStatic()).thenReturn(
        Modifier.isStatic(realMethod.getModifiers()));

    // Return type
    when(method.getReturnType()).thenAnswer(new Answer<JType>() {
      @Override
      public JType answer(InvocationOnMock invocation) throws Throwable {
        return adaptType(realMethod.getReturnType());
      }
    });

    // Parameters
    when(method.getParameters()).thenAnswer(new Answer<JParameter[]>() {
      @Override
      public JParameter[] answer(InvocationOnMock invocation) throws Throwable {
        return adaptParameters(realMethod.getParameterTypes(),
            realMethod.getParameterAnnotations(), method);
      }
    });

    // Thrown exceptions
    when(method.getThrows()).thenAnswer(new Answer<JClassType[]>() {
      @Override
      public JClassType[] answer(InvocationOnMock invocation) throws Throwable {
        Class<?>[] realThrows = realMethod.getExceptionTypes();
        JClassType[] gwtThrows = new JClassType[realThrows.length];
        for (int i = 0; i < realThrows.length; i++) {
          gwtThrows[i] = (JClassType) adaptType(realThrows[i]);
        }
        return gwtThrows;
      }
    });

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

      when(parameter.getEnclosingMethod()).thenReturn(method);
      when(parameter.getType()).thenAnswer(new Answer<JType>() {
        @Override
        public JType answer(InvocationOnMock invocation) throws Throwable {
          return adaptType(realParameterType);
        }
      });

      // Add annotation behaviour
      final Annotation[] annotations = parameterAnnotations[i];

      when(parameter.isAnnotationPresent(any(Class.class))).thenAnswer(
          new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
              Class<? extends Annotation> annotationClass =
                  (Class<? extends Annotation>)
                  invocation.getArguments()[0];
              for (Annotation annotation : annotations) {
                if (annotation.equals(annotationClass)) {
                  return true;
                }
              }
              return false;
            }
          });

      when(parameter.getAnnotation(any(Class.class))).thenAnswer(
          new Answer<Annotation>() {
            @Override
            public Annotation answer(InvocationOnMock invocation) throws Throwable {
              Class<? extends Annotation> annotationClass =
                  (Class<? extends Annotation>)
                  invocation.getArguments()[0];
              for (Annotation annotation : annotations) {
                if (annotation.equals(annotationClass)) {
                  return annotation;
                }
              }
              return null;
            }
          });
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
    when(member.isPublic()).thenReturn(Modifier.isPublic(modifiers));
    when(member.isProtected()).thenReturn(Modifier.isProtected(modifiers));
    when(member.isPrivate()).thenReturn(Modifier.isPrivate(modifiers));
    when(member.getName()).thenReturn(realMember.getName());
    when(member.getEnclosingType()).thenReturn(enclosingType);
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
    when(element.isAnnotationPresent(any(Class.class))).thenAnswer(
        new Answer<Boolean>() {
          @Override
          public Boolean answer(InvocationOnMock invocation) throws Throwable {
            Class<? extends Annotation> annotationClass =
                (Class<? extends Annotation>) invocation.getArguments()[0];
            return realElement.isAnnotationPresent(annotationClass);
          }
        });

    when(element.getAnnotation(any(Class.class))).thenAnswer(
        new Answer<Annotation>() {
          @Override
          public Annotation answer(InvocationOnMock invocation) throws Throwable {
            Class<? extends Annotation> annotationClass =
                (Class<? extends Annotation>) invocation.getArguments()[0];
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
    T mock = Mockito.mock(clazz);
    allMocks.add(mock);
    return mock;
  }
}
