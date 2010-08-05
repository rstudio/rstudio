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

import com.google.gwt.text.shared.Parser;

import java.text.ParseException;

class MockParser implements Parser<String> {
  public boolean throwException;
  public boolean parseCalled;

  public String parse(CharSequence text) throws ParseException {
    parseCalled = true;
    
    if (throwException) {
      throw new ParseException("mock exception", 0);
    } else {
      return text.toString();
    }
  } 
}