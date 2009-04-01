/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.rebind.ui;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.user.client.ui.ImageBundle.Resource;
import com.google.gwt.user.rebind.ui.ImageBundleBuilder.HasRect;
import com.google.gwt.user.rebind.ui.ImageBundleGenerator.JMethodOracle;

import junit.framework.TestCase;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link ImageBundleGenerator}. Note that, at present, only a
 * small minority of the functionality is really covered. The present tests
 * cover the correctness of compile-time diagnostics and the sequence and
 * phrasing of error and warning log messages.
 */
public class ImageBundleGeneratorTest extends TestCase {

  private static class MockRect implements HasRect {
    private final String name;
    private final int width, height;
    private int left, top;
    private boolean hasBeenPositioned;

    MockRect(String name, int width, int height) {
      this.name = name;
      this.width = width;
      this.height = height;
    }

    public int getHeight() {
      return height;
    }

    public int getLeft() {
      return left;
    }

    public String getName() {
      return name;
    }

    public int getTop() {
      return top;
    }

    public int getWidth() {
      return width;
    }

    public boolean hasBeenPositioned() {
      return hasBeenPositioned;
    }

    public void setPosition(int left, int top) {
      this.left = left;
      this.top = top;
      hasBeenPositioned = true;
    }

    @Override
    public String toString() {
      return "{" + left + ", " + top + ", " + width + ", " + height + "}";
    }

    void assertPosition(int left, int top) {
      assertTrue(hasBeenPositioned);
      assertEquals(left, this.left);
      assertEquals(top, this.top);
    }

    MockRect duplicate() {
      final MockRect mr = new MockRect(name, width, height);
      mr.hasBeenPositioned = hasBeenPositioned;
      mr.left = left;
      mr.top = top;
      return mr;
    }
  }

  /**
   * Ensures that two collections of MockRects are arranged identically.
   * 
   * @param a first collection
   * @param b second collection
   */
  private static void assertSameArrangement(Collection<MockRect> a,
      Collection<MockRect> b) {
    final Map<String, MockRect> index = new HashMap<String, MockRect>();
    for (MockRect r : a) {
      index.put(r.getName(), r);
    }

    for (MockRect rb : b) {
      final MockRect ra = index.get(rb.getName());
      rb.assertPosition(ra.getLeft(), ra.getTop());
    }
  }

  @Resource("dunebuggy.gif")
  public void duneBuggyInDefaultPackage() {
  }

  @Resource("a/b/c/dunebuggy.gif")
  public void duneBuggyInNonDefaultPackage() {
  }

  @Resource("io.jpg")
  public void ioInSamePackage() {
  }

  /**
   * Validates that presenting the images in different orders does not affect
   * the arrangement.
   */
  public void testArrangementIsDeterministic() {

    List<MockRect> orderA = Arrays.asList(new MockRect("a", 50, 100),
        new MockRect("b", 50, 100), new MockRect("c", 100, 50), new MockRect(
            "d", 50, 50), new MockRect("e", 50, 50));
    List<MockRect> orderB = new ArrayList<MockRect>();
    for (int i = orderA.size() - 1; i <= 0; --i) {
      orderB.add(orderA.get(i));
    }

    ImageBundleBuilder.arrangeImages(orderA);
    ImageBundleBuilder.arrangeImages(orderB);

    assertSameArrangement(orderA, orderB);
  }

  /**
   * Ensures that the basic image packing algorithm is arranging images as
   * expected.
   */
  public void testBasicImageArrangement() {
    // Expected in 2nd column, 2nd in 2nd row.
    final MockRect ra = new MockRect("a", 20, 30);
    // Expected to be 1st column.
    final MockRect rb = new MockRect("b", 10, 100);
    // Expected in 2nd column, 1st in 2nd row.
    final MockRect rc = new MockRect("c", 20, 40);
    // Expected in 2nd column, it is the 1st row.
    final MockRect rd = new MockRect("d", 100, 60);
    // Expected in 2nd column, 3rd in 2nd row.
    final MockRect re = new MockRect("e", 10, 30);

    final Collection<MockRect> rects = new ArrayList<MockRect>();
    Collections.addAll(rects, ra, rb, rc, rd, re);
    ImageBundleBuilder.arrangeImages(rects);

    ra.assertPosition(30, 60);
    rb.assertPosition(0, 0);
    rc.assertPosition(10, 60);
    rd.assertPosition(10, 0);
    re.assertPosition(50, 60);
  }

  /**
   * This method is to remind the reader that ClassLoader.getResource() doesn't
   * want a leading "/" on resource names.
   */
  public void testClassLoaderGetResourceHatesLeadingSlash() {
    String fqcn = getClass().getCanonicalName();
    // This class has a name.
    assertNotNull(fqcn);
    String thisClassResName = fqcn.replace('.', '/') + ".class";
    // It has some slashes.
    assertTrue(thisClassResName.indexOf('/') >= 0);
    // But it does not start with a slash.
    assertFalse(thisClassResName.startsWith("/"));
    // The resource is successfully found without a leading slash.
    URL url = getClass().getClassLoader().getResource(thisClassResName);
    assertNotNull(url);
    // It fails to be found found with a leading slash.
    url = getClass().getClassLoader().getResource("/" + thisClassResName);
    assertNull(url);
  }

