/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Collections;

/**
 * Eponymous test class.
 */
public class FieldWriterOfGeneratedCssResourceTest extends TestCase {
  private TypeOracle types;

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    CompilationState state = CompilationStateBuilder.buildFrom(
        createCompileLogger(), UiJavaResources.getUiResources());
    types = state.getTypeOracle();
  }

  public void testCamelMatchesDashes() {
    JClassType stringType = types.findType("java.lang.String");
    JClassType cssResourceType = stringType; // TODO(rjrjr) get real someday

    ImplicitCssResource css = new ImplicitCssResource("package", "ClassName",
        "fieldName", new String[] {}, cssResourceType, ".able-baker {}",
        MortalLogger.NULL, Collections.<JClassType> emptySet());

    FieldWriterOfGeneratedCssResource f = new FieldWriterOfGeneratedCssResource(
        null, stringType, css, MortalLogger.NULL);

    assertEquals(stringType, f.getReturnType(new String[] {
        "fieldName", "ableBaker"}, new MonitoredLogger(MortalLogger.NULL)));
    assertEquals(FieldWriterType.GENERATED_CSS, f.getFieldType());
  }

  public void testDashesMatchesCamels() {
    JClassType stringType = types.findType("java.lang.String");
    JClassType cssResourceType = stringType; // TODO(rjrjr) get real someday

    ImplicitCssResource css = new ImplicitCssResource("package", "ClassName",
        "fieldName", new String[] {}, cssResourceType, ".ableBaker {}",
        MortalLogger.NULL, Collections.<JClassType> emptySet());

    FieldWriterOfGeneratedCssResource f = new FieldWriterOfGeneratedCssResource(
        null, stringType, css, MortalLogger.NULL);

    assertEquals(stringType, f.getReturnType(new String[] {
        "fieldName", "able-baker"}, new MonitoredLogger(MortalLogger.NULL)));

    assertEquals(FieldWriterType.GENERATED_CSS, f.getFieldType());
  }
}
