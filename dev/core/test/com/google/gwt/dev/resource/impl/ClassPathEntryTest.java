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
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource related tests.
 */
public class ClassPathEntryTest extends AbstractResourceOrientedTestBase {

  /**
   * This test will likely not work on Windows since directories that start with . are not
   * implicitly hidden there. But since Java 6 does not have a File.setHidden() function, fixing
   * this test for Windows is deferred till GWT officially depends on Java 7.
   */
  public void testIgnoresHiddenDirectories() throws IOException {
    // Setup a /tmp/.svn/ShouldNotBeFound.java folder structure.
    File tempDir = Files.createTempDir();
    File nestedHiddenDir = new File(tempDir, ".svn");
    nestedHiddenDir.mkdir();
    File javaFile = new File(nestedHiddenDir, "ShouldNotBeFound.java");
    javaFile.createNewFile();

    // Prepare a place to record findings.
    List<Map<AbstractResource, ResourceResolution>> foundFiles =
        Lists.<Map<AbstractResource, ResourceResolution>> newArrayList();
    foundFiles.add(Maps.<AbstractResource, ResourceResolution> newHashMap());

    // Perform a class path directory inspection.
    DirectoryClassPathEntry cpe = new DirectoryClassPathEntry(tempDir);
    cpe.descendToFindResources(TreeLogger.NULL, Lists.newArrayList(createInclusivePathPrefixSet()),
        foundFiles, tempDir, "");

    // Verify that even though we're using an ALL filter, we still didn't find any files inside the
    // .svn dir, because we never even enumerate its contents.
    assertTrue(foundFiles.get(0).isEmpty());
  }

  public void testResourceCreated() throws IOException, InterruptedException {
    // With just 1 filter definition.
    testResourceCreated(Lists.newArrayList(createInclusivePathPrefixSet()));
    // With multiple filter definitions.
    testResourceCreated(Lists.newArrayList(createInclusivePathPrefixSet(),
        createInclusivePathPrefixSet(), createInclusivePathPrefixSet()));
  }

  public void testForResourceListenerLeaks() throws IOException, InterruptedException {
    // Create a folder an initially empty folder.
    PathPrefixSet pathPrefixSet = createInclusivePathPrefixSet();
    DirectoryClassPathEntry classPathEntry = new DirectoryClassPathEntry(Files.createTempDir());

    // Show that the WeakDirectoryNotifier is not listening for any updates.
    awaitFullGc();
    assertEquals(0, DirectoryPathPrefixChangeManager.getActiveListenerCount());

    // Start listening for updates.
    DirectoryPathPrefixChangeManager.ensureListening(classPathEntry, pathPrefixSet);

    // Show that the WeakDirectoryNotifier is now listening for updates.
    awaitFullGc();
    assertEquals(1, DirectoryPathPrefixChangeManager.getActiveListenerCount());

    // Dereference the classpath entry and pathprefixset and give the garbage collector an
    // opportunity to clear any weak references.
    pathPrefixSet = null;
    classPathEntry = null;

    // Show that the WeakDirectoryNotifier is no longer listening for updates.
    awaitFullGc();
    assertEquals(0, DirectoryPathPrefixChangeManager.getActiveListenerCount());
  }

  public void testResourceCreated(Collection<PathPrefixSet> pathPrefixSets) throws IOException,
      InterruptedException {
    // Create a folder an initially empty folder.
    File tempDir = Files.createTempDir();
    DirectoryClassPathEntry cpe = new DirectoryClassPathEntry(tempDir);

    // Perform a class path directory inspection.
    for (PathPrefixSet pathPrefixSet : pathPrefixSets) {
      Map<AbstractResource, ResourceResolution> foundResources =
          cpe.findApplicableResources(TreeLogger.NULL, pathPrefixSet);

      // Verify the directory is initially empty.
      assertTrue(foundResources.isEmpty());
    }

    // Create a file and give file events time to fire.
    File createdFile = new File(tempDir, "Created.java");
    createdFile.createNewFile();
    Thread.sleep(10);

    // Perform a class path directory inspection.
    for (PathPrefixSet pathPrefixSet : pathPrefixSets) {
      Map<AbstractResource, ResourceResolution> foundResources =
          cpe.findApplicableResources(TreeLogger.NULL, pathPrefixSet);

      // Verify the directory is no longer empty.
      assertEquals(1, foundResources.size());
      assertEquals("Created.java", foundResources.keySet().iterator().next().getPath());
    }
  }

