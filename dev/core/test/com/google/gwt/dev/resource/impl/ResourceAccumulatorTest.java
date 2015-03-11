/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.resource.impl;

import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tests for ResourceAccumulator.
 */
public class ResourceAccumulatorTest extends TestCase {

  public void testAddFile() throws Exception {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("subdir", rootDirectory);

    ResourceAccumulator resourceAccumulator =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    assertTrue(getResources(resourceAccumulator).isEmpty());

    createFileIn("New.java", subDirectory);
    waitForFileEvents();

    List<AbstractResource> resources = getResources(resourceAccumulator);
    assertEquals(1, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("New.java"));

    resourceAccumulator.shutdown();
  }

  public void testDeleteFile() throws Exception {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("subdir", rootDirectory);
    File originalFile = createFileIn("SomeFile.java", subDirectory);

    ResourceAccumulator resourceAccumulator =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    List<AbstractResource> resources = getResources(resourceAccumulator);
    assertEquals(1, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("SomeFile.java"));

    originalFile.delete();
    waitForFileEvents();

    assertTrue(getResources(resourceAccumulator).isEmpty());

    resourceAccumulator.shutdown();
  }

  public void testListensInNewDirectories() throws Exception {
    File rootDirectory = Files.createTempDir();

    ResourceAccumulator resourceAccumulator =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    assertTrue(getResources(resourceAccumulator).isEmpty());

    // Create a new directory and contained file AFTER the root directory has started being listened
    // to.
    File subDirectory = createDirectoryIn("subdir", rootDirectory);
    createFileIn("New.java", subDirectory);
    waitForFileEvents();

    List<AbstractResource> resources = getResources(resourceAccumulator);
    assertEquals(1, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("New.java"));

    resourceAccumulator.shutdown();
  }

  public void testMultipleListeners() throws Exception {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("subdir", rootDirectory);

    ResourceAccumulator resourceAccumulator1 =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());
    ResourceAccumulator resourceAccumulator2 =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    assertTrue(getResources(resourceAccumulator1).isEmpty());
    assertTrue(getResources(resourceAccumulator2).isEmpty());

    createFileIn("New.java", subDirectory);
    waitForFileEvents();

    List<AbstractResource> resources1 = getResources(resourceAccumulator1);
    assertEquals(1, resources1.size());
    assertTrue(resources1.get(0).getPath().endsWith("New.java"));

    List<AbstractResource> resources2 = getResources(resourceAccumulator2);
    assertEquals(1, resources2.size());
    assertTrue(resources2.get(0).getPath().endsWith("New.java"));

