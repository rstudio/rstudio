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
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests {@link ResourceOracleImpl}.
 *
 * <pre>
 * Important states to test:
 * - No class path entries
 * - A single class path entries
 *   - init/add/update/remove
 * - Multiple class path entries
 *   - init/add/update/remove
 *   - same as previous with shadowing
 *   - same as previous with superceding
 * </pre>
 */
public class ResourceOracleImplTest extends AbstractResourceOrientedTestBase {

  // Starts empty but can change during tests.
  private static class MOCK_CPE0 extends MockClassPathEntry {
    public MOCK_CPE0() {
      super("/cpe0/");
    }
  }

  private static class MOCK_CPE3 extends MockClassPathEntry {
    public MOCK_CPE3() {
      super("/cpe3/");
    }
  }

  private static class ResourceOracleSnapshot {
    private final Set<String> pathNames;
    private final Map<String, Resource> resourceMap;
    private final Set<Resource> resources;

    public ResourceOracleSnapshot(ResourceOracleImpl oracle) {
      resources = oracle.getResources();
      resourceMap = oracle.getResourceMap();
      pathNames = oracle.getPathNames();
    }

    public void assertCollectionsConsistent(int expectedSize) {
      assertEquals(expectedSize, resources.size());
      assertEquals(resources.size(), resourceMap.size());
      assertEquals(resources.size(), pathNames.size());

      // Ensure every resource is in the map correctly.
      for (Resource r : resources) {
        assertSame(r, resourceMap.get(r.getPath()));
      }

      // Ensure that every resource path is in the set.
      for (Resource r : resources) {
        assertTrue(pathNames.contains(r.getPath()));
      }
    }

    public void assertNotSameCollections(ResourceOracleSnapshot other) {
      assertNotSame(resourceMap, other.resourceMap);
      assertNotSame(resources, other.resources);
      assertNotSame(pathNames, other.pathNames);
    }

    public void assertPathIncluded(String path) {
      assertNotNull(findResourceWithPath(path));
    }

    /**
     * Asserts that a resource having the specified path is present and that it was contributed by
     * the specified classpath entry.
     */
    public void assertPathIncluded(String expectedPath, ClassPathEntry expectedCpe) {
      AbstractResource resource = findResourceWithPath(expectedPath);
      assertNotNull(resource);
      ClassPathEntry actualCpe = resource.getClassPathEntry();
      assertEquals(expectedCpe.getLocation(), actualCpe.getLocation());
    }

    public void assertPathNotIncluded(String path) {
      assertNull(findResourceWithPath(path));
    }

    public void assertSameCollections(ResourceOracleSnapshot other) {
      assertResourcesEqual(resourceMap, other.resourceMap);
      assertResourcesEqual(resources, other.resources);
      assertEquals(pathNames, other.pathNames);
    }

    public AbstractResource findResourceWithPath(String path) {
      for (Resource r : resources) {
        if (r.getPath().equals(path)) {
          return (AbstractResource) r;
        }
      }
      return null;
    }

    @SuppressWarnings("deprecation")
    public void assertResourcesGetURL() throws IOException {
      for (Resource resource : resources) {
        URL url = resource.getURL();
        assertNotNull("Resource " + resource + " had a null getURL()", url);

        InputStream is = resource.openContents();
        assertNotNull(is);

        String urlString = Util.readURLAsString(url);
        String inputString = Util.readStreamAsString(is);
        assertEquals(urlString, inputString);
        assertNotNull(urlString);
        assertNotNull(inputString);
      }
    }
  }

  public static void assertResourcesEqual(Collection<Resource> expected,
      Collection<Resource> actual) {
    Set<String> expectedLocs = new HashSet<String>();
    for (Resource resource : expected) {
      expectedLocs.add(resource.getLocation());
    }
    Set<String> actualLocs = new HashSet<String>();
    for (Resource resource : actual) {
      actualLocs.add(resource.getLocation());
    }
    assertEquals(expectedLocs, actualLocs);
  }

  public static void assertResourcesEqual(Map<String, Resource> expected,
      Map<String, Resource> actual) {
    assertResourcesEqual(expected.values(), actual.values());
  }

  private static PathPrefix makeJavaLangPrefix() {
    return new PathPrefix("java/lang/", null, false);
  }

