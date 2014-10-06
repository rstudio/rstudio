/*
 * Copyright 2014 Google Inc.
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

package com.google.gwt.resources.client.gss;

import com.google.gwt.resources.client.gss.TestResources.ClassNameAnnotation;

/**
 * Tests style classes renaming with the configuration property <code>CssResource.style</code> set
 * to stable.
 */
public class StableObfuscationStyleTest extends RenamingClassNameTest {
  @Override
  public String getModuleName() {
    return "com.google.gwt.resources.GssStableStyle";
  }

  public void testClassesRenaming() {
    ClassNameAnnotation classNameAnnotation = res().classNameAnnotation();
    String renamedClass = classNameAnnotation.renamedClass();
    String nonRenamedClass = classNameAnnotation.nonRenamedClass();

    assertEquals("com-google-gwt-resources-client-gss-TestResources-ClassNameAnnotation-renamed" +
            "-class", renamedClass);
    assertEquals("com-google-gwt-resources-client-gss-TestResources-ClassNameAnnotation" +
        "-nonRenamedClass", nonRenamedClass);
  }
}