    resourceAccumulator1.shutdown();
    resourceAccumulator2.shutdown();
  }

  public void testRenameFile() throws Exception {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("subdir", rootDirectory);
    File originalFile = createFileIn("OriginalName.java", subDirectory);
    File renamedFile = new File(subDirectory, "Renamed.java");

    ResourceAccumulator resourceAccumulator =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    List<AbstractResource> resources = getResources(resourceAccumulator);
    assertEquals(1, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("OriginalName.java"));

    originalFile.renameTo(renamedFile);
    waitForFileEvents();

    resources = getResources(resourceAccumulator);
    assertEquals(1, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("Renamed.java"));

    resourceAccumulator.shutdown();
  }

  public void testRenameDirectory() throws Exception {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("original_dir", rootDirectory);
    createFileIn("Name1.java", subDirectory);
    createFileIn("Name2.java", subDirectory);
    File renamedSubDirectory = new File(rootDirectory, "new_dir");

    ResourceAccumulator resourceAccumulator =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    List<AbstractResource> resources = getResources(resourceAccumulator);
    assertEquals(2, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("original_dir/Name1.java"));
    assertTrue(resources.get(1).getPath().endsWith("original_dir/Name2.java"));

    subDirectory.renameTo(renamedSubDirectory);
    waitForFileEvents();

    resources = getResources(resourceAccumulator);
    assertEquals(2, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("new_dir/Name1.java"));
    assertTrue(resources.get(1).getPath().endsWith("new_dir/Name2.java"));

    resourceAccumulator.shutdown();
  }

  public void testRenameParentDirectory() throws Exception {
    File rootDirectory = Files.createTempDir();
    File parentDirectory = createDirectoryIn("original_dir", rootDirectory);
    File subDirectory = createDirectoryIn("subdir", parentDirectory);
    createFileIn("Name1.java", subDirectory);
    createFileIn("Name2.java", subDirectory);
    File renamedParentDirectory = new File(rootDirectory, "new_dir");

    ResourceAccumulator resourceAccumulator =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    List<AbstractResource> resources = getResources(resourceAccumulator);
    assertEquals(2, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("original_dir/subdir/Name1.java"));
    assertTrue(resources.get(1).getPath().endsWith("original_dir/subdir/Name2.java"));

    parentDirectory.renameTo(renamedParentDirectory);
    waitForFileEvents();

    resources = getResources(resourceAccumulator);
    assertEquals(2, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("new_dir/subdir/Name1.java"));
    assertTrue(resources.get(1).getPath().endsWith("new_dir/subdir/Name2.java"));

    resourceAccumulator.shutdown();
  }

  public void testSymlinkInfiniteLoop() throws Exception {
    File rootDirectory = Files.createTempDir();
    File subDirectory = Files.createTempDir();

    ResourceAccumulator resourceAccumulator =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    assertTrue(getResources(resourceAccumulator).isEmpty());

    // Symlink in a loop
    java.nio.file.Files.createSymbolicLink(new File(rootDirectory, "sublink").toPath(),
        subDirectory.toPath()).toFile();
    java.nio.file.Files.createSymbolicLink(new File(subDirectory, "sublink").toPath(),
        rootDirectory.toPath()).toFile();
    createFileIn("New.java", subDirectory);
    waitForFileEvents();

    try {
      // Should throw an error if resourceAccumulator got stuck in an infinite directory scan loop.
      getResources(resourceAccumulator);
      fail();
    } catch (FileSystemException expected) {
      // Expected
    }

    resourceAccumulator.shutdown();
  }

  public void testSymlinks() throws Exception {
    File scratchDirectory = Files.createTempDir();
    File newFile = createFileIn("New.java", scratchDirectory);
    File rootDirectory = Files.createTempDir();
    File subDirectory = Files.createTempDir();

    ResourceAccumulator resourceAccumulator =
        new ResourceAccumulator(rootDirectory.toPath(), createInclusivePathPrefixSet());

    assertTrue(getResources(resourceAccumulator).isEmpty());

    // Symlink in a subdirectory and then symlink in a contained file.
    java.nio.file.Files.createSymbolicLink(new File(rootDirectory, "sublink").toPath(),
        subDirectory.toPath()).toFile();
    java.nio.file.Files.createSymbolicLink(new File(subDirectory, "New.java").toPath(),
        newFile.toPath()).toFile();
    waitForFileEvents();

    List<AbstractResource> resources = getResources(resourceAccumulator);
    assertEquals(1, resources.size());
    assertTrue(resources.get(0).getPath().endsWith("sublink/New.java"));

    resourceAccumulator.shutdown();
  }

  private static File createDirectoryIn(String fileName, File inDirectory) {
    File newDirectory = new File(inDirectory, fileName);
    newDirectory.mkdir();
    return newDirectory;
  }

  private static File createFileIn(String fileName, File inDirectory) throws IOException {
    File newFile = new File(inDirectory, fileName);
    newFile.createNewFile();
    return newFile;
  }

  private List<AbstractResource> getResources(ResourceAccumulator resourceAccumulator)
      throws IOException {
    List<AbstractResource> list = Lists.newArrayList(resourceAccumulator.getResources().keySet());
    Collections.sort(list, new Comparator<AbstractResource>() {
      @Override
      public int compare(AbstractResource a, AbstractResource b) {
        return a.getLocation().compareTo(b.getLocation());
      }
    });
    return list;
  }

  private static PathPrefixSet createInclusivePathPrefixSet() {
    PathPrefixSet pathPrefixes = new PathPrefixSet();
    pathPrefixes.add(new PathPrefix("", null));
    return pathPrefixes;
  }

  private void waitForFileEvents() throws InterruptedException {
    Thread.sleep(100);
  }
}
