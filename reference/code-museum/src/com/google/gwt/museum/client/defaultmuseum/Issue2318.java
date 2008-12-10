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

package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.NamedFrame;
import com.google.gwt.user.client.ui.Widget;

/**
 * Warning message in IE6 when using NamedFrame on SSL-secured web-site.
 */
public class Issue2318 extends AbstractIssue {
  @Override
  public Widget createIssue() {
    return new NamedFrame("myFrame");
  }

  @Override
  public String getInstructions() {
    return "Open this page in IE6 on an SSL-secured server (https).  Verify "
        + "that you do not see a mixed-content warning.";
  }

  @Override
  public String getSummary() {
    return "Warning message in IE6 when using NamedFrame on SSL-secured site";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
