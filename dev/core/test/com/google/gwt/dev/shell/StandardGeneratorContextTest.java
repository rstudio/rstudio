/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.util.Util;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A wide variety of tests on {@link StandardGeneratorContext}.
 */
public class StandardGeneratorContextTest extends TestCase {

  private static class MockCacheManager extends CacheManager {
  }

  private static class MockPropertyOracle implements PropertyOracle {
    public String getPropertyValue(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      return "";
    }
  }

  private static class MockTreeLogger implements TreeLogger {

    public TreeLogger branch(Type type, String msg, Throwable caught) {
      return this;
    }

    public boolean isLoggable(Type type) {
      return false;
    }

    public void log(Type type, String msg, Throwable caught) {
    }
  }

  private static class MockTypeOracle extends TypeOracle {
  }

  /**
   * Stores the File objects to delete in the order they were created. Delete
   * them in reverse order.
   */
  private final List toDelete = new ArrayList();
  private final TypeOracle mockTypeOracle = new MockTypeOracle();
  private final PropertyOracle mockPropOracle = new MockPropertyOracle();
  private final File tempGenDir;
  private final File tempOutDir;
  private final CacheManager mockCacheManager = new MockCacheManager();
  private final TreeLogger mockLogger = new MockTreeLogger();
  private final StandardGeneratorContext genCtx;
  private int tempFileCounter;

  public StandardGeneratorContextTest() {
    tempGenDir = createTempDir("gwt-gen-");
    tempOutDir = createTempDir("gwt-out-");
    genCtx = new StandardGeneratorContext(mockTypeOracle, mockPropOracle,
        tempGenDir, tempOutDir, mockCacheManager);
  }