  private static PathPrefix makeBarPrefix() {
    return new PathPrefix("org/example/bar/client/", null, false);
  }

  private static PathPrefix makeRerootBarPrefix() {
    return new PathPrefix("org/example/bar/client/", null, true);
  }

  private static PathPrefix makeRerootFooPrefix() {
    return new PathPrefix("org/example/foo/client/", null, true);
  }

  private static PathPrefix makeTranslatablePrefix() {
    return new PathPrefix("translatable/", null, true);
  }

  public void testCachingOfJarResources() throws IOException,
      URISyntaxException {
    TreeLogger logger = createTestTreeLogger();
    File jarFile = findFile("com/google/gwt/dev/resource/impl/testdata/cpe1.jar");
    ClassPathEntry cpe1jar = ZipFileClassPathEntry.get(jarFile);

    // test basic caching
    PathPrefixSet pps1 = new PathPrefixSet();
    pps1.add(new PathPrefix("com/google/gwt/", null, false));
    Map<AbstractResource, ResourceResolution> resourceMap1 =
        cpe1jar.findApplicableResources(logger, pps1);
    assertSame(resourceMap1, cpe1jar.findApplicableResources(logger, pps1));

    // test that cache is invalidated if PathPrefixSet is modified.
    pps1.add(new PathPrefix("com/google/gwt/user/", null, false));
    Map<AbstractResource, ResourceResolution> resourceMap2 =
        cpe1jar.findApplicableResources(logger, pps1);
    assertNotSame(resourceMap1, resourceMap2);

    PathPrefixSet pps2 = new PathPrefixSet();
    pps2.add(new PathPrefix("org/example/bar/", null, false));
    Map<AbstractResource, ResourceResolution> resourceMap3 =
        cpe1jar.findApplicableResources(logger, pps2);
    // check that the entry did go in the cache
    assertSame(resourceMap3, cpe1jar.findApplicableResources(logger, pps2));

    // check that the cache does not thrash
    assertSame(resourceMap2, cpe1jar.findApplicableResources(logger, pps1));
    assertSame(resourceMap3, cpe1jar.findApplicableResources(logger, pps2));
  }

  public void testGetUrlOnResources() throws URISyntaxException, IOException {
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe1dir = getClassPathEntry1AsDirectory();
    ClassPathEntry cpe1zip = getClassPathEntry1AsZip();

    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();
    ClassPathEntry cpe2dir = getClassPathEntry2AsDirectory();
    ClassPathEntry cpe2zip = getClassPathEntry2AsZip();

    testGetURLOnResourcesInCPE(cpe1jar);
    testGetURLOnResourcesInCPE(cpe1dir);
    testGetURLOnResourcesInCPE(cpe1zip);
    testGetURLOnResourcesInCPE(cpe2jar);
    testGetURLOnResourcesInCPE(cpe2dir);
    testGetURLOnResourcesInCPE(cpe2zip);
  }

  private void testGetURLOnResourcesInCPE(ClassPathEntry cpe) throws IOException {
    TreeLogger logger = createTestTreeLogger();

    ResourceOracleImpl oracle = createResourceOracle(cpe);
    ResourceOracleSnapshot s = refreshAndSnapshot(logger, oracle);
    s.assertResourcesGetURL();
  }

  /**
   * Test that ResourceOracleImpl preserves the order in which the same logical
   * resource is occurs in multiple ClassPathEntries.
   *
   * @throws URISyntaxException
   * @throws IOException
   */
  public void testClassPathOrderIsHonored() throws IOException,
      URISyntaxException {
    TreeLogger logger = createTestTreeLogger();
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();

    ClassPathEntry[] cp12 = new ClassPathEntry[]{cpe1jar, cpe2jar};
    ClassPathEntry[] cp21 = new ClassPathEntry[]{cpe2jar, cpe1jar};
    String resKeyNormal = "org/example/bar/client/BarClient2.txt";
    String resKeyReroot = "BarClient2.txt";
    testResourceInCPE(logger, resKeyNormal, cpe1jar, cp12, makeBarPrefix());
    testResourceInCPE(logger, resKeyReroot, cpe1jar, cp12,
        makeRerootBarPrefix());
    testResourceInCPE(logger, resKeyNormal, cpe2jar, cp21, makeBarPrefix());
    testResourceInCPE(logger, resKeyReroot, cpe2jar, cp21,
        makeRerootBarPrefix());
  }

