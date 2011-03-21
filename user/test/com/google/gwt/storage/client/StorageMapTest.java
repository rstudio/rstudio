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

package com.google.gwt.storage.client;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

import java.util.Map;

/**
 * Tests {@link StorageMap}.
 * 
 * Because HtmlUnit does not support Storage, you will need to run these tests
 * manually by adding this line to your VM args: -Dgwt.args="-runStyle Manual:1"
 * If you are using Eclipse and GPE, go to "run configurations" or
 * "debug configurations", select the test you would like to run, and put this
 * line in the VM args under the arguments tab: -Dgwt.args="-runStyle Manual:1"
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
public abstract class StorageMapTest extends MapInterfaceTest<String, String> {
  protected Storage storage;

  public StorageMapTest() {
    super(false, false, true, true, true);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.storage.Storage";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    storage = getStorage();
    if (storage == null) {
      return; // do not run if not supported
    }

    // setup for tests by emptying storage
    storage.clear();
  }

  /**
   * Returns a {@link Storage} object.
   * 
   * Override to return either a LocalStorage or a SessionStorage
   * 
   * @return a {@link Storage} object
   */
  abstract Storage getStorage();

  @Override
  protected String getKeyNotInPopulatedMap()
      throws UnsupportedOperationException {
    return "nonExistingKey";
  }

  @Override
  protected String getValueNotInPopulatedMap()
      throws UnsupportedOperationException {
    return "nonExistingValue";
  }

  @Override
  protected Map<String, String> makeEmptyMap()
      throws UnsupportedOperationException {
    if (storage == null) {
      throw new UnsupportedOperationException("StorageMap not supported because Storage is not supported.");
    }

    storage.clear();
    
    return new StorageMap(storage);
  }

  @Override
  protected Map<String, String> makePopulatedMap()
      throws UnsupportedOperationException {
    if (storage == null) {
      throw new UnsupportedOperationException("StorageMap not supported because Storage is not supported.");
    }

    storage.clear();
    
    storage.setItem("one", "January");
    storage.setItem("two", "February");
    storage.setItem("three", "March");
    storage.setItem("four", "April");
    storage.setItem("five", "May");
    
    return new StorageMap(storage);
  }
}
