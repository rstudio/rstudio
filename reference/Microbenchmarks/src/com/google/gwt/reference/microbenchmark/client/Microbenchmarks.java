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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * Offers up a selection of {@link Microbenchmark} implementations to run.
 */
public class Microbenchmarks implements EntryPoint {
  
  interface Binder extends UiBinder<Widget, Microbenchmarks> {}
  private static final Binder BINDER = GWT.create(Binder.class);
  
  // Add entries for new benchmarks here.
  private final Microbenchmark[] benchmarks = {
      new WidgetCreation()
  };

  @UiField ListBox listBox;
  @UiField DeckPanel deck;
  @UiField Button button;
  @UiField Element running;

  @UiHandler("listBox")
  public void onChange(@SuppressWarnings("unused") ChangeEvent ignored) {
    int index = listBox.getSelectedIndex();
    deck.showWidget(index);         
  }
  
  @UiHandler("button")
  public void onClick(@SuppressWarnings("unused") ClickEvent ignored) {
    final int index = listBox.getSelectedIndex();
    UIObject.setVisible(running, true);
    button.setEnabled(false);
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        benchmarks[index].run();
        UIObject.setVisible(running, false);
        button.setEnabled(true);
      }
    });
  }
  
  public void onModuleLoad() {
    Widget root = BINDER.createAndBindUi(this);

    for (Microbenchmark benchmark : benchmarks) {
      listBox.addItem(benchmark.getName());
      deck.add(benchmark.getWidget());
    }

    deck.showWidget(0);
    RootPanel.get().add(root);
  }
}
