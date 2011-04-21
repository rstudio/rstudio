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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link TypeHierarchyUtils}.
 */
public class TypeHierarchyUtilsTest extends TestCase {
  private static TreeLogger createLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  public void testParameterizedInterface() throws NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    {
      StringBuilder code = new StringBuilder();
      code.append("interface A<T> { }\n");
      resources.add(new StaticJavaResource("A", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("interface B extends A<String> { }\n");
      resources.add(new StaticJavaResource("B", code));
    }
    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildStandardTypeOracleWith(logger,
        resources);
    JClassType a = to.getType("A");
    JClassType b = to.getType("B");

    List<JClassType> subtypesOfA = TypeHierarchyUtils.getImmediateSubtypes(a);
    assertEquals(1, subtypesOfA.size());
    assertTrue(subtypesOfA.contains(b));
  }
}
