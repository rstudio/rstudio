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
package com.google.gwt.dev.jjs.impl.gflow;

import com.google.gwt.dev.jjs.impl.gflow.cfg.CfgBuilderTest;
import com.google.gwt.dev.jjs.impl.gflow.constants.ConstantsAssumptionTest;
import com.google.gwt.dev.jjs.impl.gflow.constants.ExpressionEvaluatorTest;
import com.google.gwt.dev.jjs.impl.gflow.constants.AssumptionsDeducerTest;
import com.google.gwt.dev.jjs.impl.gflow.constants.ConstantsAnalysisTest;
import com.google.gwt.dev.jjs.impl.gflow.constants.ConstantsAnalysisTransformationTest;
import com.google.gwt.dev.jjs.impl.gflow.copy.CopyAnalysisTest;
import com.google.gwt.dev.jjs.impl.gflow.copy.CopyAnalysisTransformationTest;
import com.google.gwt.dev.jjs.impl.gflow.liveness.LivenessAnalysisTest;
import com.google.gwt.dev.jjs.impl.gflow.liveness.LivenessTransformationTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All tests for gflow framework in bottom-to-top architecture order
 * (i.e. if lower test fails, then all further tests might fail too).
 */
public class GflowTests {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(CfgBuilderTest.class);
    suite.addTestSuite(ConstantsAssumptionTest.class);
    suite.addTestSuite(AssumptionsDeducerTest.class);
    suite.addTestSuite(ExpressionEvaluatorTest.class);
    suite.addTestSuite(ConstantsAnalysisTest.class);
    suite.addTestSuite(ConstantsAnalysisTransformationTest.class);
    suite.addTestSuite(LivenessAnalysisTest.class);
    suite.addTestSuite(LivenessTransformationTest.class);
    suite.addTestSuite(CopyAnalysisTest.class);
    suite.addTestSuite(CopyAnalysisTransformationTest.class);
    suite.addTestSuite(DataflowOptimizerTest.class);
    return suite;
  }
}
