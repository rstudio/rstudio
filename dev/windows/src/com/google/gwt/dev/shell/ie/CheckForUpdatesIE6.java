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
package com.google.gwt.dev.shell.ie;

import com.google.gwt.dev.shell.CheckForUpdates;

/**
 * IE6 implementation of the update checker.
 */
public class CheckForUpdatesIE6 extends CheckForUpdates {

  public CheckForUpdatesIE6() {
    LowLevelIE6.init();
  }

  @Override
  protected byte[] doHttpGet(String userAgent, String url) {
    byte[] response = LowLevelIE6.httpGet(userAgent, url);
    return response;
  }

}