  public void testResourceDeleted() throws IOException, InterruptedException {
    // With just 1 filter definition.
    testResourceDeleted(Lists.newArrayList(createInclusivePathPrefixSet()));
    // With multiple filter definitions.
    testResourceDeleted(Lists.newArrayList(createInclusivePathPrefixSet(),
        createInclusivePathPrefixSet(), createInclusivePathPrefixSet()));
  }

  private void testResourceDeleted(Collection<PathPrefixSet> pathPrefixSets) throws IOException,
      InterruptedException {
    // Create a folder with one initial file, that can be deleted.
    File tempDir = Files.createTempDir();
    File fileToDelete = new File(tempDir, "ToDelete.java");
    fileToDelete.createNewFile();
    DirectoryClassPathEntry cpe = new DirectoryClassPathEntry(tempDir);

    // Perform a class path directory inspection.
    for (PathPrefixSet pathPrefixSet : pathPrefixSets) {
      Map<AbstractResource, ResourceResolution> foundResources =
          cpe.findApplicableResources(TreeLogger.NULL, pathPrefixSet);

      // Verify the directory is not initially empty.
      assertEquals(1, foundResources.size());
      assertEquals("ToDelete.java", foundResources.keySet().iterator().next().getPath());
    }

    // Delete the file and give file events time to fire.
    fileToDelete.delete();
    Thread.sleep(10);

    // Perform a class path directory inspection.
    for (PathPrefixSet pathPrefixSet : pathPrefixSets) {
      Map<AbstractResource, ResourceResolution> foundResources =
          cpe.findApplicableResources(TreeLogger.NULL, pathPrefixSet);

      // Verify the directory is now empty.
      assertTrue(foundResources.isEmpty());
    }
  }

  public void testResourceRenamed() throws IOException, InterruptedException {
    // With just 1 filter definition.
    testResourceRenamed(Lists.newArrayList(createInclusivePathPrefixSet()));
    // With multiple filter definitions.
    testResourceRenamed(Lists.newArrayList(createInclusivePathPrefixSet(),
        createInclusivePathPrefixSet(), createInclusivePathPrefixSet()));
  }

  private void testResourceRenamed(Collection<PathPrefixSet> pathPrefixSets) throws IOException,
      InterruptedException {
    // Create a folder with one initial file, that can be renamed.
    File tempDir = Files.createTempDir();
    File fileToRename = new File(tempDir, "ToRename.java");
    fileToRename.createNewFile();
    DirectoryClassPathEntry cpe = new DirectoryClassPathEntry(tempDir);

    // Perform class path directory inspections.
    for (PathPrefixSet pathPrefixSet : pathPrefixSets) {
      Map<AbstractResource, ResourceResolution> foundResources =
          cpe.findApplicableResources(TreeLogger.NULL, pathPrefixSet);

      // Verify the directory is not initially empty.
      assertEquals(1, foundResources.size());
      assertEquals("ToRename.java", foundResources.keySet().iterator().next().getPath());
    }

    // Rename the file and give file events time to fire.
    fileToRename.renameTo(new File(tempDir, "Renamed.java"));
    Thread.sleep(10);

    for (PathPrefixSet pathPrefixSet : pathPrefixSets) {
      // Perform a class path directory inspection.
      Map<AbstractResource, ResourceResolution> foundResources =
          cpe.findApplicableResources(TreeLogger.NULL, pathPrefixSet);

      // Verify the file is seen as renamed.
      assertEquals(1, foundResources.size());
      assertEquals("Renamed.java", foundResources.keySet().iterator().next().getPath());
    }
  }

  public void testAllCpe1FilesFound() throws URISyntaxException, IOException {
    testAllCpe1FilesFound(getClassPathEntry1AsJar());
    testAllCpe1FilesFound(getClassPathEntry1AsDirectory());
    testAllCpe1FilesFound(getClassPathEntry1AsZip());
  }

  public void testAllCpe2FilesFound() throws URISyntaxException, IOException {
    testAllCpe2FilesFound(getClassPathEntry2AsJar());
    testAllCpe2FilesFound(getClassPathEntry2AsDirectory());
    testAllCpe2FilesFound(getClassPathEntry2AsZip());
  }

