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
package org.hibernate.jsr303.tck.util;

import static com.google.gwt.thirdparty.guava.common.base.Predicates.and;
import static com.google.gwt.thirdparty.guava.common.base.Predicates.not;
import static com.google.gwt.thirdparty.guava.common.base.Predicates.or;
import static com.google.gwt.thirdparty.guava.common.collect.ImmutableList.copyOf;
import static com.google.gwt.thirdparty.guava.common.collect.Iterables.filter;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.thirdparty.guava.common.base.Predicate;

import junit.framework.Test;
import junit.framework.TestCase;

import org.hibernate.jsr303.tck.util.client.Failing;
import org.hibernate.jsr303.tck.util.client.NonTckTest;
import org.hibernate.jsr303.tck.util.client.NotSupported;
import org.hibernate.jsr303.tck.util.client.TestNotCompatible;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

/**
 * Wrapper for {@link GWTTestSuite} to prevent selected methods from running.
 *
 * Copied code from {@link junit.framework.TestSuite} and modified to exclude
 * test methods with select annotations.
 */
public class TckTestSuiteWrapper extends GWTTestSuite {

  private static final Predicate<Method> HAS_FAILING = createHasAnnotationPredicate(Failing.class);
  private static final Predicate<Method> HAS_NON_TCK_TEST = createHasAnnotationPredicate(NonTckTest.class);
  private static final Predicate<Method> HAS_NOT_SUPPORTED = createHasAnnotationPredicate(NotSupported.class);
  private static final Predicate<Method> HAS_TEST_NOT_COMPATIBLE = createHasAnnotationPredicate(TestNotCompatible.class);

  private static final Predicate<Method> INCLUDE_FAILING = createHasProperty(Failing.INCLUDE);
  private static final Predicate<Method> INCLUDE_NOT_SUPPORTED = createHasProperty(NotSupported.INCLUDE);
  private static final Predicate<Method> INCLUDE_TEST_NOT_COMPATIBLE = createHasProperty(TestNotCompatible.INCLUDE);
  private static final Predicate<Method> EXCLUDE_NON_TCK_TEST = createHasProperty(NonTckTest.EXCLUDE);

  @SuppressWarnings("unchecked")
  private static final Predicate<Method> METHOD_FILTER = 
      and(
         or(INCLUDE_NOT_SUPPORTED, not(HAS_NOT_SUPPORTED)),
         or(INCLUDE_TEST_NOT_COMPATIBLE, not(HAS_TEST_NOT_COMPATIBLE)),
         or(INCLUDE_FAILING, not(HAS_FAILING)),
         not(and(EXCLUDE_NON_TCK_TEST, HAS_NON_TCK_TEST))
      );

  public static Predicate<Method> createHasAnnotationPredicate(
      final Class<? extends Annotation> annotationClass) {
    return new Predicate<Method>() {

      public boolean apply(Method method) {
        return method.getAnnotation(annotationClass) != null;
      }
    };
  }

  private static <T> Predicate<T> createHasProperty(final String property) {
    return new Predicate<T>() {
      public boolean apply(T notUsed) {
        String value = System.getProperty(property);
        return Boolean.parseBoolean(value);
      }
    };
  }

  public TckTestSuiteWrapper(String name) {
    super(name);
  }

  /**
   * Adds the tests from the given class to the suite.
   */
  @Override
  public void addTestSuite(Class theClass) {

    String fName = theClass.getName();
    try {
      getTestConstructor(theClass); // Avoid generating multiple error messages
    } catch (NoSuchMethodException e) {
      addTest(warning("Class " + theClass.getName()
          + " has no public constructor TestCase(String name) or TestCase()"));
      return;
    }

    if (!Modifier.isPublic(theClass.getModifiers())) {
      addTest(warning("Class " + theClass.getName() + " is not public"));
      return;
    }

    Class superClass = theClass;
    Vector names = new Vector();
    while (Test.class.isAssignableFrom(superClass)) {
      for (Method method : filter(copyOf(superClass.getDeclaredMethods()),
          METHOD_FILTER)) {
        addTestMethod(method, names, theClass);
      }
      superClass = superClass.getSuperclass();
    }
    if (testCount() == 0)
      addTest(warning("No tests found in " + theClass.getName()));
  }

  private void addTestMethod(Method m, Vector names, Class theClass) {
    String name = m.getName();
    if (names.contains(name))
      return;
    if (!isPublicTestMethod(m)) {
      if (isTestMethod(m))
        addTest(warning("Test method isn't public: " + m.getName()));
      return;
    }
    names.addElement(name);
    addTest(createTest(theClass, name));
  }

  private boolean ingoreMethod(Method m) {
    return HAS_FAILING.apply(m);
  }

  private boolean isPublicTestMethod(Method m) {
    return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
  }

  private boolean isTestMethod(Method m) {
    String name = m.getName();
    Class[] parameters = m.getParameterTypes();
    Class returnType = m.getReturnType();
    return parameters.length == 0 && name.startsWith("test")
        && returnType.equals(Void.TYPE);
  }
}
