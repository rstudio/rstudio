/*
 * Copyright 2014 Google Inc.
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

package com.google.gwt.resources.gss;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.gwt.thirdparty.common.css.SourceCode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssParser;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssParserException;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import junit.framework.TestCase;

/**
 * Base class for all test that want to build an ast based on css strings.
 */
public abstract class BaseGssTest extends TestCase {
  /**
   * Parse the css given in parameter and return the corresponding CssTree.
   */
  protected CssTree parseAndBuildTree(String source) {
    CssTree cssTree = parse(source);

    ErrorManager errorManager = mock(ErrorManager.class);

    runPassesOnNewTree(cssTree, errorManager);

    // we don't expect a failure here
    verify(errorManager, never()).report(any(GssError.class));

    return cssTree;
  }

  /**
   * Run the Passes needed to have a well formed ast needed for the test.
   * <p/>
   * This method should be overridden by concrete class in order to run visitor they need to get
   * a expected ast to use in the test.
   */
  protected void runPassesOnNewTree(CssTree cssTree, ErrorManager errorManager) {
  }

  protected String lines(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  private CssTree parse(String source) {
    try {
      return new GssParser(new SourceCode("test", source)).parse();
    } catch (GssParserException e) {
      fail(e.getMessage());
    }
    return null;
  }
}
