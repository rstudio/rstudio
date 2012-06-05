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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

/**
 * IE implementation of {@link com.google.gwt.user.client.impl.WindowImpl}.
 */
public class WindowImplIE extends WindowImpl {

  /**
   * The resources for this implementation.
   */
  public interface Resources extends ClientBundle {
    Resources INSTANCE = GWT.create(Resources.class);

    /**
     * Contains the function body used to initialize the window close handler.
     */
    @Source("initWindowCloseHandler.js")
    TextResource initWindowCloseHandler();

    /**
     * Contains the function body used to initialize the window resize handler.
     */
    @Source("initWindowResizeHandler.js")
    TextResource initWindowResizeHandler();

    /**
     * Contains the function body used to initialize the window scroll handler.
     */
    @Source("initWindowScrollHandler.js")
    TextResource initWindowScrollHandler();
  }

  /**
   * For IE6, reading from $wnd.location.hash drops part of the fragment if the
   * fragment contains a '?'. To avoid this bug, we use location.href instead.
   */
  @Override
  public native String getHash() /*-{
    var href = $wnd.location.href;
    var hashLoc = href.indexOf("#");
    return (hashLoc > 0) ? href.substring(hashLoc) : "";
  }-*/;

  /**
   * For IE6, reading from $wnd.location.search gets confused if hash contains
   * a '?'. To avoid this bug, we use location.href instead.
   */
  @Override
  public native String getQueryString() /*-{
    var href = $wnd.location.href;
    var hashLoc = href.indexOf("#");
    if (hashLoc >= 0) {
      // strip off any hash first
      href = href.substring(0, hashLoc);
    }
    var questionLoc = href.indexOf("?");
    return (questionLoc > 0) ? href.substring(questionLoc) : "";
  }-*/;

  @Override
  public void initWindowCloseHandler() {
    initHandler(Resources.INSTANCE.initWindowCloseHandler().getText(),
        new ScheduledCommand() {
          public void execute() {
            initWindowCloseHandlerImpl();
          }
        });
  }

  @Override
  public void initWindowResizeHandler() {
    initHandler(Resources.INSTANCE.initWindowResizeHandler().getText(),
        new ScheduledCommand() {
          public void execute() {
            initWindowResizeHandlerImpl();
          }
        });
  }

  @Override
  public void initWindowScrollHandler() {
    initHandler(Resources.INSTANCE.initWindowScrollHandler().getText(),
        new ScheduledCommand() {
          public void execute() {
            initWindowScrollHandlerImpl();
          }
        });
  }

  /**
   * IE6 does not allow direct access to event handlers on the parent window,
   * so we must embed a script in the parent window that will set the event
   * handlers in the correct context.
   * 
   * @param initFunc the string representation of the init function
   * @param cmd the command to execute the init function
   */
  private void initHandler(String initFunc, ScheduledCommand cmd) {
    if (GWT.isClient()) {
      // Embed the init script on the page
      ScriptElement scriptElem = Document.get().createScriptElement(initFunc);
      Document.get().getBody().appendChild(scriptElem);
  
      // Initialize the handler
      cmd.execute();
  
      // Remove the script element
      Document.get().getBody().removeChild(scriptElem);
    }
  }

  private native void initWindowCloseHandlerImpl() /*-{
    $wnd.__gwt_initWindowCloseHandler(
      $entry(@com.google.gwt.user.client.Window::onClosing()),
      $entry(@com.google.gwt.user.client.Window::onClosed())
    );
  }-*/;

  private native void initWindowResizeHandlerImpl() /*-{
    $wnd.__gwt_initWindowResizeHandler(
      $entry(@com.google.gwt.user.client.Window::onResize())
    );
  }-*/;

  private native void initWindowScrollHandlerImpl() /*-{
    $wnd.__gwt_initWindowScrollHandler(
      $entry(@com.google.gwt.user.client.Window::onScroll())
    );
  }-*/;

}
