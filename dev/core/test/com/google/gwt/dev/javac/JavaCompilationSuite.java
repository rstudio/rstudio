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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.BootStrapPlatform;
import com.google.gwt.dev.javac.asm.CollectClassDataTest;
import com.google.gwt.dev.javac.asm.CollectReferencesVisitorTest;
import com.google.gwt.dev.javac.asm.ResolveGenericsTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests script and resource injection.
 */
public class JavaCompilationSuite {

  static {
    /*
     * Required for OS X Leopard. This call ensures we have a valid context
     * ClassLoader. Many of the tests test low-level RPC mechanisms and rely on
     * a ClassLoader to resolve classes and resources.
     */
    BootStrapPlatform.applyPlatformHacks();
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(JavaCompilationSuite.class.getName());

    suite.addTestSuite(ArtificialRescueCheckerTest.class);
    suite.addTestSuite(BinaryTypeReferenceRestrictionsCheckerTest.class);
    suite.addTestSuite(BytecodeSignatureMakerTest.class);
    suite.addTestSuite(CompilationStateTest.class);
    suite.addTestSuite(CompilationUnitFileReferenceTest.class);
    suite.addTestSuite(CompiledClassTest.class);
    suite.addTestSuite(GWTProblemTest.class);
    suite.addTestSuite(JavaSourceParserTest.class);
    suite.addTestSuite(JdtBehaviorTest.class);
    suite.addTestSuite(JdtCompilerTest.class);
    suite.addTestSuite(Java7JdtCompilerTest.class);
    suite.addTestSuite(JsniReferenceResolverTest.class);
    suite.addTestSuite(JsniCollectorTest.class);
    suite.addTestSuite(JSORestrictionsTest.class);
    suite.addTestSuite(MemoryUnitCacheTest.class);
    suite.addTestSuite(PersistentUnitCacheTest.class);
    suite.addTestSuite(CompilationStateBuilderTest.class);
    suite.addTestSuite(CompilationUnitTypeOracleUpdaterFromByteCodeTest.class);
    suite.addTestSuite(CompilationUnitTypeOracleUpdaterFromSourceTest.class);
    suite.addTestSuite(LibraryCompilationUnitTypeOracleUpdaterFromSourceTest.class);

    suite.addTestSuite(CollectClassDataTest.class);
    suite.addTestSuite(CollectReferencesVisitorTest.class);
    suite.addTestSuite(ResolveGenericsTest.class);

    // TODO: Move these to another package.
    suite.addTestSuite(GeneratedClassnameComparatorTest.class);
    suite.addTestSuite(GeneratedClassnameFinderTest.class);
    suite.addTestSuite(GeneratedClassnameTest.class);

    return suite;
  }
}
