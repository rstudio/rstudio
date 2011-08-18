/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.requestfactory.apt;

import com.google.gwt.dev.util.Util;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryPolymorphicTest;
import com.google.web.bindery.requestfactory.shared.LoggingRequest;
import com.google.web.bindery.requestfactory.shared.SimpleRequestFactory;
import com.google.web.bindery.requestfactory.shared.TestRequestFactory;
import com.google.web.bindery.requestfactory.shared.impl.FindRequest;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Integration test of {@link RfValidator} using the Java6 tools API to invoke
 * the Java compiler. This test requires that the gwt-user source is available
 * on the classpath.
 */
public class RfValidatorTest extends TestCase {
  /**
   * Provides access to a source file from the classpath.
   */
  private static class UriJavaFileObject extends SimpleJavaFileObject {
    public static UriJavaFileObject create(Class<?> toLoad) {
      try {
        String path = toLoad.getName().replace('.', '/') + ".java";
        /*
         * SimpleJavaFileObject does not like the URI's created from jar URLs
         * since their path does not start with a leading forward-slash. The
         * choice of "classpath" as the scheme is arbitrary and meaningless.
         */
        URI fakeLocation = new URI("classpath:/" + path);
        return new UriJavaFileObject(fakeLocation, Thread.currentThread().getContextClassLoader()
            .getResource(path));
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    private final URL contents;

    public UriJavaFileObject(URI fakeLocation, URL contents) throws URISyntaxException {
      super(fakeLocation, JavaFileObject.Kind.SOURCE);
      this.contents = contents;
    }

    @Override
    public CharSequence getCharContent(boolean ignored) throws IOException {
      return Util.readStreamAsString(contents.openStream());
    }
  }

  /**
   * Smoke test to ensure that appropriate errors and warnings are emitted.
   */
  public void testErrorsAndWarnings() throws IOException {
    testGeneratedMessages(EntityProxyCheckDomainMapping.class);
    testGeneratedMessages(EntityProxyMismatchedFind.class);
    testGeneratedMessages(EntityProxyMissingDomainLocatorMethods.class);
    testGeneratedMessages(EntityProxyMissingDomainType.class);
    testGeneratedMessages(MyRequestContext.class);
    testGeneratedMessages(MyRequestFactory.class);
    testGeneratedMessages(RequestContextMissingDomainType.class);
    testGeneratedMessages(RequestContextUsingUnmappedProxy.class,
        EntityProxyMissingDomainType.class);
    testGeneratedMessages(RequestContextWithMismatchedBoxes.class);
    testGeneratedMessages(RequestContextWithMismatchedInstance.class);
  }

  /**
   * The target classes for this method don't contain any {@code @Expect}
   * annotations, so this test will verify that they are error- and
   * warning-free.
   */
  public void testTestClasses() throws IOException {
    testGeneratedMessages(RequestFactoryPolymorphicTest.class);
    testGeneratedMessages(FindRequest.class);
    testGeneratedMessages(LoggingRequest.class);
    testGeneratedMessages(SimpleRequestFactory.class);
    testGeneratedMessages(TestRequestFactory.class);
  }

  /**
   * The target classes for this method don't contain any {@code @Expect}
   * annotations, so this test will verify that they are error- and
   * warning-free.
   * 
   * @throws IOException
   */
  public void testTestClassesClientOnly() throws IOException {
    testGeneratedMessages(true, RequestFactoryPolymorphicTest.class);
    testGeneratedMessages(true, FindRequest.class);
    testGeneratedMessages(true, LoggingRequest.class);
    testGeneratedMessages(true, SimpleRequestFactory.class);
    testGeneratedMessages(true, TestRequestFactory.class);
  }

  private void assertEquals(TreeSet<Diagnostic<? extends JavaFileObject>> expected,
      TreeSet<Diagnostic<? extends JavaFileObject>> actual) {
    List<Diagnostic<?>> unmatched = new ArrayList<Diagnostic<?>>();

    // Remove actual elements from expect, saving any unexpected elements
    for (Diagnostic<? extends JavaFileObject> d : actual) {
      if (!expected.remove(d)) {
        unmatched.add(d);
      }
    }

    assertTrue("Did not see expected errors: " + expected + "\n\nLeftovers :" + unmatched, expected
        .isEmpty());
    assertTrue("Unexpected errors: " + unmatched, unmatched.isEmpty());
  }

  private void testGeneratedMessages(Class<?>... classes) throws IOException {
    testGeneratedMessages(false, classes);
  }

  /**
   * Run the annotation processor over one or more classes and verify that the
   * appropriate messages are generated.
   */
  private void testGeneratedMessages(boolean clientOnly, Class<?>... classes) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      // This test is being run without a full JDK
      return;
    }

    // Don't spray files in random locations
    File tempFile = File.createTempFile(RfValidatorTest.class.getSimpleName(), ".jar");
    tempFile.deleteOnExit();
    JavaFileManager fileManager =
        new ValidationTool.JarOrDirectoryOutputFileManager(tempFile, compiler
            .getStandardFileManager(null, null, null));

    List<JavaFileObject> files = new ArrayList<JavaFileObject>(classes.length);
    for (Class<?> clazz : classes) {
      JavaFileObject obj = UriJavaFileObject.create(clazz);
      files.add(obj);
    }
    StringWriter errorWriter = new StringWriter();
    RfValidator rfValidator = new RfValidator();
    rfValidator.setForceErrors(true);
    rfValidator.setClientOnly(clientOnly);

    DiagnosticCollector<JavaFileObject> expectedCollector =
        new DiagnosticCollector<JavaFileObject>();
    CompilationTask expectedTask =
        compiler.getTask(errorWriter, fileManager, expectedCollector, Arrays.asList("-proc:only"),
            null, files);
    expectedTask.setProcessors(Arrays.asList(new ExpectCollector()));
    expectedTask.call();

    DiagnosticCollector<JavaFileObject> actualCollector = new DiagnosticCollector<JavaFileObject>();
    CompilationTask actualTask =
        compiler.getTask(errorWriter, fileManager, actualCollector, Arrays.asList("-proc:only"),
            null, files);
    actualTask.setProcessors(Arrays.asList(rfValidator));
    actualTask.call();

    TreeSet<Diagnostic<? extends JavaFileObject>> expected =
        new TreeSet<Diagnostic<? extends JavaFileObject>>(new DiagnosticComparator());
    expected.addAll(expectedCollector.getDiagnostics());
    TreeSet<Diagnostic<? extends JavaFileObject>> actual =
        new TreeSet<Diagnostic<? extends JavaFileObject>>(new DiagnosticComparator());
    actual.addAll(actualCollector.getDiagnostics());
    assertEquals(expected, actual);
  }
}
