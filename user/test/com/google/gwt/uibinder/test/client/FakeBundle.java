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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;

/**
 * Faux bundle used by test.
 */
public class FakeBundle {
  public boolean aBoolean() {
    return true;
  }

  public Boolean aBooleanObject() {
    return false;
  }

  public double aDouble() {
    return 42;
  }

  public Double aDoubleObject() {
    return 21.0;
  }
  
  public String aGifPath() {
    return "www.google.com/images/logo_sm.gif";
  }
  
  public int anInt() {
    return 42;
  }

  public Integer anIntegerObject() {
    return 21;
  }

  public String anUnsafeUri() {
    return "javascript:void(0)";
  }

  public SafeUri aSafeUri() {
    return UriUtils.fromSafeConstant(anUnsafeUri());
  }
  
  public String aSelector() {
    return "http://";
  }

  public String aUrl() {
    return aSelector() + aGifPath();
  }
  
  public String helloText() {
    return "hello";
  }
  
  public ArbitraryPojo pojo() {
    return new ArbitraryPojo();
  }
}
