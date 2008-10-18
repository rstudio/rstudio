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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * 
 */
public class SourceTagTest extends TestSuperAndSourceTags {

  public SourceTagTest() throws UnableToCompleteException {
    super();
  }

  public void testSourceTag() {
    validateTags();
  }

  /**
   * Return the logical path for a given class. For example, java.lang.Object's
   * logical path would be java/lang/Object.
   */
  protected String getLogicalPath(Class<?> clazz) {
    return clazz.getCanonicalName();
  }
}