  public void testTryCreateResource_badFileName() {
    try {
      genCtx.tryCreateResource(mockLogger, null);
      fail("The null filename in the previous statement should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }

    try {
      genCtx.tryCreateResource(mockLogger, "");
      fail("The empty filename in the previous statement should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }

    try {
      genCtx.tryCreateResource(mockLogger, "       ");
      fail("The whitespace-only filename in the previous statement should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }

    try {
      File absFile = new File("stuff.bin");
      String asbPath = absFile.getAbsolutePath();
      genCtx.tryCreateResource(mockLogger, asbPath);
      fail("The absolute path in the previous statement should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }

    try {
      genCtx.tryCreateResource(mockLogger, "asdf\\stuff.bin");
      fail("The backslash in the path in the previous statement should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }
  }

  /**
   * Tests that calling commit a second time on the same OutputStream throws an
   * exception. Note that this behavior should follow the same basic code path
   * attempting to commit an unknown OutputStream, as in
   * {@link #testTryCreateResource_commitWithUnknownStream()}.
   */
  public void testTryCreateResource_commitCalledTwice()
      throws UnableToCompleteException, IOException {
    String path = createTempOutFilename();
    OutputStream os = genCtx.tryCreateResource(mockLogger, path);
    os.write("going to call commit twice after this...".getBytes());
    genCtx.commitResource(mockLogger, os);
    File createdFile = new File(tempOutDir, path);
    assertTrue(createdFile.exists());
    rememberToDelete(createdFile);
    try {
      genCtx.commitResource(mockLogger, os);
      fail("Calling commit() again on the same stream object should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }
  }

  public void testTryCreateResource_commitNotCalled()
      throws UnableToCompleteException, IOException {
    String path = createTempOutFilename();
    OutputStream os = genCtx.tryCreateResource(mockLogger, path);
    byte[] arrayWritten = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    os.write(arrayWritten);

    // Note that we're *not* committing before calling finish().
    genCtx.finish(mockLogger);

    File wouldBeCreatedFile = new File(tempOutDir, path);
    assertFalse(wouldBeCreatedFile.exists());
  }

  public void testTryCreateResource_commitWithBadStream() {
    try {
      genCtx.commitResource(mockLogger, (OutputStream) null);
      fail("Calling commit() on a null stream should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }

    try {
      OutputStream os = new ByteArrayOutputStream();
      genCtx.commitResource(mockLogger, os);
      fail("Calling commit() on a stream not returned from tryCreateResource() should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }
  }

  /**
   * Tests that finish() can be called before and after output file creation.
   * 
   * @throws UnableToCompleteException
   * @throws IOException
   * 
   */
  public void testTryCreateResource_creationWorksBetweenFinishes()
      throws UnableToCompleteException, IOException {
    genCtx.finish(mockLogger);
    testTryCreateResource_normalCompletionWithoutSubDir();
    genCtx.finish(mockLogger);
    testTryCreateResource_normalCompletionWithoutSubDir();
    genCtx.finish(mockLogger);
  }

  public void testTryCreateResource_duplicateCreationAttempt()
      throws UnableToCompleteException {
    String path = createTempOutFilename();
    OutputStream os1 = genCtx.tryCreateResource(mockLogger, path);
    assertNotNull(os1);
    OutputStream os2 = genCtx.tryCreateResource(mockLogger, path);
    assertNull(os2);
  }

  public void testTryCreateResource_finishCalledTwice()
      throws UnableToCompleteException, IOException {
    // Borrow impl.
    testTryCreateResource_commitNotCalled();

    // Now call finish() again to make sure nothing blows up.
    try {
      genCtx.finish(mockLogger);
    } catch (UnableToCompleteException e) {
      fail("finish() failed; it should support safely being called any number of times");
    }
  }

  public void testTryCreateResource_normalCompletionWithoutSubDir()
      throws UnableToCompleteException, IOException {
    String path = createTempOutFilename();
    testTryCreateResource_normalCompletion(null, path);
  }

  public void testTryCreateResource_normalCompletionWithSubDir()
      throws UnableToCompleteException, IOException {
    String subdir = createTempOutFilename();
    String filename = createTempOutFilename();
    testTryCreateResource_normalCompletion(subdir, filename);
  }

  /**
   * Tests that tryCreateResource() throws an exception when commit() is called
   * if the output exists. This situation should only happen if the file is
   * created externally between the time tryCreateResource() is called and
   * commit() is called. In the normal case of an existing file,
   * tryCreateResource() would return <code>null</code> so there would be
   * nothing to commit.
   * 
   * @throws UnableToCompleteException
   * @throws IOException
   */
  public void testTryCreateResource_outputFileConflictAtCommit()
      throws UnableToCompleteException, IOException {
    String path = createTempOutFilename();

    OutputStream os = genCtx.tryCreateResource(mockLogger, path);
    assertNotNull(
        "This test requires that the file being created does not already exist at this point",
        os);
    byte[] arrayWritten = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    os.write(arrayWritten);
    try {
      // Manually create the file that would normally be created by commit().
      File existingFile = new File(tempOutDir, path);
      Util.writeStringAsFile(existingFile, "please don't clobber me");
      assertTrue(existingFile.exists());
      rememberToDelete(existingFile);

      genCtx.commitResource(mockLogger, os);
      fail("The previous statement should have caused an exception since writing the resource must have failed");
    } catch (UnableToCompleteException e) {
      // Success.
    }
  }

  /**
   * Tests that tryCreateResource() returns <code>null</code> when the
   * specified file already exists.
   * 
   * @throws UnableToCompleteException
   * @throws IOException
   */
  public void testTryCreateResource_outputFileConflictAtCreation()
      throws UnableToCompleteException, IOException {
    String path = createTempOutFilename();

    // Manually create the file that would normally be created by commit().
    File existingFile = new File(tempOutDir, path);
    Util.writeStringAsFile(existingFile, "please don't clobber me");
    assertTrue(existingFile.exists());
    rememberToDelete(existingFile);

    OutputStream os = genCtx.tryCreateResource(mockLogger, path);
    assertNull(
        "tryCreateResource() should return null when the target file already exists",
        os);
  }

  protected void tearDown() throws Exception {
    for (int i = toDelete.size() - 1; i >= 0; --i) {
      File f = (File) toDelete.get(i);
      assertTrue(f.delete());
    }
  }

  private File createTempDir(String prefix) {
    String baseTempPath = System.getProperty("java.io.tmpdir");
    File baseTempDir = new File(baseTempPath);
    File newTempDir;
    do {
      newTempDir = new File(baseTempPath, prefix + System.currentTimeMillis());
    } while (!newTempDir.mkdirs());
    rememberToDelete(newTempDir);
    return newTempDir;
  }

  private String createTempOutFilename() {
    File tempFile;
    do {
      tempFile = new File(tempOutDir, System.currentTimeMillis() + "-" + (++tempFileCounter) + ".gwt.tmp");
    } while (tempFile.exists());
    return tempFile.getName();
  }

  private void rememberToDelete(File f) {
    toDelete.add(f);
  }

  private void testTryCreateResource_normalCompletion(String subdir, String name)
      throws UnableToCompleteException, IOException {
    if (subdir != null) {
      name = subdir + "/" + name;
    }
    OutputStream os = genCtx.tryCreateResource(mockLogger, name);
    assertNotNull(os);
    byte[] arrayWritten = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    os.write(arrayWritten);
    genCtx.commitResource(mockLogger, os);

    if (subdir != null) {
      File createdDir = new File(tempOutDir, subdir);
      assertTrue(createdDir.exists());
      rememberToDelete(createdDir);
    }

    File createdFile = new File(tempOutDir, name);
    assertTrue(createdFile.exists());
    rememberToDelete(createdFile);

    // Read the file.
    byte[] arrayRead = Util.readFileAsBytes(createdFile);
    assertTrue(Arrays.equals(arrayWritten, arrayRead));
  }

}
