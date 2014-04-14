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
package com.google.gwt.dev.util.log;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.HashSet;

/**
 * Abstract base class for TreeLoggers.
 */
public abstract class AbstractTreeLogger extends TreeLogger {

  private static class UncommittedBranchData {

    public final Throwable caught;
    public final String message;
    public final TreeLogger.Type type;
    private final HelpInfo helpInfo;

    public UncommittedBranchData(Type type, String message, Throwable caught,
        HelpInfo helpInfo) {
      this.caught = caught;
      this.message = message;
      this.type = type;
      this.helpInfo = helpInfo;
    }
  }

  // This message is package-protected so that the unit test can access it.
  static final String OUT_OF_MEMORY_MSG = "Out of memory; to increase the "
      + "amount of memory, use the -Xmx flag at startup (java -Xmx128M ...)";

  // This message is package-protected so that the unit test can access it.
  static final String STACK_OVERFLOW_MSG = "Stack overflow; to increase the "
      + "stack size, use the -Xss flag at startup (java -Xss1M ...)";

  public static String getStackTraceAsString(Throwable e) {
    // Show the exception info for anything other than "UnableToComplete".
    if (e == null || e instanceof UnableToCompleteException) {
      return null;
    }
    // For each cause, print the requested number of entries of its stack
    // trace, being careful to avoid getting stuck in an infinite loop.
    //
    StringBuffer message = new StringBuffer();
    Throwable currentCause = e;
    String causedBy = "";
    HashSet<Throwable> seenCauses = new HashSet<Throwable>();
    while (currentCause != null && !seenCauses.contains(currentCause)) {
      seenCauses.add(currentCause);

      message.append(causedBy);
      causedBy = "\nCaused by: "; // after 1st, all say "caused by"
      message.append(currentCause.getClass().getName());
      message.append(": " + currentCause.getMessage());
      StackTraceElement[] stackElems = currentCause.getStackTrace();
      if (stackElems != null) {
        for (int i = 0; i < stackElems.length; ++i) {
          message.append("\n\tat ");
          message.append(stackElems[i].toString());
        }
      }

      currentCause = currentCause.getCause();
    }
    return message.toString();
  }

  protected static String getExceptionName(Throwable e) {
    if (e == null || e instanceof UnableToCompleteException) {
      return null;
    }
    return e.getClass().getSimpleName();
  }

  protected TreeLogger.Type logLevel = TreeLogger.ALL;

  protected AbstractTreeLogger parent;

  private int indexWithinMyParent;

  private int nextChildIndex;

  private final Object nextChildIndexLock = new Object();

  private UncommittedBranchData uncommitted;

  /**
   * The constructor used when creating a top-level logger.
   */
  protected AbstractTreeLogger() {
  }

