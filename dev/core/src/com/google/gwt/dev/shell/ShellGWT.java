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
package com.google.gwt.dev.shell;

/**
 * This class is the hosted-mode peer for {@link com.google.gwt.core.client.GWT}.
 */
public class ShellGWT {

  public static Object create(Class classLiteral) {
    return JavaScriptHost.rebindAndCreate(classLiteral);
  }

  public static String getTypeName(Object o) {
    return o != null ? o.getClass().getName() : null;
  }
  
  public static void log(String message, Throwable e) {
    JavaScriptHost.log(message, e);
  }
}
