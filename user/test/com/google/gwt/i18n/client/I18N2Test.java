/*
 * Copyright 2007 Google Inc.
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

package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.gen.Colors;
import com.google.gwt.i18n.client.gen.TestBadKeys;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * TODO: document me.
 */
public class I18N2Test extends GWTTestCase {
  public String getModuleName() {
    return "com.google.gwt.i18n.I18N2Test";
  }

  public void testBadKeys() {
    TestBadKeys test = (TestBadKeys) GWT.create(TestBadKeys.class);
    assertEquals("zh_spacer", test.zh_spacer());
    assertEquals("zh_spacer", test.getString("zh_spacer"));
    assertEquals("logger_org_hibernate_jdbc", test.logger_org_hibernate_jdbc());
    assertEquals("logger_org_hibernate_jdbc", test
      .getString("logger_org_hibernate_jdbc"));
    assertEquals("cell_2_5", test.cell_2_5());
    assertEquals("cell_2_5", test.getString("cell_2_5"));
    assertEquals("_level", test._level());
    assertEquals("_level", test.getString("_level"));
    assertEquals("__s", test.__s());
    assertEquals("__s", test.getString("__s"));
    assertEquals(
      "________________________________________________________________", test
        .________________________________________________________________());
    assertEquals(
      "________________________________________________________________",
      test
        .getString("________________________________________________________________"));
    assertEquals("_", test._());
    assertEquals("_", test.getString("_"));
    assertEquals("maven_jdiff_old_tag", test.maven_jdiff_old_tag());
    assertEquals("maven_jdiff_old_tag", test.getString("maven_jdiff_old_tag"));
    assertEquals("maven_checkstyle_properties", test
      .maven_checkstyle_properties());
    assertEquals("maven_checkstyle_properties", test
      .getString("maven_checkstyle_properties"));
    assertEquals("_1_2_3_4", test._1_2_3_4());
    assertEquals("_1_2_3_4", test.getString("_1_2_3_4"));
    assertEquals("entity_160", test.entity_160());
    assertEquals("entity_160", test.getString("entity_160"));
    assertEquals("a__b", test.a__b());
    assertEquals("a__b", test.getString("a__b"));
    assertEquals("AWT_f5", test.AWT_f5());
    assertEquals("AWT_f5", test.getString("AWT_f5"));
    assertEquals("Cursor_MoveDrop_32x32_File", test
      .Cursor_MoveDrop_32x32_File());
    assertEquals("Cursor_MoveDrop_32x32_File", test
      .getString("Cursor_MoveDrop_32x32_File"));
    assertEquals("_c_____", test._c_____());
    assertEquals("_c_____", test.getString("_c_____"));
    assertEquals("__s_dup", test.__s_dup());
    assertEquals("__s_dup", test.getString("__s_dup"));
    assertEquals("__dup", test.__dup());
    assertEquals("__dup", test.getString("__dup"));
    assertEquals("AWT_end", test.AWT_end());
    assertEquals("AWT_end", test.getString("AWT_end"));
    assertEquals("permissions_755", test.permissions_755());
    assertEquals("permissions_755", test.getString("permissions_755"));
    assertEquals("a_b_c", test.a_b_c());
    assertEquals("a_b_c", test.getString("a_b_c"));
    assertEquals("__s_dup_dup", test.__s_dup_dup());
  }

  public void testBinding() {
    TestBinding t = (TestBinding) GWT.create(TestBinding.class);
    assertEquals("b_c_d", t.a());
    assertEquals("default", t.b());
  }

  public void testCheckColorsAndShapes() {
    ColorsAndShapes s = (ColorsAndShapes) GWT.create(ColorsAndShapes.class);
    // should not have changed, because we included no shapesAndColor info.
    assertEquals("ýéļļöŵ", s.yellow());
  }

  public void testWalkUpColorTree() {
    Colors colors = (Colors) GWT.create(Colors.class);
    assertEquals("red_b_C_d", colors.red());
    assertEquals("blue_b_C", colors.blue());
    assertEquals("yellow_b", colors.yellow());
  }

}
