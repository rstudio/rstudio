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
package com.google.gwt.user.tools;

import com.google.gwt.dev.util.Util;
import com.google.gwt.user.tools.WebAppCreator.ArgProcessor;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Test Class for WebAppCreator.
 */
public class WebAppCreatorTest extends TestCase {

  private static final String MY_PROJECT = "com.foo.Hello";
  private String mockJar;
  private String projectFolder;
  private File tempFolder;
  private String tempPath;

  @Override
  public void setUp() throws Exception {
    // Create a temporary folder for the test
    tempFolder = File.createTempFile("gwt-test-webappcreator-", "");
    assertTrue(tempFolder.delete());
    assertTrue(tempFolder.mkdir());
    tempPath = tempFolder.getAbsolutePath();

    // Generate an empty .jar
    mockJar = tempPath + File.separatorChar + "mock-junit.jar";
    assertTrue(new File(mockJar).createNewFile());

    // Verify that project folder doesn't exist
    projectFolder = tempPath + File.separatorChar + "project";
    assertFalse(new File(projectFolder).exists());
  }

  @Override
  public void tearDown() {
    // Delete temporary folder
    deleteDir(tempFolder);
  }

  /**
   * Default options, generate ant and eclipse files.
   */
  public void testAppCreatorAnt() throws IOException, WebAppCreatorException {
    runCreator("-out", projectFolder, MY_PROJECT);
    assertFileExists(".project");
    assertFileExists(".classpath");
    assertFileExists("build.xml");
    assertFileExists("README.txt");
    assertFileExists("Hello.launch");
    assertFileExists("war/Hello.html");
    assertFileExists("war/Hello.css");
    assertFileExists("war/WEB-INF/web.xml");
    assertFileExists("src/com/foo/Hello.gwt.xml");
    assertFileExists("src/com/foo/client/GreetingServiceAsync.java");
    assertFileExists("src/com/foo/client/GreetingService.java");
    assertFileExists("src/com/foo/client/Hello.java");
    assertFileExists("src/com/foo/shared/FieldVerifier.java");
    assertFileExists("src/com/foo/server/GreetingServiceImpl.java");
    assertFileDoesNotExist("HelloTest-dev.launch");
    assertFileDoesNotExist("HelloTest-prod.launch");
    assertFileDoesNotExist("test/com/foo/HelloJUnit.gwt.xml");
    assertFileDoesNotExist("test/com/foo/client/HelloTest.java");
  }

  /**
   * Adding a valid junit.jar, the test stuff is generated.
   */
  public void testCreatorAntJunit() throws IOException, WebAppCreatorException {
    runCreator("-out", projectFolder, "-junit", mockJar, MY_PROJECT);
    assertFileExists(".project");
    assertFileExists(".classpath");
    assertFileExists("build.xml");
    assertFileExists("README.txt");
    assertFileExists("Hello.launch");
    assertFileExists("HelloTest-dev.launch");
    assertFileExists("HelloTest-prod.launch");
    assertFileExists("war/Hello.html");
    assertFileExists("war/Hello.css");
    assertFileExists("war/WEB-INF/web.xml");
    assertFileExists("src/com/foo/Hello.gwt.xml");
    assertFileExists("src/com/foo/client/GreetingServiceAsync.java");
    assertFileExists("src/com/foo/client/GreetingService.java");
    assertFileExists("src/com/foo/client/Hello.java");
    assertFileExists("src/com/foo/shared/FieldVerifier.java");
    assertFileExists("src/com/foo/server/GreetingServiceImpl.java");
    assertFileExists("test/com/foo/HelloJUnit.gwt.xml");
    assertFileExists("test/com/foo/client/HelloTest.java");
  }

  /**
   * Default options, generate ant template only.
   */
  public void testAppCreatorAntOnly() throws IOException, WebAppCreatorException {
    runCreator("-out", projectFolder, "-templates", "ant", MY_PROJECT);
    assertFileExists("build.xml");
  }

  /**
   * Check illegal argument combinations.
   */
  public void testCreatorBadArguments() {

    ArgProcessor argProcessor = new WebAppCreator().new ArgProcessor();
    assertFalse(argProcessor.processArgs("-out", projectFolder,
        "unknown_parameter", MY_PROJECT));

    argProcessor = new WebAppCreator().new ArgProcessor();
    assertFalse(argProcessor.processArgs("-out", projectFolder,
        "wrong_project_name"));

    argProcessor = new WebAppCreator().new ArgProcessor();
    assertFalse(argProcessor.processArgs("-out", projectFolder, "-ignore",
        "-overwrite", MY_PROJECT));

    argProcessor = new WebAppCreator().new ArgProcessor();
    assertFalse(argProcessor.processArgs("-out", projectFolder, "-overwrite",
        "-ignore", MY_PROJECT));
  }

