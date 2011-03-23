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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shared abstract class for tests that rely on well-known test data.
 * 
 * These tests rely on the external existence of the following files under
 * <code>test/com/google/gwt/dev/javac/impl/testdata/</code>
 * 
 * <pre>
 * cpe1/com/google/gwt/user/User.gwt.xml
 * cpe1/com/google/gwt/user/client/Command.java
 * cpe1/com/google/gwt/user/client/Timer.java
 * cpe1/com/google/gwt/user/client/ui/Widget.java
 * cpe1/org/example/bar/client/BarClient1.txt
 * cpe1/org/example/bar/client/BarClient2.txt
 * cpe1/org/example/bar/client/etc/BarEtc.txt
 * cpe1/org/example/foo/client/FooClient.java
 * cpe1/org/example/foo/server/FooServer.java
 * 
 * cpe1/com/google/gwt/i18n/I18N.gwt.xml
 * cpe2/com/google/gwt/i18n/client/Messages.java
 * cpe2/com/google/gwt/i18n/rebind/LocalizableGenerator.java
 * cpe2/org/example/bar/client/BarClient2.txt
 * cpe2/org/example/bar/client/BarClient3.txt
 * </pre>
 * 
 * The same files should be present in jar files named cpe1.jar and cpe2.jar;
 * note that the contents of each will not have the <code>cpe1/</code> and
 * <code>cpe2/</code> prefixes (respectively) on their contained files.
 */
public abstract class AbstractResourceOrientedTestBase extends TestCase {

  private static class ExcludeSvnClassPathEntry extends ClassPathEntry {
    private final ClassPathEntry cpe;

    public ExcludeSvnClassPathEntry(ClassPathEntry cpe) {
      this.cpe = cpe;
    }

    @Override
    public Map<AbstractResource, PathPrefix> findApplicableResources(
        TreeLogger logger, PathPrefixSet pathPrefixSet) {
      Map<AbstractResource, PathPrefix> results = new IdentityHashMap<AbstractResource, PathPrefix>();
      Map<AbstractResource, PathPrefix> rs = cpe.findApplicableResources(
          logger, pathPrefixSet);
      for (Map.Entry<AbstractResource, PathPrefix> entry : rs.entrySet()) {
        AbstractResource r = entry.getKey();
        if (r.getPath().indexOf(".svn/") < 0) {
          results.put(r, entry.getValue());
        }
      }
      return results;
    }

    @Override
    public String getLocation() {
      return cpe.getLocation();
    }

    @Override
    public String toString() {
      return cpe.toString();
    }
  }

  private static class MOCK_CPE1 extends MockClassPathEntry {
    public MOCK_CPE1() {
      super("/cpe1/");
      addResource("com/google/gwt/user/User.gwt.xml");
      addResource("com/google/gwt/user/client/Command.java");
      addResource("com/google/gwt/user/client/Timer.java");
      addResource("com/google/gwt/user/client/ui/Widget.java");
      addResource("org/example/bar/client/BarClient1.txt");
      addResource("org/example/bar/client/BarClient2.txt");
      addResource("org/example/bar/client/etc/BarEtc.txt");
      addResource("org/example/foo/client/FooClient.java");
      addResource("org/example/foo/server/FooServer.java");
    }
  }

  private static class MOCK_CPE2 extends MockClassPathEntry {
    public MOCK_CPE2() {
      super("C:\\cpe2");
      addResource("com/google/gwt/i18n/I18N.gwt.xml");
      addResource("com/google/gwt/i18n/client/Messages.java");
      addResource("com/google/gwt/i18n/rebind/LocalizableGenerator.java");
      addResource("org/example/bar/client/BarClient2.txt");
      addResource("org/example/bar/client/BarClient3.txt");
      addResource("org/example/foo/client/BarClient1.txt");
    }
  }

  // Set LOG_TO_CONSOLE to true to see a play-by-play.
  private static final boolean LOG_TO_CONSOLE = false;

