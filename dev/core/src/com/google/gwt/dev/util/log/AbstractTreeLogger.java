// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.log;

import com.google.gwt.core.ext.TreeLogger;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Abstract base class for TreeLoggers. 
 */
public abstract class AbstractTreeLogger implements TreeLogger {

  private static class UncommittedBranchData {

    public UncommittedBranchData(Type type, String message, Throwable exception) {
      fCaught = exception;
      fMessage = message;
      fType = type;
    }

    public final Throwable fCaught;
    public final String fMessage;
    public final TreeLogger.Type fType;
  }

  public static String getStackTraceAsString(Throwable e) {
    // For each cause, print the requested number of entries of its stack trace,
    // being careful to avoid getting stuck in an infinite loop.
    //
    StringBuffer message = new StringBuffer();
    Throwable currentCause = e;
    String causedBy = "";
    HashSet seenCauses = new HashSet();
    while (currentCause != null && !seenCauses.contains(currentCause)) {
      seenCauses.add(currentCause);

      StackTraceElement[] trace = currentCause.getStackTrace();
      message.append(causedBy);
      causedBy = "\nCaused by: "; // after 1st, all say "caused by"
      message.append(currentCause.getClass().getName());
      message.append(": " + currentCause.getMessage());
      StackTraceElement[] stackElems = findMeaningfulStackTraceElements(trace);
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

  private static StackTraceElement[] findMeaningfulStackTraceElements(
      StackTraceElement[] elems) {
    ArrayList goodElems = new ArrayList();
    StackTraceElement prevElem = null;
    for (int i = 0; i < elems.length; i++) {
      StackTraceElement elem = elems[i];
      if (elem.getLineNumber() > 0) {
        goodElems.add(elem);
        if (goodElems.size() < 10
          || prevElem.getClassName().equals(elem.getClassName())) {
          // Keep going.
          prevElem = elem;
        } else {
          // That's enough.
          break;
        }
      }
    }
    if (goodElems.size() > 0) {
      return (StackTraceElement[]) goodElems
        .toArray(new StackTraceElement[goodElems.size()]);
    } else {
      return null;
    }
  }

  /**
   * The constructor used when creating a top-level logger.
   */
  protected AbstractTreeLogger() {
  }

  /**
   * Implements branching behavior that supports lazy logging for low-priority
   * branched loggers.
   */
  public final synchronized TreeLogger branch(TreeLogger.Type type, String msg,
      Throwable caught) {

    if (msg == null) {
      msg = "(Null branch message)";
    }
    
    // Compute at which index the new child will be placed.
    //
    int childIndex = allocateNextChildIndex();

    // The derived class creates the child logger.
    AbstractTreeLogger childLogger = doBranch();

    // Set up the child logger.
    childLogger.fLogLevel = fLogLevel;

    // Take a snapshot of the index that the branched child should have.
    //
    childLogger.fIndexWithinMyParent = childIndex;

    // Have the child hang onto this (its parent logger).
    //
    childLogger.fParent = this;

    // We can avoid committing this branch entry until and unless a some
    // child (or grandchild) tries to log something that is loggable,
    // in which case there will be cascading commits of the parent branches.
    //
    childLogger.fUncommitted = new UncommittedBranchData(type, msg, caught);

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

  /**
   * Commits the branch after ensuring that the parent logger (if there is one)
   * has been committed first.
   */
  private synchronized void commitMyBranchEntryInMyParentLogger() {
    // (Only the root logger doesn't have a parent.)
    //
    if (fParent != null) {
      if (fUncommitted != null) {
        // Commit the parent first.
        //
        fParent.commitMyBranchEntryInMyParentLogger();

        // Let the subclass do its thing to commit this branch.
        //
        fParent.doCommitBranch(this, fUncommitted.fType, fUncommitted.fMessage,
          fUncommitted.fCaught);

        // Release the uncommitted state.
        //
        fUncommitted = null;
      }
    }
  }

  public final AbstractTreeLogger getParentLogger() {
    return fParent;
  }

  public final synchronized boolean isLoggable(TreeLogger.Type type) {
    TreeLogger.Type maxLevel = fLogLevel;
    while (maxLevel != null && maxLevel != type) {
      maxLevel = maxLevel.getParent();
    }
    return maxLevel == type;
  }

  /**
   * Immediately logs or ignores the specified messages, based on the specified
   * message type and this logger's settings. If the message is loggable, then
   * parent branches may be lazily created before the log can take place.
   */
  public final synchronized void log(TreeLogger.Type type, String msg,
      Throwable caught) {

    if (msg == null) {
      msg = "(Null log message)";
    }
    
    int childIndex = allocateNextChildIndex();
    if (isLoggable(type)) {
      commitMyBranchEntryInMyParentLogger();
      doLog(childIndex, type, msg, caught);
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
    fLogLevel = type;
  }

  public String toString() {
    return getLoggerId();
  }

  /**
   * Derived classes should override this method to return a branched logger.
   */
  protected abstract AbstractTreeLogger doBranch();

  /**
   * Derived classes should override this method to actually commit the
   * specified message associated with this the root of this branch.
   */
  protected abstract void doCommitBranch(
      AbstractTreeLogger childBeingCommitted, TreeLogger.Type type, String msg,
      Throwable caught);

  /**
   * Dervied classes should override this method to actually write a log
   * message. Note that {@link #isLoggable(TreeLogger.Type)} will have already
   * been called.
   */
  protected abstract void doLog(int indexOfLogEntryWithinParentLogger,
      TreeLogger.Type type, String msg, Throwable caught);

  private String getLoggerId() {
    if (fParent != null) {
      if (fParent.fParent == null) {
        // Top-level
        return fParent.getLoggerId() + getBranchedIndex();
      } else {
        // Nested
        return fParent.getLoggerId() + "." + getBranchedIndex();
      }
    } else {
      // The root
      return "#";
    }
  }

  private int allocateNextChildIndex() {
    synchronized (fNextChildIndexLock) {
      // postincrement because we want indices to start at 0
      return fNextChildIndex++;
    }
  }

  public final int getBranchedIndex() {
    return fIndexWithinMyParent;
  }

  private UncommittedBranchData fUncommitted;
  private TreeLogger.Type fLogLevel = TreeLogger.ALL;
  public int fIndexWithinMyParent;
  private int fNextChildIndex;
  private final Object fNextChildIndexLock = new Object();
  private AbstractTreeLogger fParent;
}
