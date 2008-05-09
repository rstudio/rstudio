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
public class SuperSourceTagTest extends TestSuperAndSourceTags {

  public SuperSourceTagTest() throws UnableToCompleteException {
    super();
  }
  
  public void testSuperSourceTag() {
    validateTags();
  }
  
  /**
   * Return the logical path for a given class.  For super source, the logical
   * path does not include the path component from the tag.
   */
  protected String getLogicalPath(Class<?> clazz) {
    String name = clazz.getCanonicalName();
    name = name.substring(getClass().getPackage().getName().length() + 1);
    name = name.replaceFirst("test\\.\\w+\\.", "");
    return name;
  }
}