  public static TreeLogger createTestTreeLogger() {
    if (LOG_TO_CONSOLE) {
      PrintWriterTreeLogger treeLogger = new PrintWriterTreeLogger();
      treeLogger.setMaxDetail(TreeLogger.ALL);
      treeLogger.log(TreeLogger.INFO, "=== logger start ===");
      return treeLogger;
    } else {
      return TreeLogger.NULL;
    }
  }

  protected void assertPathIncluded(Set<AbstractResource> resources, String path) {
    assertNotNull("path = " + path + " should have been found in resources = "
        + resources, findResourceWithPath(resources, path));
  }

  protected void assertPathNotIncluded(Set<AbstractResource> resources,
      String path) {
    assertNull("path = " + path + " should not have been found in resources = "
        + resources, findResourceWithPath(resources, path));
  }

  protected File findJarDirectory(String name) throws URISyntaxException {
    ClassLoader classLoader = getClass().getClassLoader();
    URL url = classLoader.getResource(name);
    assertNotNull("Expecting on the classpath: " + name);
    File file = new File(url.toURI());
    assertTrue("Cannot read as file: " + url.toExternalForm(), file.canRead());
    return file;
  }

  protected File findFile(String name) throws URISyntaxException {
    URL url = findUrl(name);
    assertNotNull(
        "Expecting on the classpath: "
            + name
            + "; did you forget to put the source root containing this very source file to the classpath?",
        url);
    File file = new File(url.toURI());
    assertTrue("Cannot read as file: " + url.toExternalForm(), file.canRead());
    return file;
  }

  /**
   * @param name
   * @return
   */
  protected URL findUrl(String name) {
    ClassLoader classLoader = getClass().getClassLoader();
    return classLoader.getResource(name);
  }

  protected Resource findResourceWithPath(Set<AbstractResource> resources,
      String path) {
    for (Resource r : resources) {
      if (r.getPath().equals(path)) {
        return r;
      }
    }
    return null;
  }

  protected ClassPathEntry getClassPathEntry1AsDirectory()
      throws URISyntaxException {
    File dir = findJarDirectory("com/google/gwt/dev/resource/impl/testdata/cpe1");
    return new ExcludeSvnClassPathEntry(new DirectoryClassPathEntry(dir));
  }

  protected ClassPathEntry getClassPathEntry1AsJar() throws IOException,
      URISyntaxException {
    File file = findFile("com/google/gwt/dev/resource/impl/testdata/cpe1.jar");
    return new ExcludeSvnClassPathEntry(ZipFileClassPathEntry.get(file));
  }
  
  protected ClassPathEntry getClassPathEntry1AsZip() throws IOException, URISyntaxException {
    File file = findFile("com/google/gwt/dev/resource/impl/testdata/cpe1.zip");
    return new ExcludeSvnClassPathEntry(ZipFileClassPathEntry.get(file));
  }

  protected ClassPathEntry getClassPathEntry1AsMock() {
    return new ExcludeSvnClassPathEntry(new MOCK_CPE1());
  }

  protected ClassPathEntry getClassPathEntry2AsDirectory()
      throws URISyntaxException {
    File dir = findJarDirectory("com/google/gwt/dev/resource/impl/testdata/cpe2");
    return new ExcludeSvnClassPathEntry(new DirectoryClassPathEntry(dir));
  }

  protected ClassPathEntry getClassPathEntry2AsJar() throws URISyntaxException,
      IOException {
    File file = findFile("com/google/gwt/dev/resource/impl/testdata/cpe2.jar");
    return new ExcludeSvnClassPathEntry(ZipFileClassPathEntry.get(file));
  }
  
  protected ClassPathEntry getClassPathEntry2AsZip() throws URISyntaxException, IOException {
    File file = findFile("com/google/gwt/dev/resource/impl/testdata/cpe2.zip");
    return new ExcludeSvnClassPathEntry(ZipFileClassPathEntry.get(file));
  }

  protected ClassPathEntry getClassPathEntry2AsMock() {
    return new ExcludeSvnClassPathEntry(new MOCK_CPE2());
  }
}
