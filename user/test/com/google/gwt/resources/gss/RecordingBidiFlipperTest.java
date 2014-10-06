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

import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;

/**
 * Test class for {@link RecordingBidiFlipper}.
 */
public class RecordingBidiFlipperTest extends BaseGssTest {

  public void testFlipWithRuleValueIsRecorded() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        ".foo {",
        "  background-position: 10% 20%;",
        "}"));

    RecordingBidiFlipper visitor = new RecordingBidiFlipper(cssTree.getMutatingVisitController(),
        false, false, true);

    // when
    visitor.runPass();

    // then
    assertEquals(true, visitor.nodeFlipped());
  }

  public void testFlipWithRuleNameIsRecorded() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        ".foo {",
        "  padding-left: 15px;",
        "}"));

    RecordingBidiFlipper visitor = new RecordingBidiFlipper(cssTree.getMutatingVisitController(),
        false, false, true);

    // when
    visitor.runPass();

    // then
    assertEquals(true, visitor.nodeFlipped());
  }

  public void testNoFlipNothingIsRecorder() {
    // given
    CssTree cssTree = parseAndBuildTree(lines(
        ".foo {",
        "  background-color: black;",
        "  color: white;",
        "  font-size: 12px;",
        "  padding: 5px;",
        "}"));

    RecordingBidiFlipper visitor = new RecordingBidiFlipper(cssTree.getMutatingVisitController(),
        false, false, true);

    // when
    visitor.runPass();

    // then
    assertEquals(false, visitor.nodeFlipped());
  }
}
