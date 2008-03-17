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
public class UnitTestTreeLogger implements TreeLogger {

  /**
   * Simplifies the creation of a {@link UnitTestTreeLogger} by providing
   * convenience methods for specifying the expected log events.
   */
  public static class Builder {

    private final List<LogEntry> expected = new ArrayList<LogEntry>();

    public Builder() {
    }

    public UnitTestTreeLogger createLogger() {
      return new UnitTestTreeLogger(expected);
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
  }

  /**
   * Represents a log event to check for.
   */
  private static class LogEntry {
    private final Type type;
    private final String msg;
    private final Throwable caught;

    public LogEntry(TreeLogger.Type type, String msg, Throwable caught) {
      assert (type != null);
      assert (msg != null);
      this.type = type;
      this.msg = msg;
      this.caught = caught;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof LogEntry) {
        LogEntry other = (LogEntry) obj;
        if (getType().equals(other.getType())) {
          if (getMessage().equals(other.getMessage())) {
            if (caught == null ? other.getCaught() == null : caught.equals(other.getCaught())) {
              return true;
            }
          }
        }
      }
      return false;
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
    public int hashCode() {
      return getMessage().hashCode();
    }

    @Override
    public String toString() {
      Throwable t = getCaught();
      String caughtStr = (t != null ? ": " + t.getClass().getName() : "");
      return type.getLabel() + ": " + getMessage() + caughtStr;
    }
  }

  private final List<LogEntry> actualEntries = new ArrayList<LogEntry>();
  private final List<LogEntry> expectedEntries = new ArrayList<LogEntry>();
  private final EnumSet<TreeLogger.Type> loggableTypes = EnumSet.noneOf(TreeLogger.Type.class);

  public UnitTestTreeLogger(List<LogEntry> expectedEntries) {
    this.expectedEntries.addAll(expectedEntries);

    // Infer the set of types that are loggable.
    for (LogEntry entry : expectedEntries) {
      loggableTypes.add(entry.getType());
    }
  }

  public void assertCorrectLogEntries() {
    LogEntry expectedEntry = expectedEntries.isEmpty() ? null : expectedEntries.get(0);
    if (expectedEntry != null) {
      Assert.fail("Never received log entry: " + expectedEntry);
    }
  }

  public TreeLogger branch(Type type, String msg, Throwable caught) {
    log(type, msg, caught);
    return this;
  }

  public boolean isLoggable(Type type) {
    return loggableTypes.contains(type);
  }

  public void log(Type type, String msg, Throwable caught) {
    LogEntry actualEntry = new LogEntry(type, msg, caught);
    actualEntries.add(actualEntry);

    if (expectedEntries.isEmpty()) {
      Assert.fail("Unexpected trailing log entry: " + actualEntry);
    } else {
      LogEntry expectedEntry = expectedEntries.get(0);
      Assert.assertEquals(expectedEntry, actualEntry);
      expectedEntries.remove(0);
    }
  }
}
