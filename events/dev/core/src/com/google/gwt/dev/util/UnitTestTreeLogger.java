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
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.TreeLogger;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A {@link TreeLogger} implementation that can be used during JUnit tests to
 * check for a specified sequence of log events.
 */
public class UnitTestTreeLogger extends TreeLogger {

  /**
   * Simplifies the creation of a {@link UnitTestTreeLogger} by providing
   * convenience methods for specifying the expected log events.
   */
  public static class Builder {

    private final List<LogEntry> expected = new ArrayList<LogEntry>();
    private EnumSet<Type> loggableTypes = EnumSet.allOf(TreeLogger.Type.class);

    public Builder() {
    }

    public UnitTestTreeLogger createLogger() {
      return new UnitTestTreeLogger(expected, loggableTypes);
    }

    public void expect(TreeLogger.Type type, String msg, Throwable caught) {
      expected.add(new LogEntry(type, msg, caught));
    }

    public void expectDebug(String msg, Throwable caught) {
      expect(TreeLogger.DEBUG, msg, caught);
    }

    public void expectError(String msg, Throwable caught) {
      expect(TreeLogger.ERROR, msg, caught);
    }

    public void expectInfo(String msg, Throwable caught) {
      expect(TreeLogger.INFO, msg, caught);
    }

    public void expectSpam(String msg, Throwable caught) {
      expect(TreeLogger.SPAM, msg, caught);
    }

    public void expectTrace(String msg, Throwable caught) {
      expect(TreeLogger.TRACE, msg, caught);
    }

    public void expectWarn(String msg, Throwable caught) {
      expect(TreeLogger.WARN, msg, caught);
    }

    /**
     * Sets the loggable types based on an explicit set.
     */
    public void setLoggableTypes(EnumSet<TreeLogger.Type> loggableTypes) {
      this.loggableTypes = loggableTypes;
    }

    /**
     * Sets the loggable types based on a lowest log level.
     */
    public void setLowestLogLevel(TreeLogger.Type lowestLogLevel) {
      loggableTypes.clear();
      for (Type type : TreeLogger.Type.values()) {
        if (!type.isLowerPriorityThan(lowestLogLevel)) {
          loggableTypes.add(type);
        }
      }
    }
  }

  /**
   * Represents a log event to check for.
   */
  private static class LogEntry {
    private final Throwable caught;
    private final String msg;
    private final Type type;

    public LogEntry(TreeLogger.Type type, String msg, Throwable caught) {
      assert (type != null);
      this.type = type;
      this.msg = msg;
      this.caught = caught;
    }

    public Throwable getCaught() {
      return caught;
    }

    public String getMessage() {
      return msg;
    }

    public Type getType() {
      return type;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(type.getLabel());
      sb.append(": ");
      sb.append(getMessage());
      Throwable t = getCaught();
      if (t != null) {
        sb.append("; ");
        sb.append(t);
      }
      return sb.toString();
    }
  }

  private static String createLog(List<LogEntry> entries) {
    StringBuilder sb = new StringBuilder();
    for (LogEntry entry : entries) {
      sb.append(entry.toString());
      sb.append('\n');
    }
    return sb.toString();
  }

  private final List<LogEntry> actualEntries = new ArrayList<LogEntry>();
  private final List<LogEntry> expectedEntries = new ArrayList<LogEntry>();
  private final EnumSet<TreeLogger.Type> loggableTypes;

  public UnitTestTreeLogger(List<LogEntry> expectedEntries,
      EnumSet<TreeLogger.Type> loggableTypes) {
    this.expectedEntries.addAll(expectedEntries);
    this.loggableTypes = loggableTypes;

    // Sanity check that all expected entries are actually loggable.
    for (LogEntry entry : expectedEntries) {
      Type type = entry.getType();
      Assert.assertTrue("Cannot expect an entry of a non-loggable type!",
          isLoggable(type));
      loggableTypes.add(type);
    }
  }

  public void assertCorrectLogEntries() {
    String expected = createLog(expectedEntries);
    String actual = createLog(actualEntries);
    Assert.assertEquals("Logs do not match", expected, actual);
  }

  public TreeLogger branch(Type type, String msg, Throwable caught,
      HelpInfo helpInfo) {
    log(type, msg, caught);
    return this;
  }

  public boolean isLoggable(Type type) {
    return loggableTypes.contains(type);
  }

  public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    if (!isLoggable(type)) {
      return;
    }
    LogEntry actualEntry = new LogEntry(type, msg, caught);
    actualEntries.add(actualEntry);
  }
}