  /**
   * Implements branching behavior that supports lazy logging for low-priority
   * branched loggers.
   */
  @Override
  public final synchronized TreeLogger branch(TreeLogger.Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {

    if (msg == null) {
      msg = "(Null branch message)";
    }

    // Compute at which index the new child will be placed.
    //
    int childIndex = allocateNextChildIndex();

    // The derived class creates the child logger.
    AbstractTreeLogger childLogger = doBranch();

    // Set up the child logger.
    //
    // Unsynchronized operations on childLogger are safe since no other
    // thread could have a reference to it yet.
    childLogger.logLevel = logLevel;

    // Take a snapshot of the index that the branched child should have.
    //
    childLogger.indexWithinMyParent = childIndex;

    // Have the child hang onto this (its parent logger).
    //
    childLogger.parent = this;

    // We can avoid committing this branch entry until and unless some
    // child (or grandchild) tries to log something that is loggable,
    // in which case there will be cascading commits of the parent branches.
    //
    childLogger.uncommitted = new UncommittedBranchData(type, msg, caught,
        helpInfo);

    // This logic is intertwined with log(). If a log message is associated
    // with a special error condition, then we turn it into a branch,
    // so this method can be called directly from log(). It is of course
    // also possible for someone to call branch() directly. In either case, we
    // (1) turn the original message into an ERROR and
    // (2) drop an extra log message that explains how to recover
    String specialErrorMessage = causedBySpecialError(caught);
    if (specialErrorMessage != null) {
      type = TreeLogger.ERROR;
      childLogger.log(type, specialErrorMessage, null);
    }

    // Decide whether we want to log the branch message eagerly or lazily.
    //
    if (isLoggable(type)) {
      // We can commit this branch entry eagerly since it is a-priori loggable.
      // Commit the parent logger if necessary before continuing.
      //
      childLogger.commitMyBranchEntryInMyParentLogger();
    }

    return childLogger;
  }

  public final int getBranchedIndex() {
    return indexWithinMyParent;
  }

  public final synchronized TreeLogger.Type getMaxDetail() {
    return logLevel;
  }

  public final AbstractTreeLogger getParentLogger() {
    return parent;
  }

  @Override
  public final synchronized boolean isLoggable(TreeLogger.Type type) {
    return !type.isLowerPriorityThan(logLevel);
  }

  /**
   * Immediately logs or ignores the specified messages, based on the specified
   * message type and this logger's settings. If the message is loggable, then
   * parent branches may be lazily created before the log can take place.
   */
  @Override
  public final synchronized void log(TreeLogger.Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {

    if (msg == null) {
      msg = "(Null log message)";
    }

    // If this log message is caused by out of memory or stack overflow, we
    // provide a little extra help by creating a child log message.
    if (causedBySpecialError(caught) != null) {
      branch(TreeLogger.ERROR, msg, caught);
      return;
    }

    int childIndex = allocateNextChildIndex();
    if (isLoggable(type)) {
      commitMyBranchEntryInMyParentLogger();
      doLog(childIndex, type, msg, caught, helpInfo);
    }
  }

  /**
   * @param type the log type representing the most detailed level of logging
   *          that the caller is interested in, or <code>null</code> to choose
   *          the default level.
   */
  public final synchronized void setMaxDetail(TreeLogger.Type type) {
    if (type == null) {
      type = TreeLogger.INFO;
    }
    logLevel = type;
  }

  @Override
  public String toString() {
    return getLoggerId();
  }

  protected int allocateNextChildIndex() {
    synchronized (nextChildIndexLock) {
      // postincrement because we want indices to start at 0
      return nextChildIndex++;
    }
  }

  /**
   * Commits the branch after ensuring that the parent logger (if there is one)
   * has been committed first.
   */
  protected synchronized void commitMyBranchEntryInMyParentLogger() {
    // (Only the root logger doesn't have a parent.)
    //
    if (parent != null) {
      if (uncommitted != null) {
        // Commit the parent first.
        //
        parent.commitMyBranchEntryInMyParentLogger();

        // Let the subclass do its thing to commit this branch.
        //
        parent.doCommitBranch(this, uncommitted.type, uncommitted.message,
            uncommitted.caught, uncommitted.helpInfo);

        // Release the uncommitted state.
        //
        uncommitted = null;
      }
    }
  }

  /**
   * Derived classes should override this method to return a branched logger.
   */
  protected abstract AbstractTreeLogger doBranch();

  /**
   * @deprecated This method has been deprecated; override
   *             {@link #doCommitBranch(AbstractTreeLogger, com.google.gwt.core.ext.TreeLogger.Type, String, Throwable, com.google.gwt.core.ext.TreeLogger.HelpInfo)}
   *             instead.
   *
   * @param childBeingCommitted
   * @param type
   * @param msg
   * @param caught
   */
  @Deprecated
  protected final void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      TreeLogger.Type type, String msg, Throwable caught) {
  }

  /**
   * Derived classes should override this method to actually commit the
   * specified message associated with this the root of this branch.
   */
  protected abstract void doCommitBranch(
      AbstractTreeLogger childBeingCommitted, TreeLogger.Type type, String msg,
      Throwable caught, HelpInfo helpInfo);

  /**
   * @deprecated This method has been deprecated; override
   *             {@link #branch(com.google.gwt.core.ext.TreeLogger.Type, String, Throwable, com.google.gwt.core.ext.TreeLogger.HelpInfo)
   *             instead.
   *
   * @param indexOfLogEntryWithinParentLogger
   * @param type
   * @param msg
   * @param caught
   */
  @Deprecated
  protected final void doLog(int indexOfLogEntryWithinParentLogger,
      TreeLogger.Type type, String msg, Throwable caught) {
  }

  /**
   * Derived classes should override this method to actually write a log
   * message. Note that {@link #isLoggable(TreeLogger.Type)} will have already
   * been called.
   */
  protected abstract void doLog(int indexOfLogEntryWithinParentLogger,
      TreeLogger.Type type, String msg, Throwable caught, HelpInfo helpInfo);

  /**
   * Scans <code>t</code> and its causes for {@link OutOfMemoryError} or
   * {@link StackOverflowError}.
   *
   * @param t a possibly null {@link Throwable}
   * @return true if {@link OutOfMemoryError} or {@link StackOverflowError}
   *         appears anywhere in the cause list or if <code>t</code> is an
   *         {@link OutOfMemoryError} or {@link StackOverflowError.
   */
  private String causedBySpecialError(Throwable t) {
    while (t != null) {
      if (t instanceof OutOfMemoryError) {
        return OUT_OF_MEMORY_MSG;
      } else if (t instanceof StackOverflowError) {
        return STACK_OVERFLOW_MSG;
      }
      t = t.getCause();
    }
    return null;
  }

  private String getLoggerId() {
    if (parent != null) {
      if (parent.parent == null) {
        // Top-level
        return parent.getLoggerId() + getBranchedIndex();
      } else {
        // Nested
        return parent.getLoggerId() + "." + getBranchedIndex();
      }
    } else {
      // The root
      return "#";
    }
  }
}
