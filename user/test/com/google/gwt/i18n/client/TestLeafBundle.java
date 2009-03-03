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

/**
 * TODO: document me.
 */
public abstract class TestLeafBundle implements TestBinding {
  abstract public String b();
}

class TestLeafBundleImpl extends TestLeafBundle {

  @Override
  public String b() {
    return "TestLeafBundleImpl";
  }

  public String a() {
    throw new IllegalStateException("bad");
  }
}

class TestLeafBundle_piglatin extends TestLeafBundleImpl {

  @Override
  public String b() {
    return "TestLeafBundle_piglatin";
  }
}

class TestLeafBundle_piglatin_UK extends TestLeafBundle_piglatin {

  @Override
  public String b() {
    return "TestLeafBundle_piglatin_UK";
  }
}

class TestLeafBundle_piglatin_UK_WINDOWS extends TestLeafBundleImpl {

  @Override
  public String b() {
    return "TestLeafBundle_piglatin_UK_WINDOWS";
  }
}
