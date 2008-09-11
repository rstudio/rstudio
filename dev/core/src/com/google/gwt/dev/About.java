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
package com.google.gwt.dev;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * About information for GWT.
 */
public class About {

  public static String GWT_SVNREV;

  public static String GWT_VERSION_NUM;

  public static String GWT_NAME = "Google Web Toolkit";

  public static String GWT_VERSION;

  static {
    Properties props = new Properties();
    try {
      InputStream instream = About.class.getResourceAsStream("About.properties");
      props.load(instream);
    } catch (IOException iox) {
      // okay... we use default values, then.
    }

    GWT_SVNREV = props.getProperty("gwt.svnrev");
    if (GWT_SVNREV == null) {
      GWT_SVNREV = "unknown";
    }
    GWT_VERSION_NUM = props.getProperty("gwt.version");
    if (GWT_VERSION_NUM == null) {
      GWT_VERSION_NUM = "0.0.0";
    }
    GWT_VERSION = GWT_NAME + " " + GWT_VERSION_NUM;
  }

  private About() {
  }
}