  /**
   * Do not generate eclipse files.
   */
  public void testCreatorNoAnt() throws IOException, WebAppCreatorException {
    runCreator("-out", projectFolder, "-noant", "-junit", mockJar, MY_PROJECT);
    assertFileExists(".project");
    assertFileExists(".classpath");
    assertFileDoesNotExist("build.xml");
    assertFileExists("README.txt");
    assertFileExists("Hello.launch");
    assertFileExists("HelloTest-dev.launch");
    assertFileExists("HelloTest-prod.launch");
    assertFileExists("war/Hello.html");
    assertFileExists("war/Hello.css");
    assertFileExists("war/WEB-INF/web.xml");
    assertFileExists("src/com/foo/Hello.gwt.xml");
    assertFileExists("src/com/foo/client/GreetingServiceAsync.java");
    assertFileExists("src/com/foo/client/GreetingService.java");
    assertFileExists("src/com/foo/client/Hello.java");
    assertFileExists("src/com/foo/shared/FieldVerifier.java");
    assertFileExists("src/com/foo/server/GreetingServiceImpl.java");
    assertFileExists("test/com/foo/HelloJUnit.gwt.xml");
    assertFileExists("test/com/foo/client/HelloTest.java");
  }

  /**
   * Do not generate eclipse files.
   */
  public void testCreatorNoEclipse() throws IOException, WebAppCreatorException {
    runCreator("-out", projectFolder, "-XnoEclipse", "-junit", mockJar,
        MY_PROJECT);
    assertFileDoesNotExist(".project");
    assertFileDoesNotExist(".classpath");
    assertFileExists("build.xml");
    assertFileExists("README.txt");
    assertFileDoesNotExist("Hello.launch");
    assertFileDoesNotExist("HelloTest-dev.launch");
    assertFileDoesNotExist("HelloTest-prod.launch");
    assertFileExists("war/Hello.html");
    assertFileExists("war/Hello.css");
    assertFileExists("war/WEB-INF/web.xml");
    assertFileExists("src/com/foo/Hello.gwt.xml");
    assertFileExists("src/com/foo/client/GreetingServiceAsync.java");
    assertFileExists("src/com/foo/client/GreetingService.java");
    assertFileExists("src/com/foo/client/Hello.java");
    assertFileExists("src/com/foo/shared/FieldVerifier.java");
    assertFileExists("src/com/foo/server/GreetingServiceImpl.java");
    assertFileExists("test/com/foo/HelloJUnit.gwt.xml");
    assertFileExists("test/com/foo/client/HelloTest.java");
  }

  /**
   * Generate a maven2 project. Note that -junit option is not needed.
   */
  public void testCreatorMaven() throws IOException, WebAppCreatorException {
    runCreator("-out", projectFolder, "-maven", MY_PROJECT);
    assertFileExists("pom.xml");
    assertFileExists("README.txt");
    assertFileExists("src/main/java/com/foo/shared/FieldVerifier.java");
    assertFileExists("src/main/java/com/foo/server");
    assertFileExists("src/main/java/com/foo/server/GreetingServiceImpl.java");
    assertFileExists("src/main/java/com/foo/Hello.gwt.xml");
    assertFileExists("src/main/java/com/foo/client");
    assertFileExists("src/main/java/com/foo/client/Hello.java");
    assertFileExists("src/main/java/com/foo/client/GreetingServiceAsync.java");
    assertFileExists("src/main/java/com/foo/client/GreetingService.java");
    assertFileExists("src/main/webapp/Hello.html");
    assertFileExists("src/main/webapp/Hello.css");
    assertFileExists("src/main/webapp/WEB-INF/web.xml");
    assertFileExists("src/test/java/com/foo/client/HelloTest.java");
    assertFileExists("src/test/java/com/foo/HelloJUnit.gwt.xml");
  }

