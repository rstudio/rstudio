/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.viewer.server;

import com.google.gwt.junit.viewer.client.Benchmark;
import com.google.gwt.junit.viewer.client.BrowserInfo;
import com.google.gwt.junit.viewer.client.Category;
import com.google.gwt.junit.viewer.client.Report;
import com.google.gwt.junit.viewer.client.Result;
import com.google.gwt.junit.viewer.client.Trial;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves up report images for the ReportViewer application. Generates the
 * charts/graphs which contain the benchmarking data for a report.
 * 
 * <p>
 * This servlet requires the name of the report file, the category, the
 * benchmark class, the test method, and the browser agent.
 * <p>
 * 
 * <p>
 * An Example URI:
 * 
 * <pre>
 * /com.google.gwt.junit.viewer.ReportViewer/test_images/
 *   report-12345.xml/
 *   RemoveCategory/
 *   com.google.gwt.junit.client.ArrayListAndVectorBenchmark/
 *   testArrayListRemoves/
 *   Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.7.12) Gecko/20050920/
 * </pre>
 * 
 * </p>
 */
public class ReportImageServer extends HttpServlet {

  private static final String charset = "UTF-8";

  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[512];

    while (true) {
      int bytesRead = in.read(buf);
      if (bytesRead == -1) {
        break;
      }
      out.write(buf, 0, bytesRead);
    }
  }

  private JFreeChart createChart(String testName, Result result, String title) {

    // Display the trial data - we might need meta information from the result
    // that tells us how many total variables there are, what types they are of,
    // etc....
    List trials = result.getTrials();
    Trial firstTrial = (Trial) trials.get(0);

    // Pick the domain and series variables for our graph.
    // Right now we only handle up to two "user" variables.
    // We set the domain variable to the be the one containing the most unique
    // values.
    int numVariables = firstTrial.getVariables().size();

    String domainVariable = null;
    String seriesVariable = null;

    Map/* <String,Set<String>> */variableValues = null;

    if (numVariables == 1) {
      domainVariable = (String) firstTrial.getVariables().keySet().iterator().next();
    } else {
      // TODO(tobyr): Do something smarter, like allow the user to specify which
      // variables
      // are domain and series, along with the variables which are held
      // constant.

      variableValues = new HashMap();

      for (int i = 0; i < trials.size(); ++i) {
        Trial trial = (Trial) trials.get(i);
        Map variables = trial.getVariables();

        for (Iterator it = variables.entrySet().iterator(); it.hasNext();) {
          Map.Entry entry = (Map.Entry) it.next();
          String variable = (String) entry.getKey();
          String value = (String) entry.getValue();
          Set set = (Set) variableValues.get(variable);
          if (set == null) {
            set = new TreeSet();
            variableValues.put(variable, set);
          }
          set.add(value);
        }
      }

      TreeMap numValuesMap = new TreeMap();

      for (Iterator it = variableValues.entrySet().iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        String variable = (String) entry.getKey();
        Set values = (Set) entry.getValue();
        Integer numValues = new Integer(values.size());
        List variables = (List) numValuesMap.get(numValues);
        if (variables == null) {
          variables = new ArrayList();
          numValuesMap.put(numValues, variables);
        }
        variables.add(variable);
      }

      if (numValuesMap.values().size() > 0) {
        domainVariable = (String) ((List) numValuesMap.get(numValuesMap.lastKey())).get(0);
        seriesVariable = (String) ((List) numValuesMap.get(numValuesMap.firstKey())).get(0);
      }
    }

    String valueTitle = "time (ms)"; // This axis is time across all charts.

    if (numVariables == 0) {
      // Show a bar graph, with a single centered simple bar
      // 0 variables means there is only 1 trial
      Trial trial = (Trial) trials.iterator().next();

      DefaultCategoryDataset data = new DefaultCategoryDataset();
      data.addValue(trial.getRunTimeMillis(), "result", "result");

      return ChartFactory.createBarChart(title, testName, valueTitle, data,
          PlotOrientation.VERTICAL, false, false, false);
    } else if (numVariables == 1) {

      // Show a line graph with only 1 series
      // Or.... choose between a line graph and a bar graph depending upon
      // whether the
      // type of the domain is numeric.

      XYSeriesCollection data = new XYSeriesCollection();

      XYSeries series = new XYSeries(domainVariable);

      for (Iterator it = trials.iterator(); it.hasNext();) {
        Trial trial = (Trial) it.next();
        if (trial.getException() != null) {
          continue;
        }
        double time = trial.getRunTimeMillis();
        String domainValue = (String) trial.getVariables().get(domainVariable);
        series.add(Double.parseDouble(domainValue), time);
      }

      data.addSeries(series);

      return ChartFactory.createXYLineChart(title, domainVariable, valueTitle,
          data, PlotOrientation.VERTICAL, false, false, false);
    } else if (numVariables == 2) {
      // Show a line graph with multiple series
      XYSeriesCollection data = new XYSeriesCollection();

      Set seriesValues = (Set) variableValues.get(seriesVariable);

      for (Iterator it = seriesValues.iterator(); it.hasNext();) {
        String seriesValue = (String) it.next();
        XYSeries series = new XYSeries(seriesValue);

        for (Iterator trialsIt = trials.iterator(); trialsIt.hasNext();) {
          Trial trial = (Trial) trialsIt.next();
          if (trial.getException() != null) {
            continue;
          }
          Map variables = trial.getVariables();
          if (variables.get(seriesVariable).equals(seriesValue)) {
            double time = trial.getRunTimeMillis();
            String domainValue = (String) trial.getVariables().get(
                domainVariable);
            series.add(Double.parseDouble(domainValue), time);
          }
        }
        data.addSeries(series);
      }

      return ChartFactory.createXYLineChart(title, domainVariable, valueTitle,
          data, PlotOrientation.VERTICAL, true, true, false);
    }

    return null;

    // Sample JFreeChart code for creating certain charts:
    // Leaving this around until we can handle multivariate charts in dimensions
    // greater than two.

    // Code for creating a category data set - probably better with a bar chart
    // instead of line chart
    /*
     * DefaultCategoryDataset data = new DefaultCategoryDataset(); String series =
     * domainVariable;
     * 
     * for ( Iterator it = trials.iterator(); it.hasNext(); ) { Trial trial =
     * (Trial) it.next(); double time = trial.getRunTimeMillis(); String
     * domainValue = (String) trial.getVariables().get( domainVariable );
     * data.addValue( time, series, domainValue ); }
     * 
     * String title = ""; String categoryTitle = domainVariable; PlotOrientation
     * orientation = PlotOrientation.VERTICAL;
     * 
     * chart = ChartFactory.createLineChart( title, categoryTitle, valueTitle,
     * data, orientation, true, true, false );
     */

    /*
     * DefaultCategoryDataset data = new DefaultCategoryDataset(); String
     * series1 = "firefox"; String series2 = "ie";
     * 
     * data.addValue( 1.0, series1, "1024"); data.addValue( 2.0, series1,
     * "2048"); data.addValue( 4.0, series1, "4096"); data.addValue( 8.0,
     * series1, "8192");
     * 
     * data.addValue( 2.0, series2, "1024"); data.addValue( 4.0, series2,
     * "2048"); data.addValue( 8.0, series2, "4096"); data.addValue( 16.0,
     * series2,"8192");
     * 
     * String title = ""; String categoryTitle = "size"; PlotOrientation
     * orientation = PlotOrientation.VERTICAL;
     * 
     * chart = ChartFactory.createLineChart( title, categoryTitle, valueTitle,
     * data, orientation, true, true, false );
     */
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    handleRequest(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    handleRequest(request, response);
  }

  private Benchmark getBenchmarkByName(List benchmarks, String name) {
    for (Iterator it = benchmarks.iterator(); it.hasNext();) {
      Benchmark benchmark = (Benchmark) it.next();
      if (benchmark.getName().equals(name)) {
        return benchmark;
      }
    }
    return null;
  }

  private Category getCategoryByName(List categories, String categoryName) {
    for (Iterator catIt = categories.iterator(); catIt.hasNext();) {
      Category category = (Category) catIt.next();
      if (category.getName().equals(categoryName)) {
        return category;
      }
    }
    return null;
  }

  private Result getResultsByAgent(List results, String agent) {
    for (Iterator it = results.iterator(); it.hasNext();) {
      Result result = (Result) it.next();
      if (result.getAgent().equals(agent)) {
        return result;
      }
    }
    return null;
  }

  private void handleRequest(HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {

    String uri = request.getRequestURI();
    String requestString = uri.split("test_images/")[1];
    String[] requestParams = requestString.split("/");

    String reportName = URLDecoder.decode(requestParams[0], charset);
    String categoryName = URLDecoder.decode(requestParams[1], charset);
    String className = URLDecoder.decode(requestParams[2], charset);
    String testName = URLDecoder.decode(requestParams[3], charset);
    String agent = URLDecoder.decode(requestParams[4], charset);

    ReportDatabase db = ReportDatabase.getInstance();
    Report report = db.getReport(reportName);
    List categories = report.getCategories();
    Category category = getCategoryByName(categories, categoryName);
    List benchmarks = category.getBenchmarks();
    Benchmark benchmark = getBenchmarkByName(benchmarks, testName);
    List results = benchmark.getResults();
    Result result = getResultsByAgent(results, agent);

    String title = BrowserInfo.getBrowser(agent);
    JFreeChart chart = null;

    try {
      chart = createChart(testName, result, title);

      if (chart == null) {
        super.doGet(request, response);
        return;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    chart.getTitle().setFont(Font.decode("Arial"));

    // Try to fit all the graphs into a 1024 window, with a min of 240 and a max
    // of 480
    final int graphWidth = Math.max(240, Math.min(480,
        (1024 - 10 * results.size()) / results.size()));
    BufferedImage img = chart.createBufferedImage(graphWidth, 240);
    byte[] image = EncoderUtil.encode(img, ImageFormat.PNG);

    response.setContentType("image/png");

    OutputStream output = response.getOutputStream();
    copy(new ByteArrayInputStream(image), output);
  }
}
