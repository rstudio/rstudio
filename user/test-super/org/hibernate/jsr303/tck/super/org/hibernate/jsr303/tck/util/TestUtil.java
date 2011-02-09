// $Id: TestUtil.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
/*
 * JBoss, Home of Professional Open Source Copyright 2009, Red Hat, Inc. and/or
 * its affiliates, and individual contributors by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of individual
 * contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.hibernate.jsr303.tck.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.Validation;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.PropertyDescriptor;
import javax.validation.spi.ValidationProvider;

/**
 * Modified by Google.
 * <ul>
 * <li>Use RegExp instead of Pattern</li>
 * </ul>
 * @author Hardy Ferentschik
 */
public final class TestUtil {

  private static String VALIDATION_PROVIDER_TEST_CLASS = "validation.provider";

  private static ValidationProvider<?> validationProviderUnderTest;

  private TestUtil() {
  }

  public static Validator getValidatorUnderTest() {
    return getValidatorFactoryUnderTest().getValidator();
  }

  public static ValidationProvider<?> getValidationProviderUnderTest() {
    if (validationProviderUnderTest == null) {
      instantiateValidationProviderUnderTest();
    }
    return validationProviderUnderTest;
  }

  public static ValidatorFactory getValidatorFactoryUnderTest() {
    Configuration<?> config = getConfigurationUnderTest();
    return config.buildValidatorFactory();
  }

  public static Configuration<?> getConfigurationUnderTest() {
    if (validationProviderUnderTest == null) {
      instantiateValidationProviderUnderTest();
    }

    ProviderSpecificBootstrap<?> bootstrap =
    Validation.byProvider(validationProviderUnderTest.getClass());
    return bootstrap.configure();
  }

  public static MessageInterpolator getDefaultMessageInterpolator() {
    Configuration<?> config = getConfigurationUnderTest();
    return config.getDefaultMessageInterpolator();
  }

  public static <T> void assertCorrectNumberOfViolations(
      Set<ConstraintViolation<T>> violations, int expectedViolations) {
    assertEquals(violations.size(), expectedViolations,
        "Wrong number of constraint violations. Expected: "
            + expectedViolations + " Actual: " + violations.size());
  }

  public static <T> void assertCorrectConstraintViolationMessages(
      Set<ConstraintViolation<T>> violations, String... messages) {
    List<String> actualMessages = new ArrayList<String>();
    for (ConstraintViolation<?> violation : violations) {
      actualMessages.add(violation.getMessage());
    }

    assertTrue(actualMessages.size() == messages.length,
        "Wrong number or error messages. Expected: " + messages.length
            + " Actual: " + actualMessages.size());

    for (String expectedMessage : messages) {
      assertTrue(actualMessages.contains(expectedMessage), "The message '"
          + expectedMessage
          + "' should have been in the list of actual messages: "
          + actualMessages);
      actualMessages.remove(expectedMessage);
    }
    assertTrue(actualMessages.isEmpty(),
        "Actual messages contained more messages as specified expected messages");
  }

  public static <T> void assertCorrectConstraintTypes(
      Set<ConstraintViolation<T>> violations,
      Class<?>... expectedConsraintTypes) {
    List<String> actualConstraintTypes = new ArrayList<String>();
    for (ConstraintViolation<?> violation : violations) {
      actualConstraintTypes.add(((Annotation) violation.getConstraintDescriptor().getAnnotation()).annotationType().getName());
    }

    assertEquals(expectedConsraintTypes.length, actualConstraintTypes.size(),
        "Wrong number of constraint types.");

    for (Class<?> expectedConstraintType : expectedConsraintTypes) {
      assertTrue(
          actualConstraintTypes.contains(expectedConstraintType.getName()),
          "The constraint type " + expectedConstraintType.getName()
              + " is not in the list of actual violated constraint types: "
              + actualConstraintTypes);
    }
  }

  public static <T> void assertCorrectPropertyPaths(
      Set<ConstraintViolation<T>> violations, String... propertyPaths) {
    List<Path> propertyPathsOfViolations = new ArrayList<Path>();
    for (ConstraintViolation<?> violation : violations) {
      propertyPathsOfViolations.add(violation.getPropertyPath());
    }

    assertEquals(propertyPaths.length, propertyPathsOfViolations.size(),
        "Wrong number of property paths. Expected: " + propertyPaths.length
            + " Actual: " + propertyPathsOfViolations.size());

    for (String propertyPath : propertyPaths) {
      Path expectedPath = PathImpl.createPathFromString(propertyPath);
      boolean containsPath = false;
      for (Path actualPath : propertyPathsOfViolations) {
        if (assertEqualPaths(expectedPath, actualPath)) {
          containsPath = true;
          break;
        }
      }
      if (!containsPath) {
        fail(expectedPath
            + " is not in the list of path instances contained in the actual constraint violations: "
            + propertyPathsOfViolations);
      }
    }
  }

  public static <T> void assertConstraintViolation(
      ConstraintViolation<T> violation, Class<?> rootBean, Object invalidValue,
      String propertyPath) {
    Path expectedPath = PathImpl.createPathFromString(propertyPath);
    if (!assertEqualPaths(violation.getPropertyPath(), expectedPath)) {
      fail("Property paths differ. Actual: " + violation.getPropertyPath()
          + " Expected: " + expectedPath);
    }

    assertEquals(violation.getRootBeanClass(), rootBean, "Wrong root bean.");
    assertEquals(violation.getInvalidValue(), invalidValue,
        "Wrong invalid value.");
  }

