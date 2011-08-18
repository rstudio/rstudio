/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.core.ext.typeinfo.JType;

import junit.framework.TestCase;

/**
 * Tests for {@link FieldReferenceConverter}.
 */
public class FieldReferenceConverterTest extends TestCase {

  FieldReferenceConverter.Delegate frDelegate = new FieldReferenceConverter.Delegate() {
    public String handleFragment(String path) {
      return "*" + path + "*";
    }

    public String handleReference(String reference) {
      return String.format(" & %s & ", reference);
    }
    
    public JType[] getTypes() {
      return null;
    }
  };
  FieldReferenceConverter converter = new FieldReferenceConverter(null);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testNone() {
    String before = "able.baker.charlie";
    String expected = "*able.baker.charlie*";

    assertEquals(expected, converter.convert(before, frDelegate));
  }

  public void testOne() {
    String before = "{baker}";
    String expected = "** & baker & **";

    assertEquals(expected, converter.convert(before, frDelegate));
  }
  
  public void testReplaceSimple() {
    String before = "able {baker} charlie";
    String expected = "*able * & baker & * charlie*";

    assertEquals(expected, converter.convert(before, frDelegate));
  }

  public void testDashes() {
    String before = "{foo-bar.baz-bangZoom.zip-zap}";
    String expected = "** & fooBar.bazBangZoom().zipZap() & **";
    
    assertEquals(expected, converter.convert(before, frDelegate));
  }
  
  public void testReplaceSeveral() {
    String before = "{foo.bar.baz} baker {bang.zoom} delta {zap}";
    String expected = "** & foo.bar().baz() & * baker * & bang.zoom() & * delta * & zap & **";

    assertEquals(expected, converter.convert(before, frDelegate));
  }
  
  public void testEscaping() {
    String before = "Well {{Hi mom}!";
    String expected = "*Well {Hi mom}!*";

    assertEquals(expected, converter.convert(before, frDelegate));
  }

  public void testIgnoreEmpty() {
    String before = "Hi {} mom";
    String expected = "*Hi {} mom*";

    assertEquals(expected, converter.convert(before, frDelegate));
  }

  public void testIgnoreNonIdentifierFirstChar() {
    String before = "Hi { } mom, how { are } {1you}?";
    String expected = "*Hi { } mom, how { are } {1you}?*";

    assertEquals(expected, converter.convert(before, frDelegate));
  }
  
  public void testHasFieldReferences() {
    assertTrue(FieldReferenceConverter.hasFieldReferences("{able} {baker}"));
    assertFalse(FieldReferenceConverter.hasFieldReferences("{{able} baker { Charlie }"));
  }
}
