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
package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestWidgetBinder extends Composite {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Text heavy UI with HTMLPanel via UiBinder");
    }

    @Override
    public Widget make() {
      return new TestWidgetBinder();
    }
  }
  interface Binder extends UiBinder<Widget, TestWidgetBinder> {}
  @UiField DivElement div1;
  @UiField DivElement div2;
  @UiField DivElement div3;
  @UiField DivElement div4;

  @UiField SpanElement span1;
  @UiField SpanElement span2;

  private static final Binder BINDER = GWT.create(Binder.class);
  
  private TestWidgetBinder() {
    initWidget(BINDER.createAndBindUi(this));
  }
}
