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
package com.google.gwt.user.client.impl;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Command;

/**
 * IE implementation of {@link com.google.gwt.user.client.impl.WindowImpl}.
 */
public class WindowImplIE extends WindowImpl {

  /**
   * For IE6, reading from $wnd.location.hash drops part of the fragment if the
   * fragment contains a '?'. To avoid this bug, we use location.href instead.
   */
  @Override
  public native String getHash() /*-{
    var href = $wnd.location.href;
    var hashLoc = href.lastIndexOf("#");
    return (hashLoc > 0) ? href.substring(hashLoc) : "";
  }-*/;

  /**
   * For IE6, reading from $wnd.location.search gets confused if hash contains
   * a '?'. To avoid this bug, we use location.href instead.
   */
  @Override
  public native String getQueryString() /*-{
    var href = $wnd.location.href;
    var hashLoc = href.lastIndexOf("#");
    if (hashLoc >= 0) {
      // strip off any hash first
      href = href.substring(0, hashLoc);
    }
    var questionLoc = href.lastIndexOf("?");
    return (questionLoc > 0) ? href.substring(questionLoc) : "";
  }-*/;

  /**
   * IE6 does not allow direct access to event handlers on the parent window,
   * so we must embed a script in the parent window that will set the event
   * handlers in the correct context.
   */
  @Override
  public void initHandler(String initFunc, String funcName, Command cmd) {
    // Embed the init script on the page
    initFunc = initFunc.replaceFirst("function", "function " + funcName);
    ScriptElement scriptElem = Document.get().createScriptElement(initFunc);
    Document.get().getBody().appendChild(scriptElem);

    // Initialize the handler
    cmd.execute();

    // Remove the script element
    Document.get().getBody().removeChild(scriptElem);
  }
}
