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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * Creates an empty HTMLPanel via UiBinder.
 */
public class EmptyBinder extends Composite {

  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("HTMLPanel UiBinder");
    }

    @Override
    public Widget make() {
      return new EmptyBinder();
    }
  }
  interface Binder extends UiBinder<Widget, EmptyBinder> {}

  private static final Binder BINDER = GWT.create(Binder.class);
  
  private EmptyBinder() {
    initWidget(BINDER.createAndBindUi(this));
  }
}
