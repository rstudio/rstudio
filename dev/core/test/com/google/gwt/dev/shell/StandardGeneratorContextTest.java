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
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.dev.cfg.MockModuleDef;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A wide variety of tests on {@link StandardGeneratorContext}.
 */
public class StandardGeneratorContextTest extends TestCase {

  private static class MockGenerator extends Generator {
    @Override
    public String generate(TreeLogger logger, GeneratorContext context,
        String typeName) throws UnableToCompleteException {
      return typeName;
    }
  }

  private static class MockPropertyOracle implements PropertyOracle {
    public ConfigurationProperty getConfigurationProperty(String name) {
      return null;
    }

    public String getPropertyValue(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      return "";
    }

    public String[] getPropertyValueSet(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      return new String[] {};
    }

    public SelectionProperty getSelectionProperty(TreeLogger logger, String name) {
      return null;
    }
  }

  private final ArtifactSet artifactSet = new ArtifactSet();
  private final StandardGeneratorContext genCtx;
  private final CompilationState mockCompilationState = CompilationStateBuilder.buildFrom(
      TreeLogger.NULL, Collections.<Resource> emptySet());
  private final TreeLogger mockLogger = TreeLogger.NULL;
  private final PropertyOracle mockPropOracle = new MockPropertyOracle();
  /**
   * Stores the File objects to delete in the order they were created. Delete
   * them in reverse order.
   */
  private final List<File> toDelete = new ArrayList<File>();

  public StandardGeneratorContextTest() {
    genCtx = new StandardGeneratorContext(mockCompilationState,
        new MockModuleDef(), null, artifactSet, false);
    genCtx.setPropertyOracle(mockPropOracle);
    genCtx.setCurrentGenerator(Generator.class);
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
    String path = "testTryCreateResource/commitCalledTwice";
    OutputStream os = genCtx.tryCreateResource(mockLogger, path);
    os.write("going to call commit twice after this...".getBytes(Util.DEFAULT_ENCODING));
    genCtx.setCurrentGenerator(MockGenerator.class);
    GeneratedResource res = genCtx.commitResource(mockLogger, os);
    assertEquals(path, res.getPartialPath());
    assertEquals(MockGenerator.class, res.getGenerator());
    assertEquals(1, artifactSet.size());
    assertTrue(artifactSet.contains(res));
    try {
      genCtx.commitResource(mockLogger, os);
      fail("Calling commit() again on the same stream object should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }
    // Didn't change the artifactSet
    assertEquals(1, artifactSet.size());
  }

  public void testTryCreateResource_commitNotCalled()
      throws UnableToCompleteException, IOException {
    String path = "testTryCreateResource/commitNotCalled";
    OutputStream os = genCtx.tryCreateResource(mockLogger, path);
    byte[] arrayWritten = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    os.write(arrayWritten);

    // Note that we're *not* committing before calling finish().
    genCtx.finish(mockLogger);
    assertEquals(0, artifactSet.size());
    try {
      os.write(arrayWritten);
      fail("Expected IOException for writing after abort");
    } catch (IOException expected) {
    }
  }

  public void testTryCreateResource_commitWithBadStream() {
    try {
      genCtx.commitResource(mockLogger, (OutputStream) null);
      fail("Calling commit() on a null stream should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }
    assertEquals(0, artifactSet.size());

    try {
      OutputStream os = new ByteArrayOutputStream();
      genCtx.commitResource(mockLogger, os);
      fail("Calling commit() on a stream not returned from tryCreateResource() should have caused an exception");
    } catch (UnableToCompleteException e) {
      // Success
    }
    assertEquals(0, artifactSet.size());
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
    testTryCreateResource_normalCompletion("creationWorksBetweenFinishes1");
    genCtx.finish(mockLogger);
    testTryCreateResource_normalCompletion("creationWorksBetweenFinishes2");
    genCtx.finish(mockLogger);
  }

  public void testTryCreateResource_duplicateCreationAfterCommit()
      throws UnableToCompleteException, UnsupportedEncodingException,
      IOException {
    String path = "testTryCreateResource/duplicateCreationAfterCommit";
    OutputStream os1 = genCtx.tryCreateResource(mockLogger, path);
    os1.write("going to call commit twice after this...".getBytes(Util.DEFAULT_ENCODING));
    genCtx.commitResource(mockLogger, os1);
    assertEquals(1, artifactSet.size());

    OutputStream os2 = genCtx.tryCreateResource(mockLogger, path);
    assertNull(os2);
  }

  public void testTryCreateResource_duplicateCreationAttempt()
      throws UnableToCompleteException {
    String path = "testTryCreateResource/duplicateCreationAttempt";
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
    genCtx.finish(mockLogger);
  }

  public void testTryCreateResource_normalCompletionWithoutSubDir()
      throws UnableToCompleteException, IOException {
    String path = "normalCompletionWithoutSubDir";
    testTryCreateResource_normalCompletion(path);
  }

  public void testTryCreateResource_normalCompletionWithSubDir()
      throws UnableToCompleteException, IOException {
    String filename = "testTryCreateResource/normalCompletionWithSubDir";
    testTryCreateResource_normalCompletion(filename);
  }

  /**
   * Tests that tryCreateResource() returns <code>null</code> when the specified
   * file is already on the public path.
   * 
   * @throws UnableToCompleteException
   * @throws IOException
   */
  public void testTryCreateResource_outputFileOnPublicPath()
      throws UnableToCompleteException {
    OutputStream os = genCtx.tryCreateResource(mockLogger, "onPublicPath.txt");
    assertNull(
        "tryCreateResource() should return null when the target file is already on the public path",
        os);
    assertEquals(0, artifactSet.size());
  }

  public void testTryCreateResource_writeAfterCommit()
      throws UnableToCompleteException, IOException {
    String path = "normalCompletionWithoutSubDir";
    OutputStream os = genCtx.tryCreateResource(mockLogger, path);
    assertNotNull(os);
    byte[] arrayWritten = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    os.write(arrayWritten);
    genCtx.commitResource(mockLogger, os);
    try {
      os.write(arrayWritten);
      fail("Expected IOException for writing after commit");
    } catch (IOException expected) {
    }
  }

  @Override
  protected void tearDown() throws Exception {
    for (int i = toDelete.size() - 1; i >= 0; --i) {
      File f = toDelete.get(i);
      Util.recursiveDelete(f, false);
      assertFalse("Unable to delete " + f.getAbsolutePath(), f.exists());
    }
  }

  private void testTryCreateResource_normalCompletion(String name)
      throws UnableToCompleteException, IOException {
    OutputStream os = genCtx.tryCreateResource(mockLogger, name);
    assertNotNull(os);
    byte[] arrayWritten = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    os.write(arrayWritten);
    GeneratedResource res = genCtx.commitResource(mockLogger, os);
    assertEquals(name, res.getPartialPath());
    assertTrue(artifactSet.contains(res));

    // Read the file.
    byte[] arrayRead = Util.readStreamAsBytes(res.getContents(mockLogger));
    assertTrue(Arrays.equals(arrayWritten, arrayRead));
  }

}
