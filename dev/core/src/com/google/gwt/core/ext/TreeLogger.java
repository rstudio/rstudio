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
package com.google.gwt.core.ext;

import java.net.URL;

/**
 * An interface used to log messages in deferred binding generators.
 */
public abstract class TreeLogger {

  /**
   * Provides extra information to the user, generally details of what caused
   * the problem or what the user should do to fix the problem. How this
   * information is interpreted and displayed is implementation-dependent.
   */
  public abstract static class HelpInfo {

    /**
     * @return the text to use for an anchor if not null and getURL is non-null.
     */
    public String getAnchorText() {
      return null;
    }

    /**
     * @return the prefix to go before the link.
     */
    public String getPrefix() {
      return "More info: ";
    }

    /**
     * @return a URL containing extra information about the problem, or null if
     *     none.
     */
    public URL getURL() {
      return null;
    }
  }

  /**
   * A type-safe enum of all possible logging severity types.
   */
  @SuppressWarnings("hiding")
  public enum Type {

    /**
     * Logs an error.
     */
    ERROR(true),

    /**
     * Logs a warning.
     */
    WARN(true),

    /**
     * Logs information.
     */
    INFO(false),

    /**
     * Logs information related to lower-level operation.
     */
    TRACE(false),

    /**
     * Logs detailed information that could be useful during debugging.
     */
    DEBUG(false),

    /**
     * Logs extremely verbose and detailed information that is typically useful
     * only to product implementors.
     */
    SPAM(false),

    /**
     * Logs everything -- quite a bit of stuff.
     */
    ALL(false);

    /**
     * Gets all the possible severity types as an array.
     * 
     * @return an array of severity types
     */
    public static Type[] instances() {
      return Type.values();
    }

    private final boolean needsAttention;

    /**
     * Constructs a log type with an optional parent.
     */
    private Type(boolean needsAttention) {
      this.needsAttention = needsAttention;
    }

    /**
     * Gets the label for this severity type.
     * 
     * @return the label
     */
    public String getLabel() {
      return this.toString();
    }

    /**
     * Determines whether this log type is of lower priority than some other log
     * type.
     * 
     * @param other the other log type
     * @return <code>true</code> if this log type is lower priority
     */
    public boolean isLowerPriorityThan(Type other) {
      // Counterintuitive: higher number is lower priority.
      return this.ordinal() > other.ordinal();
    }

    /**
     * Indicates whether this severity type represents a high severity that
     * should be highlighted for the user.
     * 
     * @return <code>true</code> if this severity is high, otherwise
     *         <code>false</code>.
     */
    public boolean needsAttention() {
      return needsAttention;
    }
  }

  /**
   * Logs an error.
   */
  public static final Type ERROR = Type.ERROR;

  /**
   * Logs a warning.
   */
  public static final Type WARN = Type.WARN;

  /**
   * Logs information.
   */
  public static final Type INFO = Type.INFO;

  /**
   * Logs information related to lower-level operation.
   */
  public static final Type TRACE = Type.TRACE;

  /**
   * Logs detailed information that could be useful during debugging.
   */
  public static final Type DEBUG = Type.DEBUG;

  /**
   * Logs extremely verbose and detailed information that is typically useful
   * only to product implementors.
   */
  public static final Type SPAM = Type.SPAM;

  /**
   * Logs everything -- quite a bit of stuff.
   */
  public static final Type ALL = Type.ALL;

  /**
   * A valid logger that ignores all messages. Occasionally useful when calling
   * methods that require a logger parameter.
   */
  public static final TreeLogger NULL = new TreeLogger() {
    @Override
    public TreeLogger branch(Type type, String msg, Throwable caught,
        HelpInfo helpInfo) {
      return this;
    }

    @Override
    public boolean isLoggable(Type type) {
      return false;
    }

    @Override
    public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
      // nothing
    }
  };

  /**
   * Calls
   * {@link #branch(com.google.gwt.core.ext.TreeLogger.Type, String, Throwable, com.google.gwt.core.ext.TreeLogger.HelpInfo)}
   * with a <code>null</code> <code>caught</code> and <code>helpInfo</code>.
   */
  public final TreeLogger branch(TreeLogger.Type type, String msg) {
    return branch(type, msg, null, null);
  }

  /**
   * Calls
   * {@link #branch(com.google.gwt.core.ext.TreeLogger.Type, String, Throwable, com.google.gwt.core.ext.TreeLogger.HelpInfo)}
   * with a <code>null</code> <code>helpInfo</code>.
   */
  public final TreeLogger branch(TreeLogger.Type type, String msg,
      Throwable caught) {
    return branch(type, msg, caught, null);
  }

  /**
   * Produces a branched logger, which can be used to write messages that are
   * logically grouped together underneath the current logger. The details of
   * how/if the resulting messages are displayed is implementation-dependent.
   * 
   * <p>
   * The log message supplied when branching serves two purposes. First, the
   * message should be considered a heading for all the child messages below it.
   * Second, the <code>type</code> of the message provides a hint as to the
   * importance of the children below it. As an optimization, an implementation
   * could return a "no-op" logger if messages of the specified type weren't
   * being logged, which the implication being that all nested log messages were
   * no more important than the level of their branch parent.
   * </p>
   * 
   * <p>
   * As an example of how hierarchical logging can be used, a branched logger in
   * a GUI could write log message as child items of a parent node in a tree
   * control. If logging to streams, such as a text console, the branched logger
   * could prefix each entry with a unique string and indent its text so that it
   * could be sorted later to reconstruct a proper hierarchy.
   * </p>
   * 
   * @param type
   * @param msg an optional message to log, which can be <code>null</code> if
   *          only an exception is being logged
   * @param caught an optional exception to log, which can be <code>null</code>
   *          if only a message is being logged
   * @param helpInfo extra information that might be used by the logger to
   *          provide extended information to the user
   * @return an instance of {@link TreeLogger} representing the new branch of
   *         the log; may be the same instance on which this method is called
   */
  public abstract TreeLogger branch(TreeLogger.Type type, String msg,
      Throwable caught, HelpInfo helpInfo);

  /**
   * Determines whether or not a log entry of the specified type would actually
   * be logged. Caller use this method to avoid constructing log messages that
   * would be thrown away.
   */
  public abstract boolean isLoggable(TreeLogger.Type type);

  /**
   * Calls {@link #log(TreeLogger.Type, String, Throwable, HelpInfo)} with a
   * <code>null</code> <code>caught</code> and <code>helpInfo</code>.
   */
  public final void log(TreeLogger.Type type, String msg) {
    log(type, msg, null, null);
  }

  /**
   * Calls {@link #log(TreeLogger.Type, String, Throwable, HelpInfo)} with a
   * <code>null</code> <code>helpInfo</code>.
   */
  public final void log(TreeLogger.Type type, String msg, Throwable caught) {
    log(type, msg, caught, null);
  }

  /**
   * Logs a message and/or an exception, with optional help info. It is also
   * legal to call this method using <code>null</code> arguments for <i>both</i>
   * <code>msg</code> and <code>caught</code>, in which case the log event
   * can be ignored. The <code>info</code> can provide extra information to
   * the logger; a logger may choose to ignore this info.
   * 
   * @param type
   * @param msg an optional message to log, which can be <code>null</code> if
   *          only an exception is being logged
   * @param caught an optional exception to log, which can be <code>null</code>
   *          if only a message is being logged
   * @param helpInfo extra information that might be used by the logger to
   *          provide extended information to the user
   */
  public abstract void log(TreeLogger.Type type, String msg, Throwable caught,
      HelpInfo helpInfo);
}
