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
package com.google.gwt.dom.builder;

import com.google.gwt.dom.builder.shared.HtmlAnchorBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlAreaBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlAudioBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlBRBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlBaseBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlBodyBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlButtonBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlCanvasBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlDListBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlDivBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlFieldSetBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlFormBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlFrameBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlFrameSetBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlHRBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlHeadBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlHeadingBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlIFrameBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlImageBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlInputBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlLIBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlLabelBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlLegendBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlLinkBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlMapBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlMetaBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlOListBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlOptGroupBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlOptionBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlParagraphBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlParamBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlPreBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlQuoteBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlScriptBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlSelectBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlSourceBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlSpanBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlStyleBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlTableBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlTableCaptionBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlTableCellBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlTableColBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlTableRowBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlTableSectionBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlTextAreaBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlTitleBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlUListBuilderTest;
import com.google.gwt.dom.builder.shared.HtmlVideoBuilderTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests of the html implementation of element builders.
 */
public class HtmlBuilderJreSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("JRE tests for all html builders");

    // Element builders.
    suite.addTestSuite(HtmlAnchorBuilderTest.class);
    suite.addTestSuite(HtmlAreaBuilderTest.class);
    suite.addTestSuite(HtmlAudioBuilderTest.class);
    suite.addTestSuite(HtmlBaseBuilderTest.class);
    suite.addTestSuite(HtmlBodyBuilderTest.class);
    suite.addTestSuite(HtmlBRBuilderTest.class);
    suite.addTestSuite(HtmlButtonBuilderTest.class);
    suite.addTestSuite(HtmlCanvasBuilderTest.class);
    suite.addTestSuite(HtmlDivBuilderTest.class);
    suite.addTestSuite(HtmlDListBuilderTest.class);
    suite.addTestSuite(HtmlFieldSetBuilderTest.class);
    suite.addTestSuite(HtmlFormBuilderTest.class);
    suite.addTestSuite(HtmlFrameBuilderTest.class);
    suite.addTestSuite(HtmlFrameSetBuilderTest.class);
    suite.addTestSuite(HtmlHeadBuilderTest.class);
    suite.addTestSuite(HtmlHeadingBuilderTest.class);
    suite.addTestSuite(HtmlHRBuilderTest.class);
    suite.addTestSuite(HtmlIFrameBuilderTest.class);
    suite.addTestSuite(HtmlImageBuilderTest.class);
    suite.addTestSuite(HtmlInputBuilderTest.class);
    suite.addTestSuite(HtmlLabelBuilderTest.class);
    suite.addTestSuite(HtmlLegendBuilderTest.class);
    suite.addTestSuite(HtmlLIBuilderTest.class);
    suite.addTestSuite(HtmlLinkBuilderTest.class);
    suite.addTestSuite(HtmlMapBuilderTest.class);
    suite.addTestSuite(HtmlMetaBuilderTest.class);
    suite.addTestSuite(HtmlOListBuilderTest.class);
    suite.addTestSuite(HtmlOptGroupBuilderTest.class);
    suite.addTestSuite(HtmlOptionBuilderTest.class);
    suite.addTestSuite(HtmlParagraphBuilderTest.class);
    suite.addTestSuite(HtmlParamBuilderTest.class);
    suite.addTestSuite(HtmlPreBuilderTest.class);
    suite.addTestSuite(HtmlQuoteBuilderTest.class);
    suite.addTestSuite(HtmlScriptBuilderTest.class);
    suite.addTestSuite(HtmlSelectBuilderTest.class);
    suite.addTestSuite(HtmlSourceBuilderTest.class);
    suite.addTestSuite(HtmlSpanBuilderTest.class);
    suite.addTestSuite(HtmlStyleBuilderTest.class);
    suite.addTestSuite(HtmlTableCaptionBuilderTest.class);
    suite.addTestSuite(HtmlTableCellBuilderTest.class);
    suite.addTestSuite(HtmlTableColBuilderTest.class);
    suite.addTestSuite(HtmlTableBuilderTest.class);
    suite.addTestSuite(HtmlTableRowBuilderTest.class);
    suite.addTestSuite(HtmlTableSectionBuilderTest.class);
    suite.addTestSuite(HtmlTextAreaBuilderTest.class);
    suite.addTestSuite(HtmlTitleBuilderTest.class);
    suite.addTestSuite(HtmlUListBuilderTest.class);
    suite.addTestSuite(HtmlVideoBuilderTest.class);

    return suite;
  }
}