  public void testPathPrefixSetChanges() throws IOException, URISyntaxException {
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe1dir = getClassPathEntry1AsDirectory();
    ClassPathEntry cpe1zip = getClassPathEntry1AsZip();
    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();
    ClassPathEntry cpe2dir = getClassPathEntry2AsDirectory();
    ClassPathEntry cpe2zip = getClassPathEntry2AsZip();

    testPathPrefixSetChanges(cpe1jar, cpe2jar);
    testPathPrefixSetChanges(cpe1dir, cpe2jar);
    testPathPrefixSetChanges(cpe1zip, cpe2jar);

    testPathPrefixSetChanges(cpe1dir, cpe2dir);
    testPathPrefixSetChanges(cpe1jar, cpe2dir);
    testPathPrefixSetChanges(cpe1zip, cpe2dir);

    testPathPrefixSetChanges(cpe1dir, cpe2zip);
    testPathPrefixSetChanges(cpe1jar, cpe2zip);
    testPathPrefixSetChanges(cpe1zip, cpe2zip);
  }

  public void testUseOfPrefixesWithFiltering() throws IOException,
      URISyntaxException {
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe1dir = getClassPathEntry1AsDirectory();
    ClassPathEntry cpe1zip = getClassPathEntry1AsZip();
    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();
    ClassPathEntry cpe2dir = getClassPathEntry2AsDirectory();
    ClassPathEntry cpe2zip = getClassPathEntry2AsZip();

    testUseOfPrefixesWithFiltering(cpe1dir, cpe2jar);
    testUseOfPrefixesWithFiltering(cpe1jar, cpe2jar);
    testUseOfPrefixesWithFiltering(cpe1dir, cpe2jar);
    testUseOfPrefixesWithFiltering(cpe1zip, cpe2jar);

    testUseOfPrefixesWithFiltering(cpe1dir, cpe2dir);
    testUseOfPrefixesWithFiltering(cpe1jar, cpe2dir);
    testUseOfPrefixesWithFiltering(cpe1zip, cpe2dir);

    testUseOfPrefixesWithFiltering(cpe1dir, cpe2zip);
    testUseOfPrefixesWithFiltering(cpe1jar, cpe2zip);
    testUseOfPrefixesWithFiltering(cpe1zip, cpe2zip);
  }

  public void testUseOfPrefixesWithoutFiltering() throws URISyntaxException,
      IOException {
    ClassPathEntry cpe1jar = getClassPathEntry1AsJar();
    ClassPathEntry cpe1dir = getClassPathEntry1AsDirectory();
    ClassPathEntry cpe1zip = getClassPathEntry1AsZip();
    ClassPathEntry cpe2jar = getClassPathEntry2AsJar();
    ClassPathEntry cpe2dir = getClassPathEntry2AsDirectory();
    ClassPathEntry cpe2zip = getClassPathEntry2AsZip();

    testUseOfPrefixesWithoutFiltering(cpe1dir, cpe2jar);
    testUseOfPrefixesWithoutFiltering(cpe1jar, cpe2jar);
    testUseOfPrefixesWithoutFiltering(cpe1dir, cpe2jar);
    testUseOfPrefixesWithoutFiltering(cpe1zip, cpe2jar);

    testUseOfPrefixesWithoutFiltering(cpe1dir, cpe2dir);
    testUseOfPrefixesWithoutFiltering(cpe1jar, cpe2dir);
    testUseOfPrefixesWithoutFiltering(cpe1zip, cpe2dir);

    testUseOfPrefixesWithoutFiltering(cpe1dir, cpe2zip);
    testUseOfPrefixesWithoutFiltering(cpe1jar, cpe2zip);
    testUseOfPrefixesWithoutFiltering(cpe1zip, cpe2zip);
  }

