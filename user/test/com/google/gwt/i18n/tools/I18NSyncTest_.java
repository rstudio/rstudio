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
package com.google.gwt.i18n.tools;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Testing <code>I18NSync</code>.Cannot be currently run as part of automated
 * junit tests due to it's code generation.
 */
public class I18NSyncTest_ extends TestCase {
  static final File CLIENT_SOURCE_DIR = new File("../../user/test");
  static final String CLIENT_SOURCE_PACKAGE = "com.google.gwt.i18n.client.gen.";

  /**
   * Test Constant Creation.
   * 
   * @throws IOException
   */
  public void testConstantCreation() throws IOException {

    String javaName = CLIENT_SOURCE_PACKAGE + "Colors";
    // Examine the interface in eclipse for formatting.
    I18NSync.createConstantsInterfaceFromClassName(javaName, CLIENT_SOURCE_DIR);

    String target = CLIENT_SOURCE_PACKAGE + "SingleConstant";
    I18NSync.createConstantsInterfaceFromClassName(target, null);
    I18NSync.createConstantsInterfaceFromClassName(target, CLIENT_SOURCE_DIR);
    String empty = CLIENT_SOURCE_PACKAGE + "EmptyConstants";
    try {
      I18NSync.createConstantsInterfaceFromClassName(empty, CLIENT_SOURCE_DIR);
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      // Should be caught
    }
  }
  
  public void testConstantsQuoting() throws IOException  {
    String className = CLIENT_SOURCE_PACKAGE + "TestConstantsQuoting";
    I18NSync.createConstantsInterfaceFromClassName(className, CLIENT_SOURCE_DIR);
  }
  
  public void testFileIsDirCase() {
    try {
      I18NSync.createMessagesInterfaceFromClassName(CLIENT_SOURCE_PACKAGE, null);
      fail("Should have thrown IOException");
    } catch (IOException e) {
      assertEquals(-1, e.getMessage().indexOf("directory"));
    }
  }

  public void testMainAccess() {
    // Cannot check bad ones due to System.exit(1);
    String[] constants = {
        CLIENT_SOURCE_PACKAGE + "Shapes", "-out", CLIENT_SOURCE_DIR.getPath()};
    I18NSync.main(constants);

    String[] messages = {
        "-createMessages", CLIENT_SOURCE_PACKAGE + "SingleMessages", "-out",
        CLIENT_SOURCE_DIR.getPath()};
    I18NSync.main(messages);
  }

  public void testMessageCreation() throws IOException {
    String className = CLIENT_SOURCE_PACKAGE + "TestMessages";
    I18NSync.createMessagesInterfaceFromClassName(className, CLIENT_SOURCE_DIR);
  }

  public void testMessagesQuoting() throws IOException  {
    String className = CLIENT_SOURCE_PACKAGE + "TestMessagesQuoting";
    I18NSync.createMessagesInterfaceFromClassName(className, CLIENT_SOURCE_DIR);
  }

  public void testMethodRenaming() throws IOException {
    String className = CLIENT_SOURCE_PACKAGE + "TestBadKeys";
    I18NSync.createConstantsWithLookupInterfaceFromClassName(className,
        CLIENT_SOURCE_DIR);
  }
}