  /**
   * Running generator on existing projects.
   */
  public void testCreatorMultipleTimes() throws IOException, WebAppCreatorException {
    // Create the project
    runCreator("-out", projectFolder, MY_PROJECT);

    // Try create the project again without -ignore nor -overwrite
    try {
      runCreator("-out", projectFolder, MY_PROJECT);
      fail("webAppCreator can not be run twice");
    } catch (IOException e) {
    }

    // Check -ignore flag
    try {
      runCreator("-out", projectFolder, "-ignore", MY_PROJECT);
    } catch (IOException e) {
      fail("webAppCreator should not faild with -ignore option when is executed twice");
    }

    // Check -overwrite flag
    try {
      runCreator("-out", projectFolder, "-overwrite", MY_PROJECT);
    } catch (IOException e) {
      fail("webAppCreator should not faild with -ignore option when is executed twice");
    }
  }

  /**
   * Generate only eclipse stuff.
   */
  public void testCreatorOnlyEclipse() throws IOException, WebAppCreatorException {
    runCreator("-out", projectFolder, "-XonlyEclipse", "-junit", mockJar,
        MY_PROJECT);
    assertFileExists(".project");
    assertFileExists(".classpath");
    assertFileDoesNotExist("build.xml");
    assertFileDoesNotExist("README.txt");
    assertFileExists("Hello.launch");
    assertFileExists("HelloTest-dev.launch");
    assertFileExists("HelloTest-prod.launch");
    assertFileDoesNotExist("war/Hello.html");
    assertFileDoesNotExist("war/Hello.css");
    assertFileDoesNotExist("war/WEB-INF/web.xml");
    assertFileDoesNotExist("src/com/foo/Hello.gwt.xml");
    assertFileDoesNotExist("src/com/foo/client/GreetingServiceAsync.java");
    assertFileDoesNotExist("src/com/foo/client/GreetingService.java");
    assertFileDoesNotExist("src/com/foo/client/Hello.java");
    assertFileDoesNotExist("src/com/foo/shared/FieldVerifier.java");
    assertFileDoesNotExist("src/com/foo/server/GreetingServiceImpl.java");
    assertFileDoesNotExist("test/com/foo/HelloJUnit.gwt.xml");
    assertFileDoesNotExist("test/com/foo/client/HelloTest.java");
  }

  /**
   * Generate a .classpath containing a .jar in war/WEB-INF/lib.
   */
  public void testCreatorOnlyEclipseWithJars() throws IOException, WebAppCreatorException {
    runCreator("-out", projectFolder, "-XnoEclipse", "-junit", mockJar,
        MY_PROJECT);

    String libDir = "war" + File.separatorChar
        + "WEB-INF" + File.separatorChar
        + "lib";
    assertTrue(new File(projectFolder + File.separatorChar + libDir).mkdirs());

    String libJarName = libDir + File.separatorChar + "foo.jar";
    File libFile = new File(projectFolder + File.separatorChar + libJarName);
    assertTrue(libFile.createNewFile());

    runCreator("-out", projectFolder, "-XonlyEclipse", "-junit", mockJar,
        MY_PROJECT);

    assertFileExists(".classpath");
    File classpathFile = new File(projectFolder + File.separatorChar + ".classpath");
    String classpathContents = Util.readURLAsString(classpathFile.toURI().toURL());
    String canonicalLibJarName = libJarName.replaceAll(Pattern.quote(File.separator), "/");
    assertTrue(".classpath does not contain " + canonicalLibJarName + ". .classpath contents:"
        + classpathContents,
        classpathContents.contains(canonicalLibJarName));
  }

  /**
   * Test the main method.
   */
  public void testMain() {
    // This property overrides the default gwt installation path
    // Note: this only can be set once because Utility.getInstallPath caches it
    System.setProperty("gwt.devjar", mockJar);
    assertTrue(WebAppCreator.doMain("-out", projectFolder, MY_PROJECT));
    assertFalse(WebAppCreator.doMain());
  }

  private void assertFileDoesNotExist(String file) {
    assertFalse(new File(projectFolder + File.separatorChar + file).exists());
  }

  private void assertFileExists(String file) {
    assertTrue(new File(projectFolder + File.separatorChar + file).exists());
  }

  /**
   * Delete a folder recursively.
   */
  private boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (String child : children) {
        if (!deleteDir(new File(dir, child))) {
          return false;
        }
      }
    }
    return dir.delete();
  }

  /**
   * run appWebCreator.
   * @throws WebAppCreatorException if any template processing fails
   */
  private void runCreator(String... args) throws IOException, WebAppCreatorException {
    WebAppCreator creator = new WebAppCreator();
    ArgProcessor argProcessor = creator.new ArgProcessor();
    if (!argProcessor.processArgs(args)) {
      throw new IllegalArgumentException();
    }
    creator.doRun(tempPath);
  }
}
