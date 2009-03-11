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
package com.google.gwt.user;

import com.google.gwt.junit.GWTMockUtilities;

import junit.framework.TestCase;

import java.io.File;

/**
 * Tests that every class in com.google.gwt.user.client.ui and
 * com.google.gwt.user.datepicker.client can be init'd by the real Java
 * runtime. By ensuring this, we ensure that these classes all may be referenced
 * mocked out by pure Java unit tests, e.g. with EasyMock Class Extension
 */
public class ClassInitTest extends TestCase {
  private static final String DOT_CLASS = ".class";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    GWTMockUtilities.disarm();
  }

  @Override
  public void tearDown() {
    GWTMockUtilities.restore();
  }

  public void testUi() throws ClassNotFoundException {
    doPackage("com.google.gwt.user.client.ui");
  }
  
  public void testDatePicker() throws ClassNotFoundException {
    doPackage("com.google.gwt.user.datepicker.client");
  }
  
  private void doPackage(String packageName) throws ClassNotFoundException {
    String path = packageNameToPath(packageName);
    File directory = pathToResourceDirectory(path);

    if (directory.exists()) {
      String[] files = directory.list();
      for (String file : files) {
        if (file.endsWith(DOT_CLASS)) {
          String classname = classFileToClassName(file);
          Class.forName(packageName + "." + classname);
        }
      }
    }
  }

  private String classFileToClassName(String file) {
    return file.substring(0, file.length() - DOT_CLASS.length());
  }

  private File pathToResourceDirectory(String name) {
    File directory = new File(getClass().getResource(name).getFile());
    return directory;
  }

  private String packageNameToPath(String name) {
    name = "/" + name.replace('.', '/');
    return name;
  }
}
