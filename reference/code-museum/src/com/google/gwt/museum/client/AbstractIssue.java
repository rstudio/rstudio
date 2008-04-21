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
package com.google.gwt.museum.client;

import com.google.gwt.user.client.ui.Widget;

/**
 * An abstract issue that can be used in the code museum.
 */
public abstract class AbstractIssue {
  /**
   * <p>
   * Create a widget that illustrates the issue. Each issue should include a
   * detailed description of the expected results and the observed results
   * before the issue was fixed.
   * </p>
   * <p>
   * Note that createIssue will may be called multiple times if the user
   * refreshes the issue. If you save state within the instance, you must clear
   * it out and reset the issue when createIssue is called again.
   * </p>
   * 
   * @return a widget that can reproduce the issue
   */
  public abstract Widget createIssue();

  /**
   * Return a detailed description of what the user should expect to see. The
   * description will be added above the example.
   * 
   * @return the name of this issue
   */
  public abstract String getDescription();

  /**
   * Return a short description to display for this issue. If you do not
   * override this method, the class name will be displayed.
   * 
   * @return the name of this issue
   */
  public String getHeadline() {
    String className = getClass().getName();
    className = className.substring(className.lastIndexOf(".") + 1);
    return className;
  }

  /**
   * @return true to load a CSS file of the same name
   */
  public abstract boolean hasCSS();
}