  public void testNoClassPathEntries() {
    TreeLogger logger = createTestTreeLogger();
    ResourceOracleImpl oracle = createResourceOracle(new MOCK_CPE0());
    ResourceOracleSnapshot s = refreshAndSnapshot(logger, oracle);
    s.assertCollectionsConsistent(0);
  }

  /**
   * Test that ResourceOracleImpl prefers the order of path prefixes over
   * ClassPathEntries.
   * <p>
   * cpe1 contains org/example/bar/client/BarClient1.txt and cpe2 contains
   * org/example/foo/client/BarClient1.txt
   *
   * @throws URISyntaxException
   * @throws IOException
   */
  public void testPathPrefixOrderPreferredOverClasspath() throws IOException,
      URISyntaxException {
    TreeLogger logger = createTestTreeLogger();
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();

    ClassPathEntry[] cp12 = new ClassPathEntry[]{cpe1jar, cpe2jar};
    ClassPathEntry[] cp21 = new ClassPathEntry[]{cpe2jar, cpe1jar};

    String keyReroot = "BarClient1.txt";

    // Resource in cpe2 wins because pp2 comes later.
    testResourceInCPE(logger, keyReroot, cpe2jar, cp12, makeRerootBarPrefix(),
        makeRerootFooPrefix());
    // Order of specifying classpath is reversed, it still matches cpe2.
    testResourceInCPE(logger, keyReroot, cpe2jar, cp21, makeRerootBarPrefix(),
        makeRerootFooPrefix());

    // Resource in cpe1 wins because pp1 comes later.
    testResourceInCPE(logger, keyReroot, cpe1jar, cp12, makeRerootFooPrefix(),
        makeRerootBarPrefix());
    // Order of specifying classpath is reversed, it still matches cpe1.
    testResourceInCPE(logger, keyReroot, cpe1jar, cp21, makeRerootFooPrefix(),
        makeRerootBarPrefix());
  }

  /**
   * Tests the actual reading of resources.
   *
   * @throws URISyntaxException
   * @throws IOException
   */
  public void testReadingResource() throws IOException, URISyntaxException {
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe1dir = getClassPathEntry1AsDirectory();
    ClassPathEntry cpe1zip = getClassPathEntry1AsZip();

    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();
    ClassPathEntry cpe2dir = getClassPathEntry2AsDirectory();
    ClassPathEntry cpe2zip = getClassPathEntry2AsZip();

    testReadingResource(cpe1jar, cpe2jar);
    testReadingResource(cpe1dir, cpe2jar);
    testReadingResource(cpe1zip, cpe2jar);

    testReadingResource(cpe1jar, cpe2dir);
    testReadingResource(cpe1dir, cpe2dir);
    testReadingResource(cpe1zip, cpe2dir);

    testReadingResource(cpe1jar, cpe2zip);
    testReadingResource(cpe1dir, cpe2zip);
    testReadingResource(cpe1zip, cpe2zip);
  }

  /**
   * Verify that duplicate entries are removed from the classpath, and that
   * multiple ResourceOracleImpls created from the same classloader return the
   * same list of ClassPathEntries.
   */
  public void testRemoveDuplicates() {
    TreeLogger logger = createTestTreeLogger();
    URL cpe1 = findUrl("com/google/gwt/dev/resource/impl/testdata/cpe1.jar");
    URL cpe2 = findUrl("com/google/gwt/dev/resource/impl/testdata/cpe2.zip");
    URLClassLoader classLoader = new URLClassLoader(new URL[]{
        cpe1, cpe2, cpe2, cpe1, cpe2,}, null);
    ResourceOracleImpl oracle = new ResourceOracleImpl(logger, classLoader);
    List<ClassPathEntry> classPathEntries = oracle.getClassPathEntries();
    assertEquals(2, classPathEntries.size());
    assertJarEntry(classPathEntries.get(0), "cpe1.jar");
    assertJarEntry(classPathEntries.get(1), "cpe2.zip");
    oracle = new ResourceOracleImpl(logger, classLoader);
    List<ClassPathEntry> classPathEntries2 = oracle.getClassPathEntries();
    assertEquals(2, classPathEntries2.size());
    for (int i = 0; i < 2; ++i) {
      assertSame(classPathEntries.get(i), classPathEntries2.get(i));
    }
  }

