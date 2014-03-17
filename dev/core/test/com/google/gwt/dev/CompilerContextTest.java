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
package com.google.gwt.dev;

import junit.framework.TestCase;

/**
 * Tests for CompilerContext.
 */
public class CompilerContextTest extends TestCase {

  public void testDefaultLibraryGroup() {
    CompilerContext compilerContext = new CompilerContext();

    // Doesn't throw an NPE.
    assertFalse(compilerContext.getLibraryGroup().containsBuildResource("com/google/gwt/Foo.xml"));

    compilerContext = new CompilerContext.Builder().build();

    // Doesn't throw an NPE.
    assertFalse(compilerContext.getLibraryGroup().containsBuildResource("com/google/gwt/Foo.xml"));
  }
}
