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
import com.google.gwt.user.rebind.ui.ImageBundleGenerator.JMethodOracle;

import junit.framework.TestCase;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link ImageBundleGenerator}. Note that, at present, only a
 * small minority of the functionality is really covered. The present tests
 * cover the correctness of compile-time diagnostics and the sequence and
 * phrasing of error and warning log messages.
 */
public class ImageBundleGeneratorTest extends TestCase {

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
      getImageName(logger, new String[0], "nonexistentImg", "test", new String[0][], null);
      fail("Should have thrown");
    } catch (UnableToCompleteException e) {
    }
    logger.assertCorrectLogEntries();
  }

  /**
   * Tests that a message is logged and an exception is thrown when a resource
   * isn't found after being sought based on a javadoc "gwt.resource"
   * pseudo-annotation.
   */
  public void testResourceNotFoundGivenLegacyJavaDoc() {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.expectWarn(ImageBundleGenerator.MSG_JAVADOC_FORM_DEPRECATED, null);
    b.expectError(
        ImageBundleGenerator.msgCannotFindImageFromMetaData("from/metadata/notfound.png"), null);
    UnitTestTreeLogger logger = b.createLogger();

    try {
      getImageName(logger, new String[0], "nonexistentImg", "from.metadata",
          new String[][] {{"notfound.png"}}, null);
      fail("Should have thrown");
    } catch (UnableToCompleteException e) {
    }
    logger.assertCorrectLogEntries();
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
        ImageBundleGenerator.msgCannotFindImageFromMetaData("from/metadata/notfound.png"), null);
    UnitTestTreeLogger logger = b.createLogger();

    try {
      getImageName(logger, new String[0], "nonexistentImg", "from.metadata", new String[0][],
          getResourceAnnotation("testResourceNotFoundGivenAnnotation"));
      fail("Should have thrown");
    } catch (UnableToCompleteException e) {
    }
    logger.assertCorrectLogEntries();
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
   * Tests that resources can be found in a variety of ways from an image bundle
   * residing in the default package.
   */
  public void testResourcesFoundFromImageBundleInDefaultPackage() throws UnableToCompleteException {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    // Due to [2] below
    b.expectWarn(ImageBundleGenerator.MSG_JAVADOC_FORM_DEPRECATED, null);
    // Due to [4] below
    b.expectWarn(ImageBundleGenerator.MSG_JAVADOC_FORM_DEPRECATED, null);
    UnitTestTreeLogger logger = b.createLogger();

    {
      // [1] Find an image in the default package using the method name.
      String imgName = getImageName(logger, new String[] {"cabbage.jpg", "lettuce.jpg",},
          "cabbage", "", new String[0][], null);
      assertEquals("cabbage.jpg", imgName);
    }

    {
      // [2] Find an image in the default package using javadoc.
      String imgName = getImageName(logger, new String[] {"car.png", "dunebuggy.gif",},
          "vehicleImage", "", new String[][] {{"dunebuggy.gif"}}, null);
      assertEquals("dunebuggy.gif", imgName);
    }

    {
      // [3] Find an image in the default package using an annotation.
      String imgName = getImageName(logger, new String[] {"car.png", "dunebuggy.gif",},
          "vehicleImage", "", new String[0][], getResourceAnnotation("duneBuggyInDefaultPackage"));
      assertEquals("dunebuggy.gif", imgName);
    }

    {
      // [4] Find an image in a non-default package using javadoc.
      String imgName = getImageName(logger, new String[] {
          "car.png", "dunebuggy.gif", "a/b/c/dunebuggy.gif",}, "vehicleImage", "",
          new String[][] {{"a/b/c/dunebuggy.gif"}}, null);
      assertEquals("a/b/c/dunebuggy.gif", imgName);
    }

    {
      // [5] Find an image in a non-default package using an annotation.
      String imgName = getImageName(logger, new String[] {
          "car.png", "dunebuggy.gif", "a/b/c/dunebuggy.gif",}, "vehicleImage", "", new String[0][],
          getResourceAnnotation("duneBuggyInNonDefaultPackage"));
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
    // Due to [2] below
    b.expectWarn(ImageBundleGenerator.MSG_JAVADOC_FORM_DEPRECATED, null);
    // Due to [4] below
    b.expectWarn(ImageBundleGenerator.MSG_JAVADOC_FORM_DEPRECATED, null);
    UnitTestTreeLogger logger = b.createLogger();

    {
      // [1] Find an image in the same package using the method name.
      String imgName = getImageName(logger, new String[] {"x/y/z/europa.png", "x/y/z/io.jpg",},
          "io", "x.y.z", new String[0][], null);
      assertEquals("x/y/z/io.jpg", imgName);
    }

    {
      // [2] Find an image in the same package using javadoc.
      String imgName = getImageName(logger, new String[] {"x/y/z/europa.png", "x/y/z/io.jpg",},
          "moonImage", "x.y.z", new String[][] {{"io.jpg"}}, null);
      assertEquals("x/y/z/io.jpg", imgName);
    }

    {
      // [3] Find an image in the same package using an annotation.
      String imgName = getImageName(logger, new String[] {"x/y/z/europa.png", "x/y/z/io.jpg",},
          "moonImage", "x.y.z", new String[0][], getResourceAnnotation("ioInSamePackage"));
      assertEquals("x/y/z/io.jpg", imgName);
    }

    {
      // [4] Find an image in a non-default package using javadoc.
      String imgName = getImageName(logger, new String[] {
          "car.png", "dunebuggy.gif", "a/b/c/dunebuggy.gif",}, "vehicleImage", "x.y.z",
          new String[][] {{"a/b/c/dunebuggy.gif"}}, null);
      assertEquals("a/b/c/dunebuggy.gif", imgName);
    }

    {
      // [5] Find an image in a non-default package using an annotation.
      String imgName = getImageName(logger, new String[] {
          "car.png", "dunebuggy.gif", "a/b/c/dunebuggy.gif",}, "vehicleImage", "x.y.z",
          new String[0][], getResourceAnnotation("duneBuggyInNonDefaultPackage"));
      assertEquals("a/b/c/dunebuggy.gif", imgName);
    }

    logger.assertCorrectLogEntries();
  }

  /**
   * Tests that a warning is logged if the old "gwt.resource" javadoc construct
   * is used.
   */
  public void testWarnOnUseOfLegacyJavaDoc() throws UnableToCompleteException {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.expectWarn(ImageBundleGenerator.MSG_JAVADOC_FORM_DEPRECATED, null);
    UnitTestTreeLogger logger = b.createLogger();

    String imgName = getImageName(logger, new String[] {
        "test/flypaper.gif", "test/horse shoes.jpg", "test/Horse Shoes.jpg"}, "horseshoes", "test",
        new String[][] {{"Horse Shoes.jpg"}}, null);

    assertEquals("test/Horse Shoes.jpg", imgName);
    logger.assertCorrectLogEntries();
  }

  /**
   * Tests that a warning is logged if both the old "gwt.resource" javadoc
   * construct is used and a resource annotation is used.
   */
  @Resource("Horse Shoes.jpg")
  public void testWarnOnUseOfAnnotationAndLegacyJavaDoc() throws UnableToCompleteException {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.expectWarn(ImageBundleGenerator.MSG_MULTIPLE_ANNOTATIONS, null);
    UnitTestTreeLogger logger = b.createLogger();

    Resource resAnn = getResourceAnnotation("testWarnOnUseOfAnnotationAndLegacyJavaDoc");
    String imgName = getImageName(logger, new String[] {
        "test/flypaper.gif", "test/horse shoes.jpg", "test/Horse Shoes.jpg"}, "horseshoes", "test",
        new String[][] {{"Horse Shoes.jpg"}}, resAnn);

    assertEquals("test/Horse Shoes.jpg", imgName);
    logger.assertCorrectLogEntries();
  }

  private Resource getResourceAnnotation(String methodName) {
    Throwable caught = null;
    try {
      Resource res = getClass().getMethod(methodName, new Class[0]).getAnnotation(Resource.class);
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

  private String getImageName(UnitTestTreeLogger logger, final String[] pretendResources,
      String methodName, String pkgName, String[][] resMetadata, final Resource resAnn)
      throws UnableToCompleteException {
    ImageBundleGenerator ibg = new ImageBundleGenerator(new ImageBundleGenerator.ResourceLocator() {
      private final List<String> resList = Arrays.asList(pretendResources);

      public boolean isResourcePresent(String resName) {
        return resList.contains(resName);
      }
    });
    JMethodOracle methodOracle = createJMethodOracle(methodName, pkgName, resMetadata, resAnn);
    return ibg.getImageResourceName(logger, methodOracle);
  }

  private JMethodOracle createJMethodOracle(final String methodName, final String packageName,
      final String[][] resourceMetadata, final Resource resourceAnnotation) {
    return new JMethodOracle() {
      public Resource getAnnotation(Class<Resource> clazz) {
        return resourceAnnotation;
      }

      public String[][] getMetaData(String metadataTag) {
        return resourceMetadata;
      }

      public String getName() {
        return methodName;
      }

      public String getPackageName() {
        return packageName;
      }
    };
  }
}