  public void testResourceAddition() throws IOException, URISyntaxException {
    ClassPathEntry cpe1mock = getClassPathEntry1AsMock();
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe1dir = getClassPathEntry1AsDirectory();
    ClassPathEntry cpe1zip = getClassPathEntry1AsZip();

    ClassPathEntry cpe2mock = getClassPathEntry2AsMock();
    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();
    ClassPathEntry cpe2dir = getClassPathEntry2AsDirectory();
    ClassPathEntry cpe2zip = getClassPathEntry2AsZip();

    testResourceAddition(cpe1jar, cpe2jar);
    testResourceAddition(cpe1dir, cpe2jar);
    testResourceAddition(cpe1mock, cpe2jar);
    testResourceAddition(cpe1zip, cpe2jar);

    testResourceAddition(cpe1jar, cpe2dir);
    testResourceAddition(cpe1dir, cpe2dir);
    testResourceAddition(cpe1mock, cpe2dir);
    testResourceAddition(cpe1zip, cpe2dir);

    testResourceAddition(cpe1jar, cpe2mock);
    testResourceAddition(cpe1dir, cpe2mock);
    testResourceAddition(cpe1mock, cpe2mock);
    testResourceAddition(cpe1zip, cpe2mock);

    testResourceAddition(cpe1jar, cpe2zip);
    testResourceAddition(cpe1dir, cpe2zip);
    testResourceAddition(cpe1mock, cpe2zip);
    testResourceAddition(cpe1zip, cpe2zip);
  }

  public void testResourceDeletion() throws IOException, URISyntaxException {
    ClassPathEntry cpe1mock = getClassPathEntry1AsMock();
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe1dir = getClassPathEntry1AsDirectory();
    ClassPathEntry cpe1zip = getClassPathEntry1AsZip();

    ClassPathEntry cpe2mock = getClassPathEntry2AsMock();
    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();
    ClassPathEntry cpe2dir = getClassPathEntry2AsDirectory();
    ClassPathEntry cpe2zip = getClassPathEntry2AsZip();

    testResourceDeletion(cpe1jar, cpe2jar);
    testResourceDeletion(cpe1dir, cpe2jar);
    testResourceDeletion(cpe1mock, cpe2jar);
    testResourceDeletion(cpe1zip, cpe2jar);

    testResourceDeletion(cpe1jar, cpe2dir);
    testResourceDeletion(cpe1dir, cpe2dir);
    testResourceDeletion(cpe1mock, cpe2dir);
    testResourceDeletion(cpe1zip, cpe2dir);

    testResourceDeletion(cpe1jar, cpe2mock);
    testResourceDeletion(cpe1dir, cpe2mock);
    testResourceDeletion(cpe1mock, cpe2mock);
    testResourceDeletion(cpe1zip, cpe2mock);

    testResourceDeletion(cpe1jar, cpe2zip);
    testResourceDeletion(cpe1dir, cpe2zip);
    testResourceDeletion(cpe1mock, cpe2zip);
    testResourceDeletion(cpe1zip, cpe2zip);
  }

  public void testResourceModification() throws IOException, URISyntaxException {
    ClassPathEntry cpe1mock = getClassPathEntry1AsMock();
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe1dir = getClassPathEntry1AsDirectory();
    ClassPathEntry cpe1zip = getClassPathEntry1AsZip();

    ClassPathEntry cpe2mock = getClassPathEntry2AsMock();
    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();
    ClassPathEntry cpe2dir = getClassPathEntry2AsDirectory();
    ClassPathEntry cpe2zip = getClassPathEntry2AsZip();

    testResourceModification(cpe1jar, cpe2jar);
    testResourceModification(cpe1dir, cpe2jar);
    testResourceModification(cpe1mock, cpe2jar);
    testResourceModification(cpe1zip, cpe2jar);

    testResourceModification(cpe1jar, cpe2dir);
    testResourceModification(cpe1dir, cpe2dir);
    testResourceModification(cpe1mock, cpe2dir);
    testResourceModification(cpe1zip, cpe2dir);

    testResourceModification(cpe1jar, cpe2mock);
    testResourceModification(cpe1dir, cpe2mock);
    testResourceModification(cpe1mock, cpe2mock);
    testResourceModification(cpe1zip, cpe2mock);

    testResourceModification(cpe1jar, cpe2zip);
    testResourceModification(cpe1dir, cpe2zip);
    testResourceModification(cpe1mock, cpe2zip);
    testResourceModification(cpe1zip, cpe2zip);

    /*
     * TODO(bruce): figure out a good way to test real resource modifications of
     * jar files and directories
     */
  }