  public void testUseOfPrefixesWithoutFiltering(ClassPathEntry cpe1,
      ClassPathEntry cpe2) {
    TreeLogger logger = createTestTreeLogger();

    PathPrefixSet pps = new PathPrefixSet();
    pps.add(new PathPrefix("com/google/gwt/user/client/", null));
    pps.add(new PathPrefix("com/google/gwt/i18n/client/", null));

    {
      // Examine cpe1.
      Set<AbstractResource> r = cpe1.findApplicableResources(logger, pps).keySet();

      assertEquals(3, r.size());
      assertPathIncluded(r, "com/google/gwt/user/client/Command.java");
      assertPathIncluded(r, "com/google/gwt/user/client/Timer.java");
      assertPathIncluded(r, "com/google/gwt/user/client/ui/Widget.java");
    }

    {
      // Examine cpe2.
      Set<AbstractResource> r = cpe2.findApplicableResources(logger, pps).keySet();

      assertEquals(1, r.size());
      assertPathIncluded(r, "com/google/gwt/i18n/client/Messages.java");
    }
  }

  private static void awaitFullGc() throws InterruptedException {
    Object object = new Object();
    WeakReference<Object> objectReference = new WeakReference<Object>(object);
    object = null;
    System.gc();

    while (objectReference.get() != null) {
      Thread.sleep(10);
      System.gc();
    }
  }

  // NOTE: if this test fails, ensure that the source root containing this very
  // source file is *FIRST* on the classpath
  private void testAllCpe1FilesFound(ClassPathEntry cpe1) {
    TreeLogger logger = createTestTreeLogger();

    PathPrefixSet pps = new PathPrefixSet();
    pps.add(new PathPrefix("", null));

    Set<AbstractResource> r = cpe1.findApplicableResources(logger, pps).keySet();

    assertEquals(9, r.size());
    assertPathIncluded(r, "com/google/gwt/user/User.gwt.xml");
    assertPathIncluded(r, "com/google/gwt/user/client/Command.java");
    assertPathIncluded(r, "com/google/gwt/user/client/Timer.java");
    assertPathIncluded(r, "com/google/gwt/user/client/ui/Widget.java");
    assertPathIncluded(r, "org/example/bar/client/BarClient1.txt");
    assertPathIncluded(r, "org/example/bar/client/BarClient2.txt");
    assertPathIncluded(r, "org/example/bar/client/etc/BarEtc.txt");
    assertPathIncluded(r, "org/example/foo/client/FooClient.java");
    assertPathIncluded(r, "org/example/foo/server/FooServer.java");
  }

  // NOTE: if this test fails, ensure that the source root containing this very
  // source file is on the classpath
  private void testAllCpe2FilesFound(ClassPathEntry cpe2) {
    TreeLogger logger = createTestTreeLogger();

    PathPrefixSet pps = createInclusivePathPrefixSet();
    Set<AbstractResource> r = cpe2.findApplicableResources(logger, pps).keySet();

    assertEquals(6, r.size());
    assertPathIncluded(r, "com/google/gwt/i18n/I18N.gwt.xml");
    assertPathIncluded(r, "com/google/gwt/i18n/client/Messages.java");
    assertPathIncluded(r,
        "com/google/gwt/i18n/rebind/LocalizableGenerator.java");
    assertPathIncluded(r, "org/example/bar/client/BarClient2.txt");
    assertPathIncluded(r, "org/example/bar/client/BarClient3.txt");
    assertPathIncluded(r, "org/example/foo/client/BarClient1.txt");
  }

