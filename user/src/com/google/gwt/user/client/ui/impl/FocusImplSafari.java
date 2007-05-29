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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.Element;

/**
 * Safari-specific implementation of {@link FocusImpl} that creates a completely
 * transparent hidden element, since Safari will not keyboard focus on an input
 * element that has zero width and height.
 */
public class FocusImplSafari extends FocusImplOld {
  protected native Element createHiddenInput() /*-{
    var input = $doc.createElement('input');
    input.type = 'text';
    input.style.opacity = 0;
    input.style.zIndex = -1;
    input.style.position = 'absolute';
    return input;
  }-*/;
}