  /**
   * Ensure refresh is stable when multiple classpaths + multiple path prefixes
   * all include the same resource.
   */
  public void testStableRefreshOnBlot() {
    TreeLogger logger = createTestTreeLogger();
    ClassPathEntry cpe1 = getClassPathEntry1AsMock();
    ClassPathEntry cpe2 = getClassPathEntry2AsMock();
    testResourceInCPE(logger, "org/example/bar/client/BarClient2.txt", cpe1,
        new ClassPathEntry[]{cpe1, cpe2}, makeBarPrefix(), new PathPrefix(
            "org/example/bar/", null, false));
  }

  public void testOverlappingIncludeWarning() {
    DefaultFilters defaultFilters = new DefaultFilters();

    UnitTestTreeLogger.Builder loggerBuilder = new UnitTestTreeLogger.Builder();
    loggerBuilder.setLowestLogLevel(TreeLogger.WARN);
    loggerBuilder.expectWarn(
        "Resource com/google/gwt/dev/resource/impl/testdata/outer/inner/"
        + "empty.txt is included by multiple modules (InnerDirModule, "
        + "InnerFileModule, OuterDirModule).", null);
    UnitTestTreeLogger logger = loggerBuilder.createLogger();
    ResourceOracleImpl resourceOracleImpl = new ResourceOracleImpl(logger,
        ResourceLoaders.wrap(Thread.currentThread().getContextClassLoader()));

    PathPrefixSet pathPrefixSet = new PathPrefixSet();
    // Include from an outer directory.
    pathPrefixSet.add(new PathPrefix("OuterDirModule",
        "com/google/gwt/dev/resource/impl/testdata/outer/",
        defaultFilters.customResourceFilter(new String[0], new String[0],
            new String[0], true, true), false, new String[0]));
    // Include on the inner directory.
    pathPrefixSet.add(
        new PathPrefix("InnerDirModule",
            "com/google/gwt/dev/resource/impl/testdata/outer/inner/",
            defaultFilters.customResourceFilter(new String[0], new String[0],
                new String[0], true, true), false, new String[0]));
    // Include a specific file in the inner directory.
    pathPrefixSet.add(new PathPrefix("InnerFileModule",
        "com/google/gwt/dev/resource/impl/testdata/",
        defaultFilters.customResourceFilter(new String[] {
            "com/google/gwt/dev/resource/impl/testdata/outer/inner/empty.txt"},
            new String[0], new String[0], true, true), false, new String[0]));

    resourceOracleImpl.setPathPrefixes(pathPrefixSet);
    resourceOracleImpl.scanResources(logger);
    resourceOracleImpl.printOverlappingModuleIncludeWarnings(logger);

    logger.assertCorrectLogEntries();
  }

  /**
   * Ensure refresh is stable when multiple classpaths + multiple path prefixes
   * all include the same resource.
   */
  public void testSuperSourceSupercedesSource() {
    TreeLogger logger = createTestTreeLogger();

    MockClassPathEntry cpe1 = new MockClassPathEntry("/cpe1/");
    cpe1.addResource("java/lang/Object.java");

    MockClassPathEntry cpe2 = new MockClassPathEntry("/cpe2/");
    cpe2.addResource("translatable/java/lang/Object.java");

    // Ensure the translatable overrides the basic despite swapping CPE order.
    testResourceInCPE(logger, "java/lang/Object.java", cpe2,
        new ClassPathEntry[]{cpe1, cpe2}, makeJavaLangPrefix(),
        makeTranslatablePrefix());
    testResourceInCPE(logger, "java/lang/Object.java", cpe2,
        new ClassPathEntry[]{cpe2, cpe1}, makeJavaLangPrefix(),
        makeTranslatablePrefix());

    // Ensure the translatable overrides the basic despite swapping PPS order.
    testResourceInCPE(logger, "java/lang/Object.java", cpe2,
        new ClassPathEntry[]{cpe1, cpe2}, makeTranslatablePrefix(),
        makeJavaLangPrefix());
    testResourceInCPE(logger, "java/lang/Object.java", cpe2,
        new ClassPathEntry[]{cpe2, cpe1}, makeTranslatablePrefix(),
        makeJavaLangPrefix());
  }