  private void testPathPrefixSetChanges(ClassPathEntry cpe1, ClassPathEntry cpe2) {
    TreeLogger logger = createTestTreeLogger();

    {
      // Filter is not set yet.
      PathPrefixSet pps = new PathPrefixSet();
      pps.add(new PathPrefix("com/google/gwt/user/", null));
      pps.add(new PathPrefix("com/google/gwt/i18n/", null));

      // Examine cpe1 in the absence of the filter.
      Set<AbstractResource> r1 = cpe1.findApplicableResources(logger, pps).keySet();

      assertEquals(4, r1.size());
      assertPathIncluded(r1, "com/google/gwt/user/User.gwt.xml");
      assertPathIncluded(r1, "com/google/gwt/user/client/Command.java");
      assertPathIncluded(r1, "com/google/gwt/user/client/Timer.java");
      assertPathIncluded(r1, "com/google/gwt/user/client/ui/Widget.java");

      // Examine cpe2 in the absence of the filter.
      Set<AbstractResource> r2 = cpe2.findApplicableResources(logger, pps).keySet();

      assertEquals(3, r2.size());
      assertPathIncluded(r2, "com/google/gwt/i18n/I18N.gwt.xml");
      assertPathIncluded(r2, "com/google/gwt/i18n/client/Messages.java");
      assertPathIncluded(r2,
          "com/google/gwt/i18n/rebind/LocalizableGenerator.java");
    }

    {
      // Create a pps with a filter.
      ResourceFilter excludeXmlFiles = new ResourceFilter() {
        @Override
        public boolean allows(String path) {
          return !path.endsWith(".xml");
        }
      };

      PathPrefixSet pps = new PathPrefixSet();
      pps.add(new PathPrefix("com/google/gwt/user/", excludeXmlFiles));
      pps.add(new PathPrefix("com/google/gwt/i18n/", excludeXmlFiles));

      // Examine cpe1 in the presence of the filter.
      Set<AbstractResource> r1 = cpe1.findApplicableResources(logger, pps).keySet();

      assertEquals(3, r1.size());
      assertPathNotIncluded(r1, "com/google/gwt/user/User.gwt.xml");
      assertPathIncluded(r1, "com/google/gwt/user/client/Command.java");
      assertPathIncluded(r1, "com/google/gwt/user/client/Timer.java");
      assertPathIncluded(r1, "com/google/gwt/user/client/ui/Widget.java");

      // Examine cpe2 in the presence of the filter.
      Set<AbstractResource> r2 = cpe2.findApplicableResources(logger, pps).keySet();

      assertEquals(2, r2.size());
      assertPathNotIncluded(r1, "com/google/gwt/user/User.gwt.xml");
      assertPathIncluded(r2, "com/google/gwt/i18n/client/Messages.java");
      assertPathIncluded(r2,
          "com/google/gwt/i18n/rebind/LocalizableGenerator.java");
    }

    {
      /*
       * Change the prefix path set to the zero-lenth prefix (which allows
       * everything), but specify a filter that disallows everything.
       */
      PathPrefixSet pps = new PathPrefixSet();
      pps.add(new PathPrefix("", new ResourceFilter() {
        @Override
        public boolean allows(String path) {
          // Exclude everything.
          return false;
        }
      }));

      // Examine cpe1 in the presence of the filter.
      Set<AbstractResource> r1 = cpe1.findApplicableResources(logger, pps).keySet();

      assertEquals(0, r1.size());

      // Examine cpe2 in the presence of the filter.
      Set<AbstractResource> r2 = cpe2.findApplicableResources(logger, pps).keySet();

      assertEquals(0, r2.size());
    }
  }

  private void testUseOfPrefixesWithFiltering(ClassPathEntry cpe1,
      ClassPathEntry cpe2) {
    TreeLogger logger = createTestTreeLogger();

    PathPrefixSet pps = new PathPrefixSet();
    ResourceFilter excludeXmlFiles = new ResourceFilter() {
      @Override
      public boolean allows(String path) {
        return !path.endsWith(".xml");
      }
    };
    // The prefix is intentionally starting at the module-level, not 'client'.
    pps.add(new PathPrefix("com/google/gwt/user/", excludeXmlFiles));
    pps.add(new PathPrefix("com/google/gwt/i18n/", excludeXmlFiles));

    {
      // Examine cpe1.
      Set<AbstractResource> r = cpe1.findApplicableResources(logger, pps).keySet();

      assertEquals(3, r.size());
      // User.gwt.xml would be included but for the filter.
      assertPathIncluded(r, "com/google/gwt/user/client/Command.java");
      assertPathIncluded(r, "com/google/gwt/user/client/Timer.java");
      assertPathIncluded(r, "com/google/gwt/user/client/ui/Widget.java");
    }

    {
      // Examine cpe2.
      Set<AbstractResource> r = cpe2.findApplicableResources(logger, pps).keySet();

      assertEquals(2, r.size());
      // I18N.gwt.xml would be included but for the filter.
      assertPathIncluded(r, "com/google/gwt/i18n/client/Messages.java");
      assertPathIncluded(r,
          "com/google/gwt/i18n/rebind/LocalizableGenerator.java");
    }
  }

  private static PathPrefixSet createInclusivePathPrefixSet() {
    PathPrefixSet pathPrefixes = new PathPrefixSet();
    pathPrefixes.add(new PathPrefix("", null));
    return pathPrefixes;
  }
}
