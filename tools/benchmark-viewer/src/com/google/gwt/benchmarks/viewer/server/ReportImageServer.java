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
package com.google.gwt.benchmarks.viewer.server;

import com.google.gwt.benchmarks.viewer.client.Benchmark;
import com.google.gwt.benchmarks.viewer.client.BrowserInfo;
import com.google.gwt.benchmarks.viewer.client.Category;
import com.google.gwt.benchmarks.viewer.client.Report;
import com.google.gwt.benchmarks.viewer.client.Result;
import com.google.gwt.benchmarks.viewer.client.Trial;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletContext;
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

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      handleRequest(request, response);
    } catch (Exception e) {
      if (e.getClass().getName().endsWith(".ClientAbortException")
          || e.getClass().getName().endsWith(".EofException")) {
        // No big deal, the client browser terminated a download.
      } else {
        logException("An error occured while trying to create the chart.", e, response);
      }
      return;
    }
  }

  private JFreeChart createChart(String testName, Result result, String title,
      List<Result> comparativeResults) {

    // Find the maximum values across both axes for all of the results
    // (this chart's own results, plus all comparative results).
    //
    // This is a stop-gap solution that helps us compare different charts for
    // the same benchmark method (usually with different user agents) until we
    // get real comparative functionality in version two.

    double maxTime = 0;

    for (Result r : comparativeResults) {
      for (Trial t : r.getTrials()) {
        maxTime = Math.max(maxTime, t.getRunTimeMillis());
      }
    }

    // Determine the number of variables in this benchmark method
    List<Trial> trials = result.getTrials();
    Trial firstTrial = new Trial();
    int numVariables = 0;
    if (trials.size() > 0) {
      firstTrial = trials.get(0);
      numVariables = firstTrial.getVariables().size();
    }

    // Display the trial data.
    //
    // First, pick the domain and series variables for our graph.
    // Right now we only handle up to two "user" variables.
    // We set the domain variable to the be the one containing the most unique
    // values.
    // This might be easier if the results had meta information telling us
    // how many total variables there are, what types they are of, etc....

    String domainVariable = null;
    String seriesVariable = null;

    Map<String, Set<String>> variableValues = null;

    if (numVariables == 1) {
      domainVariable = firstTrial.getVariables().keySet().iterator().next();
    } else {
      // TODO(tobyr): Do something smarter, like allow the user to specify which
      // variables are domain and series, along with the variables which are
      // held constant.

      variableValues = new HashMap<String, Set<String>>();

      for (int i = 0; i < trials.size(); ++i) {
        Trial trial = trials.get(i);
        Map<String, String> variables = trial.getVariables();

        for (Map.Entry<String, String> entry : variables.entrySet()) {
          String variable = entry.getKey();
          Set<String> set = variableValues.get(variable);
          if (set == null) {
            set = new TreeSet<String>();
            variableValues.put(variable, set);
          }
          set.add(entry.getValue());
        }
      }

      TreeMap<Integer, List<String>> numValuesMap = new TreeMap<Integer, List<String>>();

      for (Map.Entry<String, Set<String>> entry : variableValues.entrySet()) {
        Integer numValues = new Integer(entry.getValue().size());
        List<String> variables = numValuesMap.get(numValues);
        if (variables == null) {
          variables = new ArrayList<String>();
          numValuesMap.put(numValues, variables);
        }
        variables.add(entry.getKey());
      }

      if (numValuesMap.values().size() > 0) {
        domainVariable = numValuesMap.get(numValuesMap.lastKey()).get(0);
        seriesVariable = numValuesMap.get(numValuesMap.firstKey()).get(0);
      }
    }

    String valueTitle = "time (ms)"; // This axis is time across all charts.

    if (numVariables == 0) {
      // Show a bar graph, with a single centered simple bar
      // 0 variables means there is only 1 trial
      DefaultCategoryDataset data = new DefaultCategoryDataset();
      data.addValue(firstTrial.getRunTimeMillis(), "result", "result");

      JFreeChart chart =
          ChartFactory.createBarChart(title, testName, valueTitle, data, PlotOrientation.VERTICAL,
              false, false, false);
      CategoryPlot p = chart.getCategoryPlot();
      ValueAxis axis = p.getRangeAxis();
      axis.setUpperBound(maxTime + maxTime * 0.1);
      return chart;
    } else if (numVariables == 1) {

      // Show a line graph with only 1 series
      // Or.... choose between a line graph and a bar graph depending upon
      // whether the type of the domain is numeric.

      XYSeriesCollection data = new XYSeriesCollection();

      XYSeries series = new XYSeries(domainVariable);

      for (Trial trial : trials) {
        double time = trial.getRunTimeMillis();
        String domainValue = trial.getVariables().get(domainVariable);
        series.add(Double.parseDouble(domainValue), time);
      }

      data.addSeries(series);

      JFreeChart chart =
          ChartFactory.createXYLineChart(title, domainVariable, valueTitle, data,
              PlotOrientation.VERTICAL, false, false, false);
      XYPlot plot = chart.getXYPlot();
      plot.getRangeAxis().setUpperBound(maxTime + maxTime * 0.1);
      double maxDomainValue = getMaxValue(comparativeResults, domainVariable);
      plot.getDomainAxis().setUpperBound(maxDomainValue + maxDomainValue * 0.1);
      return chart;
    } else if (numVariables == 2) {
      // Show a line graph with two series
      XYSeriesCollection data = new XYSeriesCollection();

      Set<String> seriesValues = variableValues.get(seriesVariable);

      for (String seriesValue : seriesValues) {
        XYSeries series = new XYSeries(seriesValue);

        for (Trial trial : trials) {
          Map<String, String> variables = trial.getVariables();
          if (variables.get(seriesVariable).equals(seriesValue)) {
            double time = trial.getRunTimeMillis();
            String domainValue = trial.getVariables().get(domainVariable);
            series.add(Double.parseDouble(domainValue), time);
          }
        }
        data.addSeries(series);
      }
      // TODO(tobyr) - Handle graphs above 2 variables

      JFreeChart chart =
          ChartFactory.createXYLineChart(title, domainVariable, valueTitle, data,
              PlotOrientation.VERTICAL, true, true, false);
      XYPlot plot = chart.getXYPlot();
      plot.getRangeAxis().setUpperBound(maxTime + maxTime * 0.1);
      double maxDomainValue = getMaxValue(comparativeResults, domainVariable);
      plot.getDomainAxis().setUpperBound(maxDomainValue + maxDomainValue * 0.1);
      return chart;
    }

    throw new RuntimeException("The ReportImageServer is not yet able to "
        + "create charts for benchmarks with more than two variables.");

    // Sample JFreeChart code for creating certain charts:
    // Leaving this around until we can handle multivariate charts in dimensions
    // greater than two.

    // Code for creating a category data set - probably better with a bar chart
    // instead of line chart
    /*
     * DefaultCategoryDataset data = new DefaultCategoryDataset(); String series
     * = domainVariable;
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

  private Benchmark getBenchmarkByName(List<Benchmark> benchmarks, String name) {
    for (Benchmark benchmark : benchmarks) {
      if (benchmark.getName().equals(name)) {
        return benchmark;
      }
    }
    return null;
  }

  private Category getCategoryByName(List<Category> categories, String categoryName) {
    for (Category category : categories) {
      if (category.getName().equals(categoryName)) {
        return category;
      }
    }
    return null;
  }

  private DrawingSupplier getDrawingSupplier() {
    Color[] colors = new Color[] {new Color(176, 29, 29, 175), // dark red
        new Color(10, 130, 86, 175), // dark green
        new Color(8, 26, 203, 175), // dark blue
        new Color(145, 162, 66, 175), // light pea green
        new Color(196, 140, 6, 175), // sienna
    };

    float size = 8;
    float offset = size / 2;

    int iOffset = (int) offset;

    Shape square = new Rectangle2D.Double(-offset, -offset, size, size);
    Shape circle = new Ellipse2D.Double(-offset, -offset, size, size);
    Shape triangle =
        new Polygon(new int[] {0, iOffset, -iOffset}, new int[] {-iOffset, iOffset, iOffset}, 3);
    Shape diamond =
        new Polygon(new int[] {0, iOffset, 0, -iOffset}, new int[] {-iOffset, 0, iOffset, 0}, 4);
    Shape ellipse = new Ellipse2D.Double(-offset, -offset / 2, size, size / 2);

    return new DefaultDrawingSupplier(colors,
        DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE, new Shape[] {
            circle, square, triangle, diamond, ellipse});
  }

  private double getMaxValue(List<Result> results, String variable) {
    double value = 0.0;

    for (int i = 0; i < results.size(); ++i) {
      Result r = results.get(i);
      List<Trial> resultTrials = r.getTrials();

      for (int j = 0; j < resultTrials.size(); ++j) {
        Trial t = resultTrials.get(j);
        Map<String, String> variables = t.getVariables();
        value = Math.max(value, Double.parseDouble(variables.get(variable)));
      }
    }

    return value;
  }

  private Result getResultsByAgent(List<Result> results, String agent) {
    for (Object element : results) {
      Result result = (Result) element;
      if (result.getAgent().equals(agent)) {
        return result;
      }
    }
    return null;
  }

  private void handleRequest(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    String uri = request.getRequestURI();
    String requestString = uri.split("test_images/")[1];
    String[] requestParams = requestString.split("/");

    String reportName = URLDecoder.decode(requestParams[0], charset);
    String categoryName = URLDecoder.decode(requestParams[1], charset);
    // String className = URLDecoder.decode(requestParams[2], charset);
    String testName = URLDecoder.decode(requestParams[3], charset);
    String agent = URLDecoder.decode(requestParams[4], charset);

    ReportDatabase db = ReportDatabase.getInstance();
    Report report = db.getReport(reportName);
    List<Category> categories = report.getCategories();
    Category category = getCategoryByName(categories, categoryName);
    List<Benchmark> benchmarks = category.getBenchmarks();
    Benchmark benchmark = getBenchmarkByName(benchmarks, testName);
    List<Result> results = benchmark.getResults();
    Result result = getResultsByAgent(results, agent);

    String title = BrowserInfo.getBrowser(agent);
    JFreeChart chart = createChart(testName, result, title, results);

    chart.getTitle().setFont(Font.decode("Verdana BOLD 12"));
    chart.setAntiAlias(true);
    chart.setBorderVisible(true);
    chart.setBackgroundPaint(new Color(241, 241, 241));

    Plot plot = chart.getPlot();

    plot.setDrawingSupplier(getDrawingSupplier());
    plot.setBackgroundPaint(new GradientPaint(0, 0, Color.white, 640, 480, new Color(200, 200, 200)));

    if (plot instanceof XYPlot) {
      XYPlot xyplot = (XYPlot) plot;
      Font labelFont = Font.decode("Verdana PLAIN");
      xyplot.getDomainAxis().setLabelFont(labelFont);
      xyplot.getRangeAxis().setLabelFont(labelFont);
      org.jfree.chart.renderer.xy.XYItemRenderer xyitemrenderer = xyplot.getRenderer();
      xyitemrenderer.setStroke(new BasicStroke(4));
      if (xyitemrenderer instanceof XYLineAndShapeRenderer) {
        XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyitemrenderer;
        xylineandshaperenderer.setShapesVisible(true);
        xylineandshaperenderer.setShapesFilled(true);
      }
    }

    // Try to fit all the graphs into a 1024 window, with a min of 240 and a max
    // of 480
    final int graphWidth =
        Math.max(240, Math.min(480, (1024 - 10 * results.size()) / results.size()));
    BufferedImage img = chart.createBufferedImage(graphWidth, 240);
    byte[] image = EncoderUtil.encode(img, ImageFormat.PNG);

    // The images have unique URLs; might as well set them to never expire.
    response.setHeader("Cache-Control", "max-age=0");
    response.setHeader("Expires", "Fri, 2 Jan 1970 00:00:00 GMT");
    response.setContentType("image/png");
    response.setContentLength(image.length);

    OutputStream output = response.getOutputStream();
    output.write(image);
  }

  private void logException(String msg, Exception e, HttpServletResponse response) {
    ServletContext servletContext = getServletContext();
    servletContext.log(msg, e);
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
