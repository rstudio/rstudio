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

import com.google.gwt.thirdparty.guava.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Tests for ChangedFileAccumulator.
 */
public class ChangedFileAccumulatorTest extends TestCase {

  public void testAddFile() throws IOException, InterruptedException, ExecutionException {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("subdir", rootDirectory);

    ChangedFileAccumulator changedFileAccumulator =
        new ChangedFileAccumulator(rootDirectory.toPath());

    assertTrue(changedFileAccumulator.getAndClearChangedFiles().isEmpty());

    createFileIn("New.java", subDirectory);
    waitForFileEvents();

    List<File> modifiedFiles = changedFileAccumulator.getAndClearChangedFiles();
    assertEquals(1, modifiedFiles.size());
    assertTrue(modifiedFiles.get(0).getPath().endsWith("New.java"));

    changedFileAccumulator.shutdown();
  }

  public void testDeleteFile() throws IOException, InterruptedException, ExecutionException {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("subdir", rootDirectory);
    File originalFile = createFileIn("SomeFile.java", subDirectory);

    ChangedFileAccumulator changedFileAccumulator =
        new ChangedFileAccumulator(rootDirectory.toPath());

    assertTrue(changedFileAccumulator.getAndClearChangedFiles().isEmpty());

    originalFile.delete();
    waitForFileEvents();

    List<File> modifiedFiles = changedFileAccumulator.getAndClearChangedFiles();
    assertEquals(1, modifiedFiles.size());
    assertTrue(modifiedFiles.get(0).getPath().endsWith("SomeFile.java"));

    changedFileAccumulator.shutdown();
  }

  public void testListensInNewDirectories() throws IOException, InterruptedException,
      ExecutionException {
    File rootDirectory = Files.createTempDir();

    ChangedFileAccumulator changedFileAccumulator =
        new ChangedFileAccumulator(rootDirectory.toPath());

    assertTrue(changedFileAccumulator.getAndClearChangedFiles().isEmpty());

    // Create a new directory and contained file AFTER the root directory has started being listened
    // to.
    File subDirectory = createDirectoryIn("subdir", rootDirectory);
    createFileIn("New.java", subDirectory);
    waitForFileEvents();

    List<File> modifiedFiles = changedFileAccumulator.getAndClearChangedFiles();
    assertEquals(2, modifiedFiles.size());
    assertTrue(modifiedFiles.get(0).getPath().endsWith("subdir"));
    assertTrue(modifiedFiles.get(1).getPath().endsWith("New.java"));

    changedFileAccumulator.shutdown();
  }

  public void testModifyRepeatedly() throws IOException, InterruptedException, ExecutionException {
    File rootDirectory = Files.createTempDir();
    File fooFile = createFileIn("Foo.java", rootDirectory);

    ChangedFileAccumulator changedFileAccumulator =
        new ChangedFileAccumulator(rootDirectory.toPath());

    assertTrue(changedFileAccumulator.getAndClearChangedFiles().isEmpty());

    for (int i = 0; i < 5; i++) {
      fooFile.setLastModified(i * 1000);
      waitForFileEvents();
      List<File> modifiedFiles = changedFileAccumulator.getAndClearChangedFiles();
      assertEquals(1, modifiedFiles.size());
      assertTrue(modifiedFiles.get(0).getPath().endsWith("Foo.java"));
    }

    changedFileAccumulator.shutdown();
  }

  public void testMultipleListeners() throws IOException, InterruptedException, ExecutionException {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("subdir", rootDirectory);

    ChangedFileAccumulator changedFileAccumulator1 =
        new ChangedFileAccumulator(rootDirectory.toPath());
    ChangedFileAccumulator changedFileAccumulator2 =
        new ChangedFileAccumulator(rootDirectory.toPath());

    assertTrue(changedFileAccumulator1.getAndClearChangedFiles().isEmpty());
    assertTrue(changedFileAccumulator2.getAndClearChangedFiles().isEmpty());

    createFileIn("New.java", subDirectory);
    waitForFileEvents();

    List<File> modifiedFiles1 = changedFileAccumulator1.getAndClearChangedFiles();
    assertEquals(1, modifiedFiles1.size());
    assertTrue(modifiedFiles1.get(0).getPath().endsWith("New.java"));

    List<File> modifiedFiles2 = changedFileAccumulator2.getAndClearChangedFiles();
    assertEquals(1, modifiedFiles2.size());
    assertTrue(modifiedFiles2.get(0).getPath().endsWith("New.java"));

    changedFileAccumulator1.shutdown();
    changedFileAccumulator2.shutdown();
  }

