/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.i18n.rebind.util;

import java.io.File;
import java.io.IOException;

/**
 * Constants Interface creator.
 */
public class ConstantsInterfaceCreator extends
    AbstractLocalizableInterfaceCreator {

  /**
   * Creates a new constants creator.
   * 
   * @param className constant class to create
   * @param packageName package to create it in
   * @param resourceBundle resource bundle with value
   * @param targetLocation
   * @throws IOException
   */
  public ConstantsInterfaceCreator(String className, String packageName,
      File resourceBundle, File targetLocation, Class interfaceClass)
      throws IOException {
    super(className, packageName, resourceBundle, targetLocation,
      interfaceClass);
  }

  protected void genMethodArgs(String defaultValue) {
    // no arguments
  }

  protected String javaDocComment(String path) {
    return "Interface to represent the constants contained in resource  bundle:\n\t'"
      + path + "'.";
  }
}
