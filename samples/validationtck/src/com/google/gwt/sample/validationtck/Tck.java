/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.validationtck;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import org.hibernate.jsr303.tck.tests.constraints.application.Woman;

import javax.validation.Validator;

/**
 * Just a place holder Entry point.
 */
public class Tck implements EntryPoint {

  public void onModuleLoad() {
    Validator validator = GWT.create(Validator.class);
    Woman w = new Woman();
    validator.validate(w);
    Label label = new Label("tck");
    RootPanel.get("view").add(label);
  }
}
