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

import com.google.gwt.dom.builder.client.GwtAnchorBuilderTest;
import com.google.gwt.dom.builder.client.GwtAreaBuilderTest;
import com.google.gwt.dom.builder.client.GwtAudioBuilderTest;
import com.google.gwt.dom.builder.client.GwtBRBuilderTest;
import com.google.gwt.dom.builder.client.GwtBaseBuilderTest;
import com.google.gwt.dom.builder.client.GwtBodyBuilderTest;
import com.google.gwt.dom.builder.client.GwtButtonBuilderTest;
import com.google.gwt.dom.builder.client.GwtCanvasBuilderTest;
import com.google.gwt.dom.builder.client.GwtDListBuilderTest;
import com.google.gwt.dom.builder.client.GwtDivBuilderTest;
import com.google.gwt.dom.builder.client.GwtDomBuilderImplTest;
import com.google.gwt.dom.builder.client.GwtDomStylesBuilderTest;
import com.google.gwt.dom.builder.client.GwtFieldSetBuilderTest;
import com.google.gwt.dom.builder.client.GwtFormBuilderTest;
import com.google.gwt.dom.builder.client.GwtFrameBuilderTest;
import com.google.gwt.dom.builder.client.GwtFrameSetBuilderTest;
import com.google.gwt.dom.builder.client.GwtHRBuilderTest;
import com.google.gwt.dom.builder.client.GwtHeadBuilderTest;
import com.google.gwt.dom.builder.client.GwtHeadingBuilderTest;
import com.google.gwt.dom.builder.client.GwtIFrameBuilderTest;
import com.google.gwt.dom.builder.client.GwtImageBuilderTest;
import com.google.gwt.dom.builder.client.GwtInputBuilderTest;
import com.google.gwt.dom.builder.client.GwtLIBuilderTest;
import com.google.gwt.dom.builder.client.GwtLabelBuilderTest;
import com.google.gwt.dom.builder.client.GwtLegendBuilderTest;
import com.google.gwt.dom.builder.client.GwtLinkBuilderTest;
import com.google.gwt.dom.builder.client.GwtMapBuilderTest;
import com.google.gwt.dom.builder.client.GwtMetaBuilderTest;
import com.google.gwt.dom.builder.client.GwtOListBuilderTest;
import com.google.gwt.dom.builder.client.GwtOptGroupBuilderTest;
import com.google.gwt.dom.builder.client.GwtOptionBuilderTest;
import com.google.gwt.dom.builder.client.GwtParagraphBuilderTest;
import com.google.gwt.dom.builder.client.GwtParamBuilderTest;
import com.google.gwt.dom.builder.client.GwtPreBuilderTest;
import com.google.gwt.dom.builder.client.GwtQuoteBuilderTest;
import com.google.gwt.dom.builder.client.GwtScriptBuilderTest;
import com.google.gwt.dom.builder.client.GwtSelectBuilderTest;
import com.google.gwt.dom.builder.client.GwtSourceBuilderTest;
import com.google.gwt.dom.builder.client.GwtSpanBuilderTest;
import com.google.gwt.dom.builder.client.GwtStyleBuilderTest;
import com.google.gwt.dom.builder.client.GwtTableBuilderTest;
import com.google.gwt.dom.builder.client.GwtTableCaptionBuilderTest;
import com.google.gwt.dom.builder.client.GwtTableCellBuilderTest;
import com.google.gwt.dom.builder.client.GwtTableColBuilderTest;
import com.google.gwt.dom.builder.client.GwtTableRowBuilderTest;
import com.google.gwt.dom.builder.client.GwtTableSectionBuilderTest;
import com.google.gwt.dom.builder.client.GwtTextAreaBuilderTest;
import com.google.gwt.dom.builder.client.GwtTitleBuilderTest;
import com.google.gwt.dom.builder.client.GwtUListBuilderTest;
import com.google.gwt.dom.builder.client.GwtVideoBuilderTest;
import com.google.gwt.dom.builder.shared.GwtHtmlBuilderImplTest;
import com.google.gwt.dom.builder.shared.GwtHtmlStylesBuilderTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests of the dom and HTML implementation of element builders.
 */
