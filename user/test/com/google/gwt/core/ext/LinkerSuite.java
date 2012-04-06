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
package com.google.gwt.core.ext;

import com.google.gwt.core.ext.linker.impl.SelectionScriptJavaScriptTest;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinkerUnitTest;
import com.google.gwt.core.ext.test.CrossSiteIframeLinkerTest;
import com.google.gwt.core.ext.test.IFrameLinkerTest;
import com.google.gwt.core.ext.test.SingleScriptLinkerTest;
import com.google.gwt.core.ext.test.XSLinkerTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Runs the linker tests. See the subclasses of
 * {@link com.google.gwt.core.ext.test.LinkerTest}.
 */
public class LinkerSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Smoke test for linkers");

    // $JUnit-BEGIN$
    suite.addTestSuite(CrossSiteIframeLinkerTest.class);
    suite.addTestSuite(IFrameLinkerTest.class);
    suite.addTestSuite(LinkerUnitTest.class);
    suite.addTestSuite(SelectionScriptJavaScriptTest.class);
    suite.addTestSuite(SelectionScriptLinkerUnitTest.class);
    suite.addTestSuite(XSLinkerTest.class);
    suite.addTestSuite(SingleScriptLinkerTest.class);
    // $JUnit-END$
    return suite;
  }
}
