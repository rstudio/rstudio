/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dom.client;

/**
 * WebKit based implementation of {@link com.google.gwt.user.client.impl.DOMImplStandardBase}.
 */
class DOMImplWebkit extends DOMImplStandardBase {

  /**
   * Return true if using Webkit 525.x (Safari 3) or earlier.
   * 
   * @return true if using Webkit 525.x (Safari 3) or earlier.
   */
  @SuppressWarnings("unused")
  private static native boolean isWebkit525OrBefore() /*-{
    var result = /safari\/([\d.]+)/.exec(navigator.userAgent.toLowerCase());
    if (result) {
      var version = (parseFloat(result[1]));
      if (version < 526) {
        return true;
      }
    }
    return false;
  }-*/;
}