  private void assertJarEntry(ClassPathEntry classPathEntry, String expected) {
    assertTrue("Should be instance of ZipFileClassPathEntry",
        classPathEntry instanceof ZipFileClassPathEntry);
    ZipFileClassPathEntry zipCPE = (ZipFileClassPathEntry) classPathEntry;
    String jar = zipCPE.getLocation();
    assertTrue("URL should contain " + expected, jar.contains(expected));
  }

  /**
   * Creates an array of class path entries, setting up each one with a
   * well-known set of client prefixes.
   */
  private ResourceOracleImpl createResourceOracle(ClassPathEntry... entries) {
    PathPrefixSet pps = new PathPrefixSet();
    pps.add(new PathPrefix("com/google/gwt/user/client/", null));
    pps.add(new PathPrefix("org/example/bar/client/", null));
    pps.add(new PathPrefix("org/example/foo/client/", null));
    pps.add(new PathPrefix("com/google/gwt/i18n/client/", null));

    List<ClassPathEntry> classPath = new ArrayList<ClassPathEntry>();
    for (ClassPathEntry entry : entries) {
      classPath.add(entry);
    }
    ResourceOracleImpl oracle = new ResourceOracleImpl(classPath);
    oracle.setPathPrefixes(pps);
    return oracle;
  }

  private ResourceOracleSnapshot refreshAndSnapshot(TreeLogger logger,
      ResourceOracleImpl oracle) {
    oracle.scanResources(logger);
    ResourceOracleSnapshot snapshot1 = new ResourceOracleSnapshot(oracle);
    oracle.scanResources(logger);
    ResourceOracleSnapshot snapshot2 = new ResourceOracleSnapshot(oracle);
    snapshot1.assertSameCollections(snapshot2);
    return snapshot1;
  }

  private void testReadingResource(ClassPathEntry cpe1, ClassPathEntry cpe2)
      throws IOException {
    TreeLogger logger = createTestTreeLogger();

    ResourceOracleImpl oracle = createResourceOracle(cpe1, cpe2);
    ResourceOracleSnapshot s = refreshAndSnapshot(logger, oracle);
    s.assertCollectionsConsistent(10);
    s.assertPathIncluded("com/google/gwt/user/client/Command.java", cpe1);
    s.assertPathIncluded("com/google/gwt/i18n/client/Messages.java", cpe2);

    {
      /*
       * Read a resource in cpe1.
       */
      AbstractResource res = s.findResourceWithPath("com/google/gwt/user/client/Command.java");
      BufferedReader rdr = null;
      try {
        InputStream is = res.openContents();
        assertNotNull(is);
        rdr = new BufferedReader(new InputStreamReader(is));

        // Skip lines until package line is found.
        String line = rdr.readLine();
        while (line != null && !line.startsWith("package")) {
          line = rdr.readLine();
        }
        assertTrue(line != null && line.indexOf(
            "package com.google.gwt.dev.resource.impl.testdata.cpe1.com.google.gwt.user.client;") >= 0);
      } finally {
        Utility.close(rdr);
      }
    }

    {
      /*
       * Read a resource in cpe2.
       */
      AbstractResource res = s.findResourceWithPath("com/google/gwt/i18n/client/Messages.java");
      BufferedReader rdr = null;
      try {
        InputStream is = res.openContents();
        assertNotNull(is);
        rdr = new BufferedReader(new InputStreamReader(is));
        // Skip lines until package line is found.
        String line = rdr.readLine();
        while (line != null && !line.startsWith("package")) {
          line = rdr.readLine();
        }
        assertTrue(line != null && line.indexOf(
            "package com.google.gwt.dev.resource.impl.testdata.cpe2.com.google.gwt.i18n.client;") >= 0);
      } finally {
        Utility.close(rdr);
      }
    }

    {
      /*
       * TODO: Try to read an invalid resource and watch it fail as intended.
       */
    }
  }

