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

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.viewer.client.Report;
import com.google.gwt.benchmarks.viewer.client.ReportSummary;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Serves up benchmark reports created during JUnit execution.
 * 
 * The benchmark reports are read from the path specified by the system property
 * named <code>Benchmark.REPORT_PATH</code>. In the case the property is not
 * set, they are read from the user's current working directory.
 */
public class ReportDatabase {

  /**
   * Indicates that a supplied path was invalid.
   * 
   */
  public static class BadPathException extends RuntimeException {
    String path;

    public BadPathException(String path) {
      super("The path " + path + " does not exist.");
      this.path = path;
    }

    public String getPath() {
      return path;
    }
  }

  private static class ReportEntry {
    private long lastModified;
    private Report report;
    private ReportSummary summary;

    public ReportEntry(Report report, ReportSummary summary, long lastModified) {
      this.report = report;
      this.summary = summary;
      this.lastModified = lastModified;
    }
  }

  private static class ReportFile {
    File file;
    long lastModified;

    ReportFile(File f) {
      this.file = f;
      this.lastModified = f.lastModified();
    }
  }

  private static ReportDatabase database = new ReportDatabase();

  /**
   * The amount of time to go between report updates.
   */
  private static final int UPDATE_DURATION_MILLIS = 30000;

  public static ReportDatabase getInstance() {
    return database;
  }

  private static String getReportId(File f) {
    return f.getName();
  }

  /**
   * The last time we updated our reports.
   */
  private long lastUpdateMillis = -1L;

  /**
   * The path to read benchmark reports from.
   */
  private final String reportPath;

  /**
   * A list of all reports by id.
   */
  private Map<String, ReportEntry> reports = new HashMap<String, ReportEntry>();

  /**
   * Lock for reports.
   */
  private Object reportsLock = new Object();

  /**
   * Lock for updating from file system. (Guarantees a single update while not
   * holding reportsLock open).
   */
  private Object updateLock = new Object();

  /**
   * Are we currently undergoing updating?
   */
  private boolean updating = false;

  private ReportDatabase() throws BadPathException {
    String path = System.getProperty(Benchmark.REPORT_PATH);
    if (path == null || path.trim().equals("")) {
      path = System.getProperty("user.dir");
    }
    reportPath = path;

    if (!new File(reportPath).exists()) {
      throw new BadPathException(reportPath);
    }
  }

  public Report getReport(String reportId) {
    synchronized (reportsLock) {
      ReportEntry entry = reports.get(reportId);
      return entry == null ? null : entry.report;
    }
  }

  public List<ReportSummary> getReportSummaries() {

    /**
     * There are probably ways to make this faster, but I've taken basic
     * precautions to try to make this scale ok with multiple clients.
     */

    boolean update = false;

    // See if we need to do an update
    // Go ahead and let others continue reading, even if an update is required.
    synchronized (updateLock) {
      if (!updating) {
        long currentTime = System.currentTimeMillis();

        if (currentTime > lastUpdateMillis + UPDATE_DURATION_MILLIS) {
          update = updating = true;
        }
      }
    }

    if (update) {
      updateReports();
    }

    synchronized (reportsLock) {
      List<ReportSummary> summaries = new ArrayList<ReportSummary>(reports.size());
      for (ReportEntry entry : reports.values()) {
        summaries.add(entry.summary);
      }
      return summaries;
    }
  }

  private void updateReports() {

    File path = new File(reportPath);

    File[] files = path.listFiles(new FilenameFilter() {
      public boolean accept(File f, String name) {
        return name.startsWith("report-") && name.endsWith(".xml");
      }
    });

    Map<String, ReportEntry> filesToUpdate = new HashMap<String, ReportEntry>();
    Map<String, ReportFile> filesById = new HashMap<String, ReportFile>();
    for (int i = 0; i < files.length; ++i) {
      File f = files[i];
      filesById.put(getReportId(f), new ReportFile(f));
    }

    // Lock temporarily so we can determine what needs updating
    // (This could be a read-lock - not a general read-write lock,
    // if we moved dead report removal outside of this critical section).
    synchronized (reportsLock) {

      // Add reports which need to be updated or are new
      for (int i = 0; i < files.length; ++i) {
        File file = files[i];
        String reportId = getReportId(file);
        ReportEntry entry = reports.get(reportId);
        if (entry == null || entry.lastModified < file.lastModified()) {
          filesToUpdate.put(reportId, null);
        }
      }

      // Remove reports which no longer exist
      for (Iterator<String> it = reports.keySet().iterator(); it.hasNext();) {
        if (filesById.get(it.next()) == null) {
          it.remove();
        }
      }
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setIgnoringElementContentWhitespace(true);
      factory.setIgnoringComments(true);
      DocumentBuilder builder = factory.newDocumentBuilder();

      for (String id : filesToUpdate.keySet()) {
        ReportFile reportFile = filesById.get(id);
        String filePath = reportFile.file.getAbsolutePath();
        Document doc = builder.parse(filePath);
        Report report = ReportXml.fromXml(doc.getDocumentElement());
        report.setId(id);
        ReportSummary summary = report.getSummary();
        long lastModified = new File(filePath).lastModified();
        filesToUpdate.put(id, new ReportEntry(report, summary, lastModified));
      }

      // Update the reports
      synchronized (reportsLock) {
        for (String id : filesToUpdate.keySet()) {
          reports.put(id, filesToUpdate.get(id));
        }
      }
    } catch (Exception e) {
      // Even if we got an error, we'll just try again on the next update
      // This might happen if a report has only been partially written, for
      // example.
      e.printStackTrace();
    }

    synchronized (updateLock) {
      updating = false;
      lastUpdateMillis = System.currentTimeMillis();
    }
  }
}