  public void testRenameFile() throws IOException, InterruptedException, ExecutionException {
    File rootDirectory = Files.createTempDir();
    File subDirectory = createDirectoryIn("subdir", rootDirectory);
    File originalFile = createFileIn("OriginalName.java", subDirectory);
    File renamedFile = new File(subDirectory, "Renamed.java");

    ChangedFileAccumulator changedFileAccumulator =
        new ChangedFileAccumulator(rootDirectory.toPath());

    assertTrue(changedFileAccumulator.getAndClearChangedFiles().isEmpty());

    originalFile.renameTo(renamedFile);
    waitForFileEvents();

    List<File> modifiedFiles = changedFileAccumulator.getAndClearChangedFiles();
    assertEquals(2, modifiedFiles.size());
    assertTrue(modifiedFiles.get(0).getPath().endsWith("OriginalName.java"));
    assertTrue(modifiedFiles.get(1).getPath().endsWith("Renamed.java"));

    changedFileAccumulator.shutdown();
  }

  public void testSymlinkInfiniteLoop() throws IOException, InterruptedException,
      ExecutionException {
    File rootDirectory = Files.createTempDir();
    File subDirectory = Files.createTempDir();

    ChangedFileAccumulator changedFileAccumulator =
        new ChangedFileAccumulator(rootDirectory.toPath());

    assertTrue(changedFileAccumulator.getAndClearChangedFiles().isEmpty());

    // Symlink in a loop
    java.nio.file.Files.createSymbolicLink(new File(rootDirectory, "sublink").toPath(),
        subDirectory.toPath()).toFile();
    java.nio.file.Files.createSymbolicLink(new File(subDirectory, "sublink").toPath(),
        rootDirectory.toPath()).toFile();
    createFileIn("New.java", subDirectory);
    waitForFileEvents();

    // Will throw an error if ChangedFileAccumulator got stuck in an infinite directory scan loop.
    List<File> modifiedFiles = changedFileAccumulator.getAndClearChangedFiles();
    assertEquals(2, modifiedFiles.size());
    assertTrue(modifiedFiles.get(0).getPath().endsWith("sublink"));
    assertTrue(modifiedFiles.get(1).getPath().endsWith("New.java"));

    changedFileAccumulator.shutdown();
  }

  public void testSymlinks() throws IOException, InterruptedException, ExecutionException {
    File scratchDirectory = Files.createTempDir();
    File newFile = createFileIn("New.java", scratchDirectory);
    File rootDirectory = Files.createTempDir();
    File subDirectory = Files.createTempDir();

    ChangedFileAccumulator changedFileAccumulator =
        new ChangedFileAccumulator(rootDirectory.toPath());

    assertTrue(changedFileAccumulator.getAndClearChangedFiles().isEmpty());

    // Symlink in a subdirectory and then symlink in a contained file.
    java.nio.file.Files.createSymbolicLink(new File(rootDirectory, "sublink").toPath(),
        subDirectory.toPath()).toFile();
    java.nio.file.Files.createSymbolicLink(new File(subDirectory, "New.java").toPath(),
        newFile.toPath()).toFile();
    waitForFileEvents();

    List<File> modifiedFiles = changedFileAccumulator.getAndClearChangedFiles();
    assertEquals(2, modifiedFiles.size());
    assertTrue(modifiedFiles.get(0).getPath().endsWith("New.java"));
    assertTrue(modifiedFiles.get(1).getPath().endsWith("sublink"));

    changedFileAccumulator.shutdown();
  }

  private File createDirectoryIn(String fileName, File inDirectory) {
    File newDirectory = new File(inDirectory, fileName);
    newDirectory.mkdir();
    return newDirectory;
  }

  private File createFileIn(String fileName, File inDirectory) throws IOException {
    File newFile = new File(inDirectory, fileName);
    newFile.createNewFile();
    return newFile;
  }

  private void waitForFileEvents() throws InterruptedException {
    Thread.sleep(100);
  }
}