  private void testResourceAddition(ClassPathEntry cpe1, ClassPathEntry cpe2) {
    TreeLogger logger = createTestTreeLogger();

    MOCK_CPE0 cpe0 = new MOCK_CPE0();
    MOCK_CPE3 cpe3 = new MOCK_CPE3();
    ResourceOracleImpl oracle = createResourceOracle(cpe0, cpe1, cpe2, cpe3);

    {
      /*
       * Ensure it's correct as a baseline. These tests have hard-coded
       * assumptions about the contents of each classpath entry.
       */
      ResourceOracleSnapshot s = refreshAndSnapshot(logger, oracle);
      s.assertCollectionsConsistent(10);
      s.assertPathIncluded("com/google/gwt/user/client/Command.java");
      s.assertPathIncluded("com/google/gwt/user/client/Timer.java");
      s.assertPathIncluded("com/google/gwt/user/client/ui/Widget.java");
      s.assertPathIncluded("org/example/bar/client/BarClient1.txt");
      s.assertPathIncluded("org/example/bar/client/BarClient2.txt");
      s.assertPathIncluded("org/example/bar/client/BarClient3.txt");
      s.assertPathIncluded("org/example/bar/client/etc/BarEtc.txt");
      s.assertPathIncluded("org/example/foo/client/FooClient.java");
      s.assertPathIncluded("com/google/gwt/i18n/client/Messages.java");
    }

    {
      /*
       * Add duplicate resources later in the classpath, which won't be found.
       * Consequently, the collections' identities should not change.
       */
      cpe3.addResource("com/google/gwt/user/client/Command.java");
      cpe3.addResource("com/google/gwt/user/client/Timer.java");
      cpe3.addResource("com/google/gwt/bar/client/etc/BarEtc.txt");

      ResourceOracleSnapshot before = new ResourceOracleSnapshot(oracle);
      ResourceOracleSnapshot after = refreshAndSnapshot(logger, oracle);
      after.assertCollectionsConsistent(10);
      after.assertSameCollections(before);
    }

    {
      /*
       * Add a unique resource later in the classpath, which will be found.
       * Consequently, the collections' identities should change.
       */
      cpe3.addResource("com/google/gwt/i18n/client/Constants.java");

      ResourceOracleSnapshot before = new ResourceOracleSnapshot(oracle);
      ResourceOracleSnapshot after = refreshAndSnapshot(logger, oracle);
      after.assertCollectionsConsistent(11);
      after.assertNotSameCollections(before);
      after.assertPathIncluded("com/google/gwt/i18n/client/Constants.java");
    }
  }

  private void testResourceDeletion(ClassPathEntry cpe1, ClassPathEntry cpe2) {
    TreeLogger logger = createTestTreeLogger();

    MOCK_CPE0 cpe0 = new MOCK_CPE0();
    MOCK_CPE3 cpe3 = new MOCK_CPE3();
    ResourceOracleImpl oracle = createResourceOracle(cpe0, cpe1, cpe2, cpe3);

    /*
     * Intentionally add some duplicate resources.
     */
    cpe0.addResource("com/google/gwt/user/client/Command.java");
    cpe0.addResource("com/google/gwt/i18n/client/Constants.java");
    cpe3.addResource("com/google/gwt/user/client/Command.java");
    cpe3.addResource("com/google/gwt/i18n/client/Constants.java");

    {
      /*
       * Ensure it's correct as a baseline. These tests have hard-coded
       * assumptions about the contents of each classpath entry.
       */
      ResourceOracleSnapshot s = refreshAndSnapshot(logger, oracle);
      s.assertCollectionsConsistent(11);
      s.assertPathIncluded("com/google/gwt/user/client/Command.java");
      s.assertPathIncluded("com/google/gwt/user/client/Timer.java");
      s.assertPathIncluded("com/google/gwt/user/client/ui/Widget.java");
      s.assertPathIncluded("org/example/bar/client/BarClient1.txt");
      s.assertPathIncluded("org/example/bar/client/BarClient2.txt");
      s.assertPathIncluded("org/example/bar/client/BarClient3.txt");
      s.assertPathIncluded("org/example/bar/client/etc/BarEtc.txt");
      s.assertPathIncluded("org/example/foo/client/FooClient.java");
      s.assertPathIncluded("com/google/gwt/i18n/client/Messages.java");
      s.assertPathIncluded("com/google/gwt/i18n/client/Constants.java");
    }

    {
      /*
       * Remove a shadowed resource, which shouldn't have been found anyway.
       * Consequently, the collections' identities should not change.
       */
      cpe3.removeResource("com/google/gwt/user/client/Command.java");
      cpe3.removeResource("com/google/gwt/i18n/client/Constants.java");

      ResourceOracleSnapshot before = new ResourceOracleSnapshot(oracle);
      ResourceOracleSnapshot after = refreshAndSnapshot(logger, oracle);
      after.assertCollectionsConsistent(11);
      after.assertSameCollections(before);
    }

    {
      /*
       * Remove a unique resource, which will no longer be found. We also add a
       * new one to ensure that lack of size change doesn't confuse anything.
       * Consequently, the collections' identities should change.
       */
      cpe0.removeResource("com/google/gwt/i18n/client/Constants.java");
      cpe3.addResource("com/google/gwt/user/client/Window.java");

      ResourceOracleSnapshot before = new ResourceOracleSnapshot(oracle);
      ResourceOracleSnapshot after = refreshAndSnapshot(logger, oracle);
      after.assertCollectionsConsistent(11);
      after.assertNotSameCollections(before);
      after.assertPathIncluded("com/google/gwt/user/client/Window.java");
      after.assertPathNotIncluded("com/google/gwt/i18n/client/Constants.java");
    }
  }

