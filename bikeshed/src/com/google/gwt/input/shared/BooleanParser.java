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
package com.google.gwt.input.shared;

/**
 * A no-op renderer.
 */
public class BooleanParser implements Parser<Boolean> {

  private static BooleanParser INSTANCE;

  /**
   * @return the instance of the no-op renderer
   */
  public static Parser<Boolean> instance() {
    if (INSTANCE == null) {
      INSTANCE = new BooleanParser();
    }
    return INSTANCE;
  }

  protected BooleanParser() {
  }

  public Boolean parse(String object) {
    return Boolean.valueOf(object);
  }
}