  public static boolean assertEqualPaths(Path p1, Path p2) {
    Iterator<Path.Node> p1Iterator = p1.iterator();
    Iterator<Path.Node> p2Iterator = p2.iterator();
    while (p1Iterator.hasNext()) {
      Path.Node p1Node = p1Iterator.next();
      if (!p2Iterator.hasNext()) {
        return false;
      }
      Path.Node p2Node = p2Iterator.next();

      // do the comparison on the node values
      if (p2Node.getName() == null) {
        if (p1Node.getName() != null) {
          return false;
        }
      } else if (!p2Node.getName().equals(p1Node.getName())) {
        return false;
      }

      if (p2Node.isInIterable() != p1Node.isInIterable()) {
        return false;
      }

      if (p2Node.getIndex() == null) {
        if (p1Node.getIndex() != null) {
          return false;
        }
      } else if (!p2Node.getIndex().equals(p1Node.getIndex())) {
        return false;
      }

      if (p2Node.getKey() == null) {
        if (p1Node.getKey() != null) {
          return false;
        }
      } else if (!p2Node.getKey().equals(p1Node.getKey())) {
        return false;
      }
    }

    return !p2Iterator.hasNext();
  }

  public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz,
      String property) {
    Validator validator = getValidatorUnderTest();
    return validator.getConstraintsForClass(clazz).getConstraintsForProperty(
        property);
  }

  public static Set<ConstraintDescriptor<?>> getConstraintDescriptorsFor(
      Class<?> clazz, String property) {
    ElementDescriptor elementDescriptor = getPropertyDescriptor(clazz, property);
    return elementDescriptor.getConstraintDescriptors();
  }

  public static Object getInputStreamForPath(String path) {

    return null;
  }

  private static <U extends ValidationProvider<?>> void instantiateValidationProviderUnderTest() {
      validationProviderUnderTest = GWT.create(ValidationProvider.class);
  }

  public static class PathImpl implements Path {

    /**
     * Regular expression used to split a string path into its elements.
     *
     * @see <a href="http://www.regexplanet.com/simple/index.jsp">Regular
     *      expression tester</a>
     */
    private static final RegExp pathPattern = RegExp.compile("(\\w+)(\\[(\\w*)\\])?(\\.(.*))*");

    private static final String PROPERTY_PATH_SEPARATOR = ".";

    private final List<Node> nodeList;

    public static PathImpl createPathFromString(String propertyPath) {
      if (propertyPath == null) {
        throw new IllegalArgumentException(
            "null is not allowed as property path.");
      }

      if (propertyPath.length() == 0) {
        return createNewPath(null);
      }

      return parseProperty(propertyPath);
    }

    public static PathImpl createNewPath(String name) {
      PathImpl path = new PathImpl();
      NodeImpl node = new NodeImpl(name);
      path.addNode(node);
      return path;
    }

    private PathImpl() {
      nodeList = new ArrayList<Node>();
    }

    public void addNode(Node node) {
      nodeList.add(node);
    }

    public Iterator<Path.Node> iterator() {
      return nodeList.iterator();
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      Iterator<Path.Node> iter = iterator();
      while (iter.hasNext()) {
        Node node = iter.next();
        builder.append(node.toString());
        if (iter.hasNext()) {
          builder.append(PROPERTY_PATH_SEPARATOR);
        }
      }
      return builder.toString();
    }

    private static PathImpl parseProperty(String property) {
      PathImpl path = new PathImpl();
      String tmp = property;
      do {
        MatchResult matcher = pathPattern.exec(tmp);
        if (matcher != null) {
          String value = matcher.getGroup(1);
          String indexed = matcher.getGroup(2);
          String index = matcher.getGroup(3);
          NodeImpl node = new NodeImpl(value);

          if (indexed != null && indexed.length() > 0) {
            node.setInIterable(true);
          }
          if (index != null && index.length() > 0) {
            try {
              Integer i = Integer.parseInt(index);
              node.setIndex(i);
            } catch (NumberFormatException e) {
              node.setKey(index);
            }
          }
          path.addNode(node);
          tmp = matcher.getGroup(5);
        } else {
          throw new IllegalArgumentException("Unable to parse property path "
              + property);
        }
      } while (tmp != null && tmp.length() > 0);
      return path;
    }
  }

  public static class NodeImpl implements Path.Node {

    private static final String INDEX_OPEN = "[";
    private static final String INDEX_CLOSE = "]";

    private final String name;
    private boolean isInIterable;
    private Integer index;
    private Object key;

    public NodeImpl(String name) {
      this.name = name;
    }

    NodeImpl(Path.Node node) {
      this.name = node.getName();
      this.isInIterable = node.isInIterable();
      this.index = node.getIndex();
      this.key = node.getKey();
    }

    public String getName() {
      return name;
    }

    public boolean isInIterable() {
      return isInIterable;
    }

    public void setInIterable(boolean inIterable) {
      isInIterable = inIterable;
    }

    public Integer getIndex() {
      return index;
    }

    public void setIndex(Integer index) {
      isInIterable = true;
      this.index = index;
    }

    public Object getKey() {
      return key;
    }

    public void setKey(Object key) {
      isInIterable = true;
      this.key = key;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder(name == null ? "" : name);
      if (isInIterable) {
        builder.append(INDEX_OPEN);
        if (getIndex() != null) {
          builder.append(getIndex());
        } else if (getKey() != null) {
          builder.append(getKey());
        }
        builder.append(INDEX_CLOSE);
      }
      return builder.toString();
    }
  }
}
