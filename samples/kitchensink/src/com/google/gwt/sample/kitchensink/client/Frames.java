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
package com.google.gwt.sample.kitchensink.client;

import com.google.gwt.user.client.ui.Frame;

/**
 * Demonstrates the {@link com.google.gwt.user.client.ui.Frame} widget.
 */
public class Frames extends Sink {

  public static SinkInfo init() {
    return new SinkInfo("Frames",
      "If you need to include multiple pages of good ol' static HTML, it's "
        + "easy to do using the <code>Frame</code> class.") {
      public Sink createInstance() {
        return new Frames();
      }
    };
  }

  private Frame frame = new Frame("rembrandt/LaMarcheNocturne.html");

  public Frames() {
    frame.setWidth("100%");
    frame.setHeight("48em");
    initWidget(frame);
  }

  public void onShow() {
  }
}
