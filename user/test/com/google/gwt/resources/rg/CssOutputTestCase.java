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

import org.easymock.EasyMock;

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
    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    Map<JMethod, String> testMap = new HashMap<JMethod, String>();
    OutputStream mockOutputStream = EasyMock.createMock(OutputStream.class);
    GeneratorContext mockGeneratorContext = EasyMock.createMock(GeneratorContext.class);
    GeneratedResource mockGeneratedResource = EasyMock.createMock(GeneratedResource.class);

    EasyMock.expect(mockResourceContext.getGeneratorContext()).andReturn(mockGeneratorContext);
    EasyMock.expectLastCall().times(2);
    EasyMock.expect(mockGeneratorContext.tryCreateResource(
        testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap")).andReturn(mockOutputStream);
    EasyMock.expect(mockGeneratorContext.commitResource(testLogger, mockOutputStream)).andReturn(
        mockGeneratedResource);

    JMethod method = EasyMock.createMock(JMethod.class);
    JClassType bundleType = EasyMock.createMock(JClassType.class);
    EasyMock.expect(method.getEnclosingType()).andReturn(bundleType);
    EasyMock.expect(bundleType.getQualifiedSourceName()).andReturn("com.test.Bundle");
    EasyMock.expect(method.getName()).andReturn("cssMethod");

    EasyMock.replay(mockResourceContext);
    EasyMock.replay(mockGeneratorContext);
    EasyMock.replay(method);
    EasyMock.replay(bundleType);

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);

    testLogger.assertCorrectLogEntries();
    EasyMock.verify(mockResourceContext);
    EasyMock.verify(mockGeneratorContext);
    EasyMock.verify(method);
    EasyMock.verify(bundleType);
  }

  public void testOutputCssMapArtifactThrowOnTryCreateResource() throws UnableToCompleteException {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expectWarn(
        "Could not create resource: cssResource/com.test.Bundle.cssMethod.cssmap", null);
    UnitTestTreeLogger testLogger =  builder.createLogger();
    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    Map<JMethod, String> testMap = new HashMap<JMethod, String>();
    OutputStream mockOutputStream = EasyMock.createMock(OutputStream.class);
    GeneratorContext mockGeneratorContext = EasyMock.createMock(GeneratorContext.class);
    GeneratedResource mockGeneratedResource = EasyMock.createMock(GeneratedResource.class);

    EasyMock.expect(mockResourceContext.getGeneratorContext()).andReturn(mockGeneratorContext);
    EasyMock.expect(mockGeneratorContext.tryCreateResource(testLogger,
        "cssResource/com.test.Bundle.cssMethod.cssmap")).andThrow(new UnableToCompleteException());

    JMethod method = EasyMock.createMock(JMethod.class);
    JClassType bundleType = EasyMock.createMock(JClassType.class);
    EasyMock.expect(method.getEnclosingType()).andReturn(bundleType);
    EasyMock.expect(bundleType.getQualifiedSourceName()).andReturn("com.test.Bundle");
    EasyMock.expect(method.getName()).andReturn("cssMethod");

    EasyMock.replay(mockResourceContext);
    EasyMock.replay(mockGeneratorContext);
    EasyMock.replay(method);
    EasyMock.replay(bundleType);

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);

    testLogger.assertCorrectLogEntries();
    EasyMock.verify(mockResourceContext);
    EasyMock.verify(mockGeneratorContext);
    EasyMock.verify(method);
    EasyMock.verify(bundleType);
  }

  public void testOutputCssMapArtifactReturnNullOutputString() throws UnableToCompleteException {
    UnitTestTreeLogger testLogger = new UnitTestTreeLogger.Builder().createLogger();
    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    Map<JMethod, String> testMap = new HashMap<JMethod, String>();
    OutputStream mockOutputStream = EasyMock.createMock(OutputStream.class);
    GeneratorContext mockGeneratorContext = EasyMock.createMock(GeneratorContext.class);
    GeneratedResource mockGeneratedResource = EasyMock.createMock(GeneratedResource.class);

    EasyMock.expect(mockResourceContext.getGeneratorContext()).andReturn(mockGeneratorContext);
    EasyMock.expect(mockGeneratorContext.tryCreateResource(
        testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap")).andReturn(null);

    JMethod method = EasyMock.createMock(JMethod.class);
    JClassType bundleType = EasyMock.createMock(JClassType.class);
    EasyMock.expect(method.getEnclosingType()).andReturn(bundleType);
    EasyMock.expect(bundleType.getQualifiedSourceName()).andReturn("com.test.Bundle");
    EasyMock.expect(method.getName()).andReturn("cssMethod");

    EasyMock.replay(mockResourceContext);
    EasyMock.replay(mockGeneratorContext);
    EasyMock.replay(method);
    EasyMock.replay(bundleType);

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);

    testLogger.assertCorrectLogEntries();
    EasyMock.verify(mockResourceContext);
    EasyMock.verify(mockGeneratorContext);
    EasyMock.verify(method);
    EasyMock.verify(bundleType);
  }

  public void testOutputCssMapArtifactThrowOnCommitResource() throws UnableToCompleteException {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expectWarn(
        "Error trying to commit artifact: cssResource/com.test.Bundle.cssMethod.cssmap", null);
    UnitTestTreeLogger testLogger =  builder.createLogger();
    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    Map<JMethod, String> testMap = new HashMap<JMethod, String>();
    OutputStream mockOutputStream = EasyMock.createMock(OutputStream.class);
    GeneratorContext mockGeneratorContext = EasyMock.createMock(GeneratorContext.class);
    GeneratedResource mockGeneratedResource = EasyMock.createMock(GeneratedResource.class);

    EasyMock.expect(mockResourceContext.getGeneratorContext()).andReturn(mockGeneratorContext);
    EasyMock.expectLastCall().times(2);
    EasyMock.expect(mockGeneratorContext.tryCreateResource(
        testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap")).andReturn(mockOutputStream);
    EasyMock.expect(mockGeneratorContext.commitResource(testLogger, mockOutputStream)).andThrow(
        new UnableToCompleteException());

    JMethod method = EasyMock.createMock(JMethod.class);
    JClassType bundleType = EasyMock.createMock(JClassType.class);
    EasyMock.expect(method.getEnclosingType()).andReturn(bundleType);
    EasyMock.expect(bundleType.getQualifiedSourceName()).andReturn("com.test.Bundle");
    EasyMock.expect(method.getName()).andReturn("cssMethod");

    EasyMock.replay(mockResourceContext);
    EasyMock.replay(mockGeneratorContext);
    EasyMock.replay(method);
    EasyMock.replay(bundleType);

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);

    testLogger.assertCorrectLogEntries();
    EasyMock.verify(mockResourceContext);
    EasyMock.verify(mockGeneratorContext);
    EasyMock.verify(method);
    EasyMock.verify(bundleType);
  }

  public void testOutputCssMapArtifactWithTestData() throws UnableToCompleteException {
    UnitTestTreeLogger testLogger = new UnitTestTreeLogger.Builder().createLogger();
    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    JMethod mockJMethod1 = EasyMock.createMock(JMethod.class);
    JMethod mockJMethod2 = EasyMock.createMock(JMethod.class);
    JMethod mockJMethod3 = EasyMock.createMock(JMethod.class);
    JClassType mockJClassType1 = EasyMock.createMock(JClassType.class);
    JClassType mockJClassType2 = EasyMock.createMock(JClassType.class);
    JClassType mockJClassType3 = EasyMock.createMock(JClassType.class);
    Map<JMethod, String> testMap = new LinkedHashMap<JMethod, String>();
    testMap.put(mockJMethod1, "TESTCSSNAME1");
    testMap.put(mockJMethod2, "TESTCSSNAME2");
    testMap.put(mockJMethod3, "TESTCSSNAME3");
    ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
    GeneratorContext mockGeneratorContext = EasyMock.createMock(GeneratorContext.class);
    GeneratedResource mockGeneratedResource = EasyMock.createMock(GeneratedResource.class);

    EasyMock.expect(mockResourceContext.getGeneratorContext()).andReturn(mockGeneratorContext);
    EasyMock.expectLastCall().times(2);
    EasyMock.expect(mockGeneratorContext.tryCreateResource(
        testLogger, "cssResource/com.test.Bundle.cssMethod.cssmap")).andReturn(testOutputStream);
    EasyMock.expect(mockJMethod1.getEnclosingType()).andReturn(mockJClassType1);
    EasyMock.expect(mockJClassType1.getQualifiedSourceName()).andReturn("test.class.type.1");
    EasyMock.expect(mockJMethod1.getName()).andReturn("basename1");
    EasyMock.expect(mockJMethod2.getEnclosingType()).andReturn(mockJClassType2);
    EasyMock.expect(mockJClassType2.getQualifiedSourceName()).andReturn("test.class.type.2");
    EasyMock.expect(mockJMethod2.getName()).andReturn("basename2");
    EasyMock.expect(mockJMethod3.getEnclosingType()).andReturn(mockJClassType3);
    EasyMock.expect(mockJClassType3.getQualifiedSourceName()).andReturn("test.class.type.3");
    EasyMock.expect(mockJMethod3.getName()).andReturn("basename3");
    EasyMock.expect(mockGeneratorContext.commitResource(testLogger, testOutputStream)).andReturn(
        mockGeneratedResource);

    JMethod method = EasyMock.createMock(JMethod.class);
    JClassType bundleType = EasyMock.createMock(JClassType.class);
    EasyMock.expect(method.getEnclosingType()).andReturn(bundleType);
    EasyMock.expect(bundleType.getQualifiedSourceName()).andReturn("com.test.Bundle");
    EasyMock.expect(method.getName()).andReturn("cssMethod");

    EasyMock.replay(mockResourceContext);
    EasyMock.replay(mockGeneratorContext);
    EasyMock.replay(mockJMethod1);
    EasyMock.replay(mockJMethod2);
    EasyMock.replay(mockJMethod3);
    EasyMock.replay(mockJClassType1);
    EasyMock.replay(mockJClassType2);
    EasyMock.replay(mockJClassType3);
    EasyMock.replay(method);
    EasyMock.replay(bundleType);

    CssResourceGenerator crg = new CssResourceGenerator();
    crg.outputCssMapArtifact(testLogger, mockResourceContext, method, testMap);
    String expectedOutput = "test-class-type-1-basename1,TESTCSSNAME1\n" +
        "test-class-type-2-basename2,TESTCSSNAME2\n" +
        "test-class-type-3-basename3,TESTCSSNAME3\n";
    assertEquals(expectedOutput, testOutputStream.toString());

    testLogger.assertCorrectLogEntries();
    EasyMock.verify(mockResourceContext);
    EasyMock.verify(mockGeneratorContext);
    EasyMock.verify(mockJMethod1);
    EasyMock.verify(mockJMethod2);
    EasyMock.verify(mockJMethod3);
    EasyMock.verify(mockJClassType1);
    EasyMock.verify(mockJClassType2);
    EasyMock.verify(mockJClassType3);
    EasyMock.verify(method);
    EasyMock.verify(bundleType);
  }

}
