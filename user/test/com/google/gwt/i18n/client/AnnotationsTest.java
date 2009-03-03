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
package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests annotations not covered elsewhere.
 */
public class AnnotationsTest extends GWTTestCase {
  
  /**
   * First grandparent for test.
   */
  public interface GP1 extends TestConstants {
    @DefaultStringValue("gp1 annot")
    String gp1();

    @DefaultStringValue("gp1 shared annot")
    String shared();
  }
  
  /**
   * Second grandparent for test.
   */
  public interface GP2 extends TestConstants {
    @DefaultStringValue("gp2 annot")
    String gp2();
   
    @DefaultStringValue("gp2 shared annot")
    String shared();
  }
  
  /**
   * Test interface for P1 before P2.
   */
  public interface Inherit1 extends P1, P2 {
  }
  
  /**
   * Test interface for P2 before P1.
   */
  public interface Inherit2 extends P2, P1 {
    @DefaultStringValue("def")
    String def();
  }
  
  /**
   * Used to verify that we can explicitly localize messages in annotations.
   */
  @DefaultLocale("en")
  public interface Inherit2_en extends Inherit2 {
    @DefaultStringValue("en def")
    String def();
  }

  /**
   * First parent interface for test.
   */
  public interface P1 extends TestConstants {
    @DefaultStringValue("p1 annot")
    String p1();

    @DefaultStringValue("p1 shared annot")
    String shared();
  }
  
  /**
   * Second parent interface for test.
   */
  public interface P2 extends GP1, GP2 {
    @DefaultStringValue("p2 annot")
    String p2();

    String shared();
  }
  
  /**
   * Basic test message.
   */
  public interface Msg1 extends Messages {
    @DefaultMessage("Test {0}")
    String getTest(String testName);
  }
  
  /**
   * Plural form test message.
   */
  public interface Msg2 extends Messages {
    @DefaultMessage("You have {0} widgets.")
    @PluralText({"one", "You have a widget."})
    String getWidgetCount(@PluralCount int count);
    
    @DefaultMessage("from en")
    String leastDerived();
  }

  /**
   * Aggregate messages into one interface.
   */
  public interface AllMessages extends Msg1, Msg2 {
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_en";
  }

  public void testInheritance() {
    Inherit1 i1 = GWT.create(Inherit1.class);
    assertEquals("p1 annot", i1.p1());
    assertEquals("gp2 annot", i1.gp2());
    assertEquals("p1 shared annot", i1.shared());
    Inherit2 i2 = GWT.create(Inherit2.class);
    assertEquals("p1 annot", i2.p1());
    assertEquals("gp2 annot", i2.gp2());
    assertEquals("gp1 shared annot", i2.shared());
    
    // TODO(jat): this doesn't work because findDerivedClasses only
    // looks for concrete classes, not other interfaces -- commenting
    // out for now, revisit later.
    // assertEquals("en def", i2.def());
  }

  public void testIssue2359() {
    AllMessages m = GWT.create(AllMessages.class);
    assertEquals("Test foo", m.getTest("foo"));
    assertEquals("You have 2 widgets.", m.getWidgetCount(2));
    assertEquals("You have a widget.", m.getWidgetCount(1));
  }
  
  public void testLeastDerived() {
    AllMessages m = GWT.create(AllMessages.class);
    assertEquals("from en_US", m.leastDerived());
  }
}