  private void testResourceInCPE(TreeLogger logger, String resourceKey,
      ClassPathEntry expectedCPE, ClassPathEntry[] classPath,
      PathPrefix... pathPrefixes) {
    ResourceOracleImpl oracle = new ResourceOracleImpl(Arrays.asList(classPath));
    PathPrefixSet pps = new PathPrefixSet();
    for (PathPrefix pathPrefix : pathPrefixes) {
      pps.add(pathPrefix);
    }
    oracle.setPathPrefixes(pps);
    ResourceOracleSnapshot s = refreshAndSnapshot(logger, oracle);
    s.assertPathIncluded(resourceKey, expectedCPE);
  }

  private void testResourceModification(ClassPathEntry cpe1, ClassPathEntry cpe2) {
    TreeLogger logger = createTestTreeLogger();

    MOCK_CPE0 cpe0 = new MOCK_CPE0();
    MOCK_CPE3 cpe3 = new MOCK_CPE3();
    ResourceOracleImpl oracle = createResourceOracle(cpe0, cpe1, cpe2, cpe3);

    {
      /*
       * Baseline assumptions about the set of resources present by default.
       */
      ResourceOracleSnapshot s = refreshAndSnapshot(logger, oracle);
      s.assertPathIncluded("com/google/gwt/user/client/Command.java", cpe1);
      s.assertPathIncluded("com/google/gwt/user/client/Timer.java", cpe1);
    }

    // Add intentionally duplicate resources.
    cpe0.addResource("com/google/gwt/user/client/Command.java");
    cpe3.addResource("com/google/gwt/user/client/Timer.java");

    {
      /*
       * Ensure that the dups have the effect we expect.
       */
      ResourceOracleSnapshot s = refreshAndSnapshot(logger, oracle);
      s.assertPathIncluded("com/google/gwt/user/client/Command.java", cpe0);
      s.assertPathIncluded("com/google/gwt/user/client/Timer.java", cpe1);
    }

    {
      /*
       * Change a mock resource that was shadowed to ensure that the change
       * isn't observed.
       */
      ResourceOracleSnapshot before = new ResourceOracleSnapshot(oracle);
      before.assertCollectionsConsistent(10);

      cpe3.updateResource("com/google/gwt/user/client/Timer.java");

      ResourceOracleSnapshot after = refreshAndSnapshot(logger, oracle);
      after.assertSameCollections(before);
    }

    {
      /*
       * Change a mock resource that was not shadowed to ensure that the change
       * is observed.
       */
      ResourceOracleSnapshot before = new ResourceOracleSnapshot(oracle);
      before.assertCollectionsConsistent(10);

      cpe0.updateResource("com/google/gwt/user/client/Command.java");

      ResourceOracleSnapshot after = refreshAndSnapshot(logger, oracle);
      after.assertNotSameCollections(before);
    }
  }
}
