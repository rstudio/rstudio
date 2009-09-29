/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import java.util.HashSet;
import java.util.Set;

/**
 * A widget that throws an exception onLoad or onUnload.
 */
class BadWidget extends Widget {
  /**
   * Wrap an existing div.
   * 
   * @param element the div to wrap
   * @return a {@link BadWidget}
   */
  public static BadWidget wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    BadWidget widget = new BadWidget(element);

    // Mark it attached and remember it for cleanup.
    widget.onAttach();
    RootPanel.detachOnWindowClose(widget);

    return widget;
  }

  private boolean failOnLoad;
  private boolean failOnUnload;
  private boolean failAttachChildren;
  private boolean failDetachChildren;

  public BadWidget() {
    this(Document.get().createDivElement());
  }

  protected BadWidget(Element element) {
    setElement(element);
    assert element.getTagName().equalsIgnoreCase("div");
  }

  @Override
  public void onLoad() {
    if (failOnLoad) {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public void onUnload() {
    if (failOnUnload) {
      throw new IllegalArgumentException();
    }
  }

  public void setFailAttachChildren(boolean fail) {
    this.failAttachChildren = fail;
  }

  public void setFailDetachChildren(boolean fail) {
    this.failDetachChildren = fail;
  }

  public void setFailOnLoad(boolean fail) {
    this.failOnLoad = fail;
  }

  public void setFailOnUnload(boolean fail) {
    this.failOnUnload = fail;
  }

  @Override
  protected void doAttachChildren() {
    if (failAttachChildren) {
      Set<Throwable> cause = new HashSet<Throwable>();
      cause.add(new IllegalArgumentException());
      throw new AttachDetachException(cause);
    }
  }

  @Override
  protected void doDetachChildren() {
    if (failDetachChildren) {
      Set<Throwable> cause = new HashSet<Throwable>();
      cause.add(new IllegalArgumentException());
      throw new AttachDetachException(cause);
    }
  }
}
