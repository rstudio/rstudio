/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.text.shared.testing.PassthroughRenderer;

import java.text.ParseException;

/**
 * Testing ValueBoxBase.
 */
public class ValueBoxBaseTest extends GWTTestCase {
  
  // Test that parser exceptions are correctly thrown with an empty string
  public void testParserExceptionWithEmptyString() {
    Element elm = Document.get().createTextInputElement();
    Renderer<String> renderer = PassthroughRenderer.instance();
    MockParser parser = new MockParser();
    
    ValueBoxBase<String> valueBoxBase = 
      new ValueBoxBase<String>(elm, renderer, parser) {
    };
    
    parser.throwException = true;
    valueBoxBase.setText("");
    try {
      valueBoxBase.getValueOrThrow();
      fail("Should have thrown ParseException");
    } catch (ParseException e) {
      // exception was correctly thrown
    }
    if (!parser.parseCalled) {
      fail("Parser was not run");
    }
  }
  
  // Test that parser exceptions are correctly thrown with a simple string
  public void testParserExceptionWithString() {
    Element elm = Document.get().createTextInputElement();
    Renderer<String> renderer = PassthroughRenderer.instance();
    MockParser parser = new MockParser();
    
    ValueBoxBase<String> valueBoxBase = 
      new ValueBoxBase<String>(elm, renderer, parser) {
    };
    
    parser.throwException = true;
    valueBoxBase.setText("simple string");
    try {
      valueBoxBase.getValueOrThrow();
      fail("Should have thrown ParseException");
    } catch (ParseException e) {
      // exception was correctly thrown
    }
    if (!parser.parseCalled) {
      fail("Parser was not run");
    }
  }
  
  // Test that a string with padding spaces correctly passes through
  public void testSpaces() throws ParseException {
    Element elm = Document.get().createTextInputElement();
    Renderer<String> renderer = PassthroughRenderer.instance();
    MockParser parser = new MockParser();
    
    ValueBoxBase<String> valueBoxBase = 
      new ValueBoxBase<String>(elm, renderer, parser) {
    };
    
    String text = "  two space padding test  ";
    valueBoxBase.setText(text);
    assertEquals(text, valueBoxBase.getValueOrThrow());
    if (!parser.parseCalled) {
      fail("Parser was not run");
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }
}