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
package com.google.gwt.resources.rg;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.resources.ext.ResourceContext;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tests output functions.
 */
public class CssOutputTestCase extends TestCase {

  public void testOutputCssMapArtifact() throws UnableToCompleteException {
    UnitTestTreeLogger testLogger = new UnitTestTreeLogger.Builder().createLogger();
    ResourceContext mockResourceContext = mock(ResourceContext.class);
    Map<JMethod, String> testMap = new HashMap<JMethod, String>();
    OutputStream mockOutputStream = mock(OutputStream.class);
    GeneratorContext mockGeneratorContext = mock(GeneratorContext.class);
    GeneratedResource mockGeneratedResource = mock(GeneratedResource.class);

    when(mockResourceContext.getGeneratorContext()).thenReturn(mockGeneratorContext);
    when(mockGeneratorContext.tryCreateResource(
        testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap")).thenReturn(mockOutputStream);
    when(mockGeneratorContext.commitResource(testLogger, mockOutputStream)).thenReturn(
        mockGeneratedResource);

    JMethod method = mock(JMethod.class);
    JClassType bundleType = mock(JClassType.class);
    when(method.getEnclosingType()).thenReturn(bundleType);
    when(bundleType.getQualifiedSourceName()).thenReturn("com.test.Bundle");
    when(method.getName()).thenReturn("cssMethod");

    CssResourceGenerator crg = new CssResourceGenerator();
    // Test the method
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);

    testLogger.assertCorrectLogEntries();

    verify(mockGeneratorContext).tryCreateResource(testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap");
    verify(mockGeneratorContext).commitResource(testLogger, mockOutputStream);
    verifyNoMoreInteractions(mockGeneratorContext);
  }

  public void testOutputCssMapArtifactThrowOnTryCreateResource() throws UnableToCompleteException {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expectWarn(
        "Could not create resource: cssResource/com.test.Bundle.cssMethod.cssmap", null);
    UnitTestTreeLogger testLogger =  builder.createLogger();
    ResourceContext mockResourceContext = mock(ResourceContext.class);
    Map<JMethod, String> testMap = new HashMap<JMethod, String>();
    GeneratorContext mockGeneratorContext = mock(GeneratorContext.class);

    when(mockResourceContext.getGeneratorContext()).thenReturn(mockGeneratorContext);
    when(mockGeneratorContext.tryCreateResource(testLogger,
        "cssResource/com.test.Bundle.cssMethod.cssmap")).thenThrow(new UnableToCompleteException());

    JMethod method = mock(JMethod.class);
    JClassType bundleType = mock(JClassType.class);
    when(method.getEnclosingType()).thenReturn(bundleType);
    when(bundleType.getQualifiedSourceName()).thenReturn("com.test.Bundle");
    when(method.getName()).thenReturn("cssMethod");

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);

    testLogger.assertCorrectLogEntries();

    verify(mockGeneratorContext).tryCreateResource(testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap");
    verifyNoMoreInteractions(mockGeneratorContext);
  }

  public void testOutputCssMapArtifactReturnNullOutputString() throws UnableToCompleteException {
    UnitTestTreeLogger testLogger = new UnitTestTreeLogger.Builder().createLogger();
    ResourceContext mockResourceContext = mock(ResourceContext.class);
    Map<JMethod, String> testMap = new HashMap<JMethod, String>();
    GeneratorContext mockGeneratorContext = mock(GeneratorContext.class);

    when(mockResourceContext.getGeneratorContext()).thenReturn(mockGeneratorContext);
    when(mockGeneratorContext.tryCreateResource(
        testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap")).thenReturn(null);

    JMethod method = mock(JMethod.class);
    JClassType bundleType = mock(JClassType.class);
    when(method.getEnclosingType()).thenReturn(bundleType);
    when(bundleType.getQualifiedSourceName()).thenReturn("com.test.Bundle");
    when(method.getName()).thenReturn("cssMethod");

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);

    testLogger.assertCorrectLogEntries();

    verify(mockGeneratorContext).tryCreateResource(testLogger,
        "cssResource/com.test.Bundle.cssMethod.cssmap");
    verifyNoMoreInteractions(mockGeneratorContext);
  }

  public void testOutputCssMapArtifactThrowOnCommitResource() throws UnableToCompleteException {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expectWarn(
        "Error trying to commit artifact: cssResource/com.test.Bundle.cssMethod.cssmap", null);
    UnitTestTreeLogger testLogger =  builder.createLogger();
    ResourceContext mockResourceContext = mock(ResourceContext.class);
    Map<JMethod, String> testMap = new HashMap<JMethod, String>();
    OutputStream mockOutputStream = mock(OutputStream.class);
    GeneratorContext mockGeneratorContext = mock(GeneratorContext.class);

    when(mockResourceContext.getGeneratorContext()).thenReturn(mockGeneratorContext);
    when(mockGeneratorContext.tryCreateResource(
        testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap")).thenReturn(mockOutputStream);
    when(mockGeneratorContext.commitResource(testLogger, mockOutputStream)).thenThrow(
        new UnableToCompleteException());

    JMethod method = mock(JMethod.class);
    JClassType bundleType = mock(JClassType.class);
    when(method.getEnclosingType()).thenReturn(bundleType);
    when(bundleType.getQualifiedSourceName()).thenReturn("com.test.Bundle");
    when(method.getName()).thenReturn("cssMethod");

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);

    testLogger.assertCorrectLogEntries();

    verify(mockGeneratorContext).tryCreateResource(testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap");
    verify(mockGeneratorContext).commitResource(testLogger, mockOutputStream);
  }

  public void testOutputCssMapArtifactWithTestData() throws UnableToCompleteException {
    UnitTestTreeLogger testLogger = new UnitTestTreeLogger.Builder().createLogger();
    ResourceContext mockResourceContext = mock(ResourceContext.class);
    JMethod mockJMethod1 = mock(JMethod.class);
    JMethod mockJMethod2 = mock(JMethod.class);
    JMethod mockJMethod3 = mock(JMethod.class);
    JClassType mockJClassType1 = mock(JClassType.class);
    JClassType mockJClassType2 = mock(JClassType.class);
    JClassType mockJClassType3 = mock(JClassType.class);
    Map<JMethod, String> testMap = new LinkedHashMap<JMethod, String>();
    testMap.put(mockJMethod1, "TESTCSSNAME1");
    testMap.put(mockJMethod2, "TESTCSSNAME2");
    testMap.put(mockJMethod3, "TESTCSSNAME3");
    ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
    GeneratorContext mockGeneratorContext = mock(GeneratorContext.class);
    GeneratedResource mockGeneratedResource = mock(GeneratedResource.class);

    when(mockResourceContext.getGeneratorContext()).thenReturn(mockGeneratorContext);
    when(mockGeneratorContext.tryCreateResource(
        testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap")).thenReturn(testOutputStream);
    when(mockJMethod1.getEnclosingType()).thenReturn(mockJClassType1);
    when(mockJClassType1.getQualifiedSourceName()).thenReturn("test.class.type.1");
    when(mockJMethod1.getName()).thenReturn("basename1");
    when(mockJMethod2.getEnclosingType()).thenReturn(mockJClassType2);
    when(mockJClassType2.getQualifiedSourceName()).thenReturn("test.class.type.2");
    when(mockJMethod2.getName()).thenReturn("basename2");
    when(mockJMethod3.getEnclosingType()).thenReturn(mockJClassType3);
    when(mockJClassType3.getQualifiedSourceName()).thenReturn("test.class.type.3");
    when(mockJMethod3.getName()).thenReturn("basename3");
    when(mockGeneratorContext.commitResource(testLogger, testOutputStream)).thenReturn(
        mockGeneratedResource);

    JMethod method = mock(JMethod.class);
    JClassType bundleType = mock(JClassType.class);
    when(method.getEnclosingType()).thenReturn(bundleType);
    when(bundleType.getQualifiedSourceName()).thenReturn("com.test.Bundle");
    when(method.getName()).thenReturn("cssMethod");

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);
    String expectedOutput = "test-class-type-1-basename1,TESTCSSNAME1\n" +
        "test-class-type-2-basename2,TESTCSSNAME2\n" +
        "test-class-type-3-basename3,TESTCSSNAME3\n";
    assertEquals(expectedOutput, testOutputStream.toString());

    testLogger.assertCorrectLogEntries();

    verify(mockGeneratorContext).tryCreateResource(testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap");
    verify(mockGeneratorContext).commitResource(testLogger, testOutputStream);
  }
}