public class ElementBuilderGwtSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("GWT tests for all builders");

    // Html implementation tests.
    suite.addTestSuite(GwtHtmlBuilderImplTest.class);
    suite.addTestSuite(GwtHtmlStylesBuilderTest.class);

    // DOM implementation tests.
    suite.addTestSuite(GwtDomBuilderImplTest.class);
    suite.addTestSuite(GwtDomStylesBuilderTest.class);

    // Element builder tests.
    suite.addTestSuite(GwtAnchorBuilderTest.class);
    suite.addTestSuite(GwtAreaBuilderTest.class);
    suite.addTestSuite(GwtAudioBuilderTest.class);
    suite.addTestSuite(GwtBaseBuilderTest.class);
    suite.addTestSuite(GwtBodyBuilderTest.class);
    suite.addTestSuite(GwtBRBuilderTest.class);
    suite.addTestSuite(GwtButtonBuilderTest.class);
    suite.addTestSuite(GwtCanvasBuilderTest.class);
    suite.addTestSuite(GwtDivBuilderTest.class);
    suite.addTestSuite(GwtDListBuilderTest.class);
    suite.addTestSuite(GwtFieldSetBuilderTest.class);
    suite.addTestSuite(GwtFormBuilderTest.class);
    suite.addTestSuite(GwtFrameBuilderTest.class);
    suite.addTestSuite(GwtFrameSetBuilderTest.class);
    suite.addTestSuite(GwtHeadBuilderTest.class);
    suite.addTestSuite(GwtHeadingBuilderTest.class);
    suite.addTestSuite(GwtHRBuilderTest.class);
    suite.addTestSuite(GwtIFrameBuilderTest.class);
    suite.addTestSuite(GwtImageBuilderTest.class);
    suite.addTestSuite(GwtInputBuilderTest.class);
    suite.addTestSuite(GwtLabelBuilderTest.class);
    suite.addTestSuite(GwtLegendBuilderTest.class);
    suite.addTestSuite(GwtLIBuilderTest.class);
    suite.addTestSuite(GwtLinkBuilderTest.class);
    suite.addTestSuite(GwtMapBuilderTest.class);
    suite.addTestSuite(GwtMetaBuilderTest.class);
    suite.addTestSuite(GwtOListBuilderTest.class);
    suite.addTestSuite(GwtOptGroupBuilderTest.class);
    suite.addTestSuite(GwtOptionBuilderTest.class);
    suite.addTestSuite(GwtParagraphBuilderTest.class);
    suite.addTestSuite(GwtParamBuilderTest.class);
    suite.addTestSuite(GwtPreBuilderTest.class);
    suite.addTestSuite(GwtQuoteBuilderTest.class);
    suite.addTestSuite(GwtScriptBuilderTest.class);
    suite.addTestSuite(GwtSelectBuilderTest.class);
    suite.addTestSuite(GwtSourceBuilderTest.class);
    suite.addTestSuite(GwtSpanBuilderTest.class);
    suite.addTestSuite(GwtStyleBuilderTest.class);
    suite.addTestSuite(GwtTableCaptionBuilderTest.class);
    suite.addTestSuite(GwtTableCellBuilderTest.class);
    suite.addTestSuite(GwtTableColBuilderTest.class);
    suite.addTestSuite(GwtTableBuilderTest.class);
    suite.addTestSuite(GwtTableRowBuilderTest.class);
    suite.addTestSuite(GwtTableSectionBuilderTest.class);
    suite.addTestSuite(GwtTextAreaBuilderTest.class);
    suite.addTestSuite(GwtTitleBuilderTest.class);
    suite.addTestSuite(GwtUListBuilderTest.class);
    suite.addTestSuite(GwtVideoBuilderTest.class);

    return suite;
  }
}
