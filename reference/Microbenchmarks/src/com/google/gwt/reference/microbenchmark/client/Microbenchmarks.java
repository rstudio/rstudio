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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.reference.microbenchmark.client.MicrobenchmarkSurvey.NanoTest;
import com.google.gwt.reference.microbenchmark.client.MicrobenchmarkSurvey.WidgetMaker;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Offers up a selection of {@link Microbenchmark} implementations to run.
 */
public class Microbenchmarks implements EntryPoint {

  interface Binder extends UiBinder<Widget, Microbenchmarks> {}
  private static final Binder BINDER = GWT.create(Binder.class);

  private final Microbenchmark[] benchmarks;

  double elapsedMs = 0;
  @UiField ListBox listBox;
  @UiField DeckPanel deck;
  @UiField Button button;
  @UiField Element running;
  @UiField Element runs;
  @UiField Element sum;

  public Microbenchmarks() {
    // Add entries for new widget benchmarks here.
    List<NanoTest> widgetMakers = new ArrayList<NanoTest>();
    widgetMakers.add(new WidgetMaker("SimplePanel") {
      @Override
      public Widget make() {
        return new SimplePanel();
      }
    });
    widgetMakers.add(new WidgetMaker("FlowPanel") {
      @Override
      public Widget make() {
        return new FlowPanel();
      }
    });
    widgetMakers.add(new WidgetMaker("HTMLPanel") {
      @Override
      public Widget make() {
        return new HTMLPanel("");
      }
    });
    widgetMakers.add(new EmptyBinder.Maker());
    widgetMakers.add(new TestEmptyDomViaApi.Maker());
    widgetMakers.add(new TestEmptyDom.Maker());
    widgetMakers.add(new TestEmptyCursorDomCrawl.Maker());
    widgetMakers.add(new TestEmptyRealisticDomCrawl.Maker());
    widgetMakers.add(new TestDomViaApi.Maker());
    widgetMakers.add(new TestDomInnerHtmlById.Maker());
    if (Util.hasQSA) {
      widgetMakers.add(new TestDomInnerHtmlQuerySelectorAll.Maker());
    }
    widgetMakers.add(new TestCursorDomCrawl.Maker());
    widgetMakers.add(new TestRealisticDomCrawl.Maker());
    widgetMakers.add(new TestDomBinder.Maker());
    widgetMakers.add(new TestFlows.Maker());
    widgetMakers.add(new TestManualHTMLPanel.Maker());
    widgetMakers.add(new TestWidgetBinder.Maker());

    // Add entries for table creation benchmarks here.
    List<NanoTest> tableMakers = new ArrayList<NanoTest>();
    tableMakers.add(new TestCreateTableInnerHtml.Maker());
    tableMakers.add(new TestCreateTablePrecreatedInnerHtml.Maker());
    tableMakers.add(new TestCreateTableDom.Maker());
    tableMakers.add(new TestCreateTableDomWithEvents.Maker());

    // Add entries for table update benchmarks here.
    List<NanoTest> tableUpdaters = new ArrayList<NanoTest>();
    tableUpdaters.add(new TestCreateTableInnerHtml.Updater());
    tableUpdaters.add(new TestCreateTablePrecreatedInnerHtml.Updater());
    tableUpdaters.add(new TestCreateTableDom.Updater());
    tableUpdaters.add(new TestCreateTableDomWithEvents.Updater());

    // Combine all table tests.
    List<NanoTest> allTableTests = new ArrayList<MicrobenchmarkSurvey.NanoTest>();
    allTableTests.addAll(tableMakers);
    allTableTests.addAll(tableUpdaters);

    benchmarks = new Microbenchmark[4];
    benchmarks[0] = new MicrobenchmarkSurvey("Widget Creation Survey", widgetMakers);
    benchmarks[1] = new MicrobenchmarkSurvey("Table Creation and Update Survey", allTableTests);
    benchmarks[2] = new MicrobenchmarkSurvey("Table Creation Survey", tableMakers);
    benchmarks[3] = new MicrobenchmarkSurvey("Table Update Survey", tableUpdaters);
  }

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
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        double start = Duration.currentTimeMillis();
        benchmarks[index].run();
        double end = Duration.currentTimeMillis();
        UIObject.setVisible(running, false);
        button.setEnabled(true);
        double run = end - start;
        runs.setInnerText(runs.getInnerText() + Util.format(run) + " ");
        elapsedMs += run;
        sum.setInnerText("(" + Util.format(elapsedMs) + ")");
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
