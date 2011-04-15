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
package com.google.gwt.benchmarks.viewer.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The application for viewing benchmark reports. In order for the ReportViewer
 * to operate correctly, you must have both the {@link ReportServer} RPC and
 * {@link com.google.gwt.junit.viewer.server.ReportImageServer} servlets up and
 * running within a servlet container.
 * 
 * <code>ReportViewer's</code> GWT XML module is configured to start these
 * servlets by default. Just start <code>ReportViewer</code> in hosted mode, and
 * GWT will run them within its own embedded servlet engine. For example,
 * 
 * <pre>java -cp &lt;classpath&gt; com.google.gwt.dev.GWTShell -out
 * ReportViewerShell/www
 * com.google.gwt.junit.viewer.ReportViewer/ReportViewer.html</pre>
 * 
 * You can configure the location where ReportServer reads the benchmark reports
 * from by setting the system property named in
 * {@link com.google.gwt.benchmarks.client.Benchmark#REPORT_PATH}.
 */
@SuppressWarnings("deprecation")
public class ReportViewer implements EntryPoint, HistoryListener {

  private static class MutableBool {

    boolean value;

    MutableBool(boolean value) {
      this.value = value;
    }
  }

  private class SummariesTableListener implements TableListener {

    public void onCellClicked(SourcesTableEvents sender, int row, int col) {
      if (row > 0) {
        ReportSummary summary = summaries.get(row - 1);
        String token = summary.getId();
        // Short circuit the history loop.
        selectReport(row, token);
        History.newItem(token);
      }
    }
  }

  private static final String imageServer = GWT.getModuleBaseURL() + "test_images/";

  HTML detailsLabel;

  Report report;

  VerticalPanel reportPanel;

  ReportServerAsync reportServer;

  FlexTable reportTable;

  HTML statusLabel;

  List<ReportSummary> summaries;

  VerticalPanel summariesPanel;

  FlexTable summariesTable;

  CellPanel topPanel;

  private int currentSelectedRow;

  private String currentToken;

  public void onHistoryChanged(String token) {
    assert (summaries != null);
    int row = 1;
    for (ReportSummary summary : summaries) {
      if (summary.getId().equals(token)) {
        selectReport(row, token);
        return;
      }
      ++row;
    }
    selectReport(0, token);
  }

  public void onModuleLoad() {
    init();

    // Asynchronously load the summaries
    reportServer = (ReportServerAsync) GWT.create(ReportServer.class);

    reportServer.getReportSummaries(new AsyncCallback<List<ReportSummary>>() {
      public void onFailure(Throwable caught) {
        String msg =
            "<p>" + caught.toString() + "</p>" + "<p>Is your path to the reports correct?</p>";
        statusLabel.setHTML(msg);
      }

      public void onSuccess(List<ReportSummary> result) {
        summaries = result;
        if (summaries != null) {
          if (summaries.size() == 0) {
            statusLabel.setText("There are no benchmark reports available in this folder.");
          }
          Collections.sort(summaries, new Comparator<ReportSummary>() {
            public int compare(ReportSummary r1, ReportSummary r2) {
              return r2.getDate().compareTo(r1.getDate()); // most recent first
            }
          });
          displaySummaries();

          // If there's an initial URL, browse to it.
          onHistoryChanged(History.getToken());
          History.addHistoryListener(ReportViewer.this);
        }
      }
    });
  }

  protected void selectReport(int row, String token) {
    if (row == currentSelectedRow && token.equals(currentToken)) {
      return;
    }
    if (currentSelectedRow != -1) {
      summariesTable.getRowFormatter().removeStyleName(currentSelectedRow, "viewer-SelectedRow");
    }
    currentToken = token;
    currentSelectedRow = row;

    if (row < 1) {
      clearReport();
    } else {
      fetchReport(row, token);
    }
  }

  private void clearReport() {
    statusLabel.setText("Select a report.");
    detailsLabel.setHTML("<h3>Report Details</h3>");
    reportPanel.remove(reportTable);
  }

  private FlexTable createReportTable() {
    FlexTable topTable = new FlexTable();

    FlexTable tempReportTable = new FlexTable();
    tempReportTable.setBorderWidth(1);
    tempReportTable.setCellPadding(5);
    tempReportTable.setWidget(0, 0, new Label("Date Created"));
    tempReportTable.setWidget(0, 1, new Label("GWT Version"));

    if (report == null) {
      tempReportTable.setWidget(1, 0, new Label("No currently selected report."));
      tempReportTable.getFlexCellFormatter().setColSpan(1, 0, 3);
      return tempReportTable;
    }

    detailsLabel.setHTML("<h3>" + report.getId() + " details </h3>");
    tempReportTable.setWidget(1, 0, new Label(report.getDateString()));
    tempReportTable.setWidget(1, 1, new Label(report.getGwtVersion()));

    // topTable.setWidget( 0, 0, tempReportTable );
    int currentRow = 1;

    Collections.sort(report.getCategories(), new Comparator<Category>() {
      public int compare(Category c1, Category c2) {
        return c1.getName().compareTo(c2.getName());
      }
    }); // Should be done once in the RPC

    for (int i = 0; i < report.getCategories().size(); ++i) {
      Category c = report.getCategories().get(i);

      if (!c.getName().equals("")) {
        FlexTable categoryTable = new FlexTable();
        categoryTable.setBorderWidth(0);
        categoryTable.setCellPadding(5);
        categoryTable.setText(0, 0, c.getName());
        categoryTable.getFlexCellFormatter().setStyleName(0, 0, "benchmark-category");

        categoryTable.setWidget(0, 1, new Label("Description"));
        categoryTable.setWidget(1, 0, new Label(c.getName()));
        categoryTable.setWidget(1, 1, new Label(c.getDescription()));

        topTable.setWidget(currentRow++, 0, categoryTable);
      }

      Collections.sort(c.getBenchmarks(), new Comparator<Benchmark>() {
        public int compare(Benchmark b1, Benchmark b2) {
          return b1.getName().compareTo(b2.getName());
        }
      }); // Should be done once in the RPC

      for (int j = 0; j < c.getBenchmarks().size(); ++j) {
        Benchmark benchmark = c.getBenchmarks().get(j);

        FlexTable benchmarkTable = new FlexTable();
        benchmarkTable.setBorderWidth(0);
        benchmarkTable.setCellPadding(5);
        benchmarkTable.setText(0, 0, benchmark.getName());
        // benchmarkTable.setText(0, 1, benchmark.getDescription());
        String codeHtml;
        String sourceText = benchmark.getSourceCode();
        if (sourceText != null) {
          Element tempElem = DOM.createDiv();
          DOM.setInnerText(tempElem, sourceText);
          String escapedCodeHtml = DOM.getInnerHTML(tempElem);
          codeHtml = "<pre>" + escapedCodeHtml + "</pre>";
        } else {
          codeHtml = "<i>(source not available)</i>";
        }
        benchmarkTable.setWidget(1, 0, new HTML(codeHtml));
        benchmarkTable.getFlexCellFormatter().setStyleName(0, 0, "benchmark-name");
        // benchmarkTable.getFlexCellFormatter().setStyleName( 0, 1,
        // "benchmark-description" );
        benchmarkTable.getFlexCellFormatter().setStyleName(1, 0, "benchmark-code");

        // TODO(tobyr) Provide detailed benchmark information.
        // Following bits of commented code are steps in that direction.
        /*
         * benchmarkTable.setWidget( 0, 1, new Label( "Description"));
         * benchmarkTable.setWidget( 0, 2, new Label( "Class Name"));
         * benchmarkTable.setWidget( 0, 3, new Label( "Source Code"));
         * benchmarkTable.setWidget( 1, 0, new Label( benchmark.getName()));
         * benchmarkTable.setWidget( 1, 1, new Label(
         * benchmark.getDescription())); benchmarkTable.setWidget( 1, 2, new
         * Label( benchmark.getClassName())); benchmarkTable.setWidget( 1, 3,
         * new HTML( "<pre>" + benchmark.getSourceCode() + "</pre>"));
         */
        topTable.setWidget(currentRow++, 0, benchmarkTable);

        FlexTable resultsTable = new FlexTable();
        resultsTable.setBorderWidth(0);
        resultsTable.setCellPadding(5);
        FlexTable.FlexCellFormatter resultsFormatter = resultsTable.getFlexCellFormatter();
        topTable.setWidget(currentRow++, 0, resultsTable);

        Collections.sort(benchmark.getResults(), new Comparator<Result>() {
          public int compare(Result r1, Result r2) {
            return r1.getAgent().compareTo(r2.getAgent());
          }
        }); // Should be done once in the RPC

        final List<FlexTable> trialsTables = new ArrayList<FlexTable>();

        int numVariables = 0;
        List<String> variableNames = null;
        if (benchmark.getResults().size() > 0) {
          Result sampleResult = benchmark.getResults().get(0);
          if (sampleResult.getTrials().size() > 0) {
            Trial sampleTrial = sampleResult.getTrials().get(0);
            numVariables = sampleTrial.getVariables().size();
            Map<String, String> variables = sampleTrial.getVariables();
            variableNames = new ArrayList<String>(variables.keySet());
            Collections.sort(variableNames);
          }
        }
        final MutableBool isVisible = new MutableBool(numVariables > 2);
        String buttonName = isVisible.value ? "Hide Data" : "Show Data";

        Button visibilityButton = new Button(buttonName, new ClickListener() {
          public void onClick(Widget sender) {
            isVisible.value = !isVisible.value;
            for (int i = 0; i < trialsTables.size(); ++i) {
              Widget w = trialsTables.get(i);
              w.setVisible(isVisible.value);
            }
            String name = isVisible.value ? "Hide Data" : "Show Data";
            ((Button) sender).setText(name);
          }
        });

        for (int k = 0; k < benchmark.getResults().size(); ++k) {
          Result result = benchmark.getResults().get(k);

          // Currently only support graphs for results of 2 variables or less
          if (numVariables <= 2) {
            resultsTable.setWidget(0, k, new Image(getImageUrl(report.getId(), c.getName(),
                benchmark.getClassName(), benchmark.getName(), result.getAgent())));
          } else {
            if (k == 0) {
              resultsTable.setHTML(0, k, "<b>" + BrowserInfo.getBrowser(result.getAgent())
                  + "</b><br><font size=\"-1\">(Graphs are not yet available "
                  + "for benchmarks with more than two parameters)</font>");
            }
          }

          /*
           * FlexTable allTrialsTable = new FlexTable();
           * allTrialsTable.setBorderWidth(1); allTrialsTable.setCellPadding(5);
           * FlexTable.CellFormatter allTrialsFormatter = allTrialsTable
           * .getFlexCellFormatter(); topTable.setWidget(currentRow++, 0,
           * allTrialsTable); allTrialsTable.setWidget(0, k, trialsTable);
           * allTrialsFormatter .setAlignment(0, k,
           * HasHorizontalAlignment.ALIGN_CENTER,
           * HasVerticalAlignment.ALIGN_TOP);
           */

          resultsFormatter.setAlignment(2, k, HasHorizontalAlignment.ALIGN_LEFT,
              HasVerticalAlignment.ALIGN_TOP);

          // A table of straight data for all trials for an agent
          FlexTable trialsTable = new FlexTable();
          trialsTable.setVisible(isVisible.value);
          trialsTables.add(trialsTable);
          trialsTable.setBorderWidth(1);
          trialsTable.setCellPadding(5);

          if (k == 0) {
            resultsTable.setWidget(1, k, visibilityButton);
            resultsFormatter.setColSpan(1, k, benchmark.getResults().size());
            resultsFormatter.setAlignment(1, k, HasHorizontalAlignment.ALIGN_LEFT,
                HasVerticalAlignment.ALIGN_MIDDLE);
          }

          resultsTable.setWidget(2, k, trialsTable);

          // Write out the variable column headers
          for (int varIndex = 0; varIndex < numVariables; ++varIndex) {
            String varName = variableNames.get(varIndex);
            trialsTable.setHTML(0, varIndex, varName);
          }

          // Timing header
          trialsTable.setHTML(0, numVariables, "Timing (ms)");

          // Write out all the trial data
          int l = 0;
          for (Trial trial : result.getTrials()) {
            // Write the variable values
            for (int varIndex = 0; varIndex < numVariables; ++varIndex) {
              String varName = variableNames.get(varIndex);
              String varValue = trial.getVariables().get(varName);
              trialsTable.setHTML(l + 1, varIndex, varValue);
            }

            // Write out the timing data
            String data = trial.getRunTimeMillis() + "";
            trialsTable.setHTML(l + 1, numVariables, data);
            ++l;
          }

          if (result.getException() != null) {
            trialsTable.setHTML(l + 1, numVariables, result.getException());
          }
        }
      }
    }

    return topTable;
  }

  private FlexTable createSummariesTable() {
    FlexTable tempSummariesTable = new FlexTable();
    tempSummariesTable.addStyleName("viewer-List");
    tempSummariesTable.setCellPadding(5);
    tempSummariesTable.setBorderWidth(1);
    tempSummariesTable.setCellSpacing(0);
    tempSummariesTable.setWidget(0, 0, new Label("Report"));
    tempSummariesTable.setWidget(0, 1, new Label("Date Created"));
    tempSummariesTable.setWidget(0, 2, new Label("Tests"));
    tempSummariesTable.getRowFormatter().addStyleName(0, "viewer-ListHeader");

    if (summaries == null) {
      tempSummariesTable.setWidget(1, 0, new Label("Fetching reports..."));
      tempSummariesTable.getFlexCellFormatter().setColSpan(1, 0, 4);
      return tempSummariesTable;
    }

    for (int i = 0; i < summaries.size(); ++i) {
      ReportSummary summary = summaries.get(i);
      int index = i + 1;
      tempSummariesTable.setWidget(index, 0, new Hyperlink(summary.getId(), summary.getId()));
      tempSummariesTable.setWidget(index, 1, new Label(summary.getDateString()));
      tempSummariesTable.setWidget(index, 2, new Label(String.valueOf(summary.getNumTests())));
    }

    tempSummariesTable.addTableListener(new SummariesTableListener());
    return tempSummariesTable;
  }

  private void displayReport() {
    FlexTable table = createReportTable();
    reportPanel.remove(reportTable);
    reportTable = table;
    reportPanel.insert(reportTable, 1);
  }

  private void displaySummaries() {
    FlexTable table = createSummariesTable();
    summariesPanel.remove(summariesTable);
    summariesTable = table;
    summariesPanel.insert(summariesTable, 1);
  }

  private String encode(String str) {
    if (str.equals("")) {
      return str;
    }
    return URL.encodeComponent(str);
  }

  private void fetchReport(int row, String token) {
    summariesTable.getRowFormatter().addStyleName(row, "viewer-SelectedRow");
    statusLabel.setText("Retrieving the report...");
    reportServer.getReport(token, new AsyncCallback<Report>() {
      public void onFailure(Throwable caught) {
        statusLabel.setText(caught.toString());
      }

      public void onSuccess(Report result) {
        report = result;
        statusLabel.setText("Finished fetching report details.");
        displayReport();
      }
    });
  }

  private String getImageUrl(String report, String category, String testClass, String testMethod,
      String agent) {
    return imageServer + encode(report) + "/" + encode(category) + "/" + encode(testClass) + "/"
        + encode(testMethod) + "/" + encode(agent);
  }

  private void init() {
    topPanel = new VerticalPanel();

    summariesPanel = new VerticalPanel();
    summariesPanel.add(new HTML("<h3>Benchmark Reports</h3>"));
    summariesTable = createSummariesTable();
    summariesPanel.add(summariesTable);

    reportPanel = new VerticalPanel();
    detailsLabel = new HTML();
    reportPanel.add(detailsLabel);
    reportTable = createReportTable();
    // reportPanel.add( reportTable );

    topPanel.add(summariesPanel);
    CellPanel spacerPanel = new HorizontalPanel();
    spacerPanel.setSpacing(10);
    spacerPanel.add(new Label());
    topPanel.add(spacerPanel);
    topPanel.add(reportPanel);
    final RootPanel root = RootPanel.get();

    root.add(topPanel);

    statusLabel = new HTML();
    root.add(statusLabel);

    selectReport(0, "");
  }
}