  /**
   * Tests that a message is logged and an exception is thrown when a resource
   * isn't found after being sought based on the ImageBundle.Resource
   * annotation.
   */
  @Resource("notfound.png")
  public void testResourceNotFoundGivenAnnotation() {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.expectError(
        ImageBundleGenerator.msgCannotFindImageFromMetaData("from/metadata/notfound.png"),
        null);
    UnitTestTreeLogger logger = b.createLogger();

    try {
      getImageName(logger, new String[0], "nonexistentImg", "from.metadata",
          getResourceAnnotation("testResourceNotFoundGivenAnnotation"));
      fail("Should have thrown");
    } catch (UnableToCompleteException e) {
    }
    logger.assertCorrectLogEntries();
  }

  /**
   * Tests that a message is logged and an exception is thrown when a resource
   * isn't found after being sought based on the method name.
   */
  public void testResourceNotFoundGivenNoMetaData() {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.expectError(ImageBundleGenerator.MSG_NO_FILE_BASED_ON_METHOD_NAME, null);
    b.expectError("test/nonexistentImg.png", null);
    b.expectError("test/nonexistentImg.gif", null);
    b.expectError("test/nonexistentImg.jpg", null);
    UnitTestTreeLogger logger = b.createLogger();

    try {
      getImageName(logger, new String[0], "nonexistentImg", "test", null);
      fail("Should have thrown");
    } catch (UnableToCompleteException e) {
    }
    logger.assertCorrectLogEntries();
  }

  /**
   * Tests that resources can be found in a variety of ways from an image bundle
   * residing in the default package.
   */
  public void testResourcesFoundFromImageBundleInDefaultPackage()
      throws UnableToCompleteException {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    UnitTestTreeLogger logger = b.createLogger();

    {
      // [1] Find an image in the default package using the method name.
      String imgName = getImageName(logger, new String[] {
          "cabbage.jpg", "lettuce.jpg",}, "cabbage", "", null);
      assertEquals("cabbage.jpg", imgName);
    }

    {
      // [2] Find an image in the default package using an annotation.
      String imgName = getImageName(logger, new String[] {
          "car.png", "dunebuggy.gif",}, "vehicleImage", "",
          getResourceAnnotation("duneBuggyInDefaultPackage"));
      assertEquals("dunebuggy.gif", imgName);
    }
    {
      // [3] Find an image in a non-default package using an annotation.
      String imgName = getImageName(logger, new String[] {
          "car.png", "dunebuggy.gif", "a/b/c/dunebuggy.gif",}, "vehicleImage",
          "", getResourceAnnotation("duneBuggyInNonDefaultPackage"));
      assertEquals("a/b/c/dunebuggy.gif", imgName);
    }

    logger.assertCorrectLogEntries();
  }

  /**
   * Tests that resources can be found in a variety of ways from an image bundle
   * residing in a non-default package.
   */
  public void testResourcesFoundFromImageBundleInNonDefaultPackage()
      throws UnableToCompleteException {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    UnitTestTreeLogger logger = b.createLogger();

    {
      // [1] Find an image in the same package using the method name.
      String imgName = getImageName(logger, new String[] {
          "x/y/z/europa.png", "x/y/z/io.jpg",}, "io", "x.y.z", null);
      assertEquals("x/y/z/io.jpg", imgName);
    }

    {
      // [2] Find an image in the same package using an annotation.
      String imgName = getImageName(logger, new String[] {
          "x/y/z/europa.png", "x/y/z/io.jpg",}, "moonImage", "x.y.z",
          getResourceAnnotation("ioInSamePackage"));
      assertEquals("x/y/z/io.jpg", imgName);
    }

    {
      // [3] Find an image in a non-default package using an annotation.
      String imgName = getImageName(logger, new String[] {
          "car.png", "dunebuggy.gif", "a/b/c/dunebuggy.gif",}, "vehicleImage",
          "x.y.z", getResourceAnnotation("duneBuggyInNonDefaultPackage"));
      assertEquals("a/b/c/dunebuggy.gif", imgName);
    }

    logger.assertCorrectLogEntries();
  }

  private JMethodOracle createJMethodOracle(final String methodName,
      final String packageName, final Resource resourceAnnotation) {
    return new JMethodOracle() {
      public Resource getAnnotation(Class<Resource> clazz) {
        return resourceAnnotation;
      }

      public String getName() {
        return methodName;
      }

      public String getPackageName() {
        return packageName;
      }
    };
  }

  private String getImageName(UnitTestTreeLogger logger,
      final String[] pretendResources, String methodName, String pkgName,
      final Resource resAnn) throws UnableToCompleteException {
    ImageBundleGenerator ibg = new ImageBundleGenerator(
        new ImageBundleGenerator.ResourceLocator() {
          private final List<String> resList = Arrays.asList(pretendResources);

          public boolean isResourcePresent(String resName) {
            return resList.contains(resName);
          }
        });
    JMethodOracle methodOracle = createJMethodOracle(methodName, pkgName,
        resAnn);
    return ibg.getImageResourceName(logger, methodOracle);
  }

  private Resource getResourceAnnotation(String methodName) {
    Throwable caught = null;
    try {
      Resource res = getClass().getMethod(methodName, new Class[0]).getAnnotation(
          Resource.class);
      assertNotNull(res);
      return res;
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    }
    fail("Unable to get @Resource annotation from method '" + methodName
        + "' during test due to exception: " + caught.getMessage());
    return null;
  }
}
