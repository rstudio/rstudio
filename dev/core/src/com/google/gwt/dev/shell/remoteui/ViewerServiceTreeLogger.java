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
package com.google.gwt.dev.shell.remoteui;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.Callback;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import java.util.List;

/**
 * A tree logger that creates log entries using a ViewerService.
 */
public final class ViewerServiceTreeLogger extends AbstractTreeLogger {

  private abstract class Pending {
    protected final Throwable caught;
    protected final HelpInfo helpInfo;
    protected final String msg;
    protected final TreeLogger.Type type;

    public Pending(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
      this.caught = caught;
      this.msg = msg;
      this.type = type;
      this.helpInfo = helpInfo;
    }

    public abstract void send();
  }

  private class PendingBranch extends Pending {
    public final ViewerServiceTreeLogger branch;

    public PendingBranch(ViewerServiceTreeLogger branch, Type type, String msg,
        Throwable caught, HelpInfo helpInfo) {
      super(type, msg, caught, helpInfo);
      this.branch = branch;
    }

    @Override
    public void send() {
      sendBranch(branch, type, msg, caught, helpInfo);
    }
  }

  private class PendingLog extends Pending {
    protected final int indexOfLogEntry;

    public PendingLog(int indexOfLogEntry, Type type, String msg,
        Throwable caught, HelpInfo helpInfo) {
      super(type, msg, caught, helpInfo);
      this.indexOfLogEntry = indexOfLogEntry;
    }

    @Override
    public void send() {
      sendEntry(indexOfLogEntry, type, msg, caught, helpInfo);
    }
  }

  private volatile int logHandle = -1;

  private List<Pending> pending = Lists.create();

  private final ViewerServiceClient viewerServiceClient;

  /**
   * Creates a new instance with the given Viewer Service requestor.
   *
   * @param viewerServiceClient An object that can be used to make requests to a
   *          viewer service server.
   */
  public ViewerServiceTreeLogger(ViewerServiceClient viewerServiceClient) {
    this.viewerServiceClient = viewerServiceClient;
  }

  /**
   * Creates a new logger for a branch. Note that the logger's handle has not
   * been set as yet; it will only be set once the branch is committed.
   */
  @Override
  protected AbstractTreeLogger doBranch() {
    return new ViewerServiceTreeLogger(viewerServiceClient);
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted,
      Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    // Already synchronized via superclass.
    ViewerServiceTreeLogger child = (ViewerServiceTreeLogger) childBeingCommitted;
    if (isSent()) {
      // Immediately send the child branch.
      sendBranch(child, type, msg, caught, helpInfo);
    } else {
      // Queue the child branch until I'm committed.
      pending = Lists.add(pending, new PendingBranch(child, type, msg, caught,
          helpInfo));
    }
  }

  @Override
  protected void doLog(int indexOfLogEntry, Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {
    // Already synchronized via superclass.
    if (isSent()) {
      // Immediately send the child log entry.
      sendEntry(indexOfLogEntry, type, msg, caught, helpInfo);
    } else {
      // Queue the log entry until I'm committed.
      pending = Lists.add(pending, new PendingLog(indexOfLogEntry, type, msg,
          caught, helpInfo));
    }
  }

  synchronized void initLogHandle(int newLogHandle) {
    assert !isSent();
    logHandle = newLogHandle;
    for (Pending item : pending) {
      item.send();
    }
    pending = null;
  }

  void sendBranch(final ViewerServiceTreeLogger branch, Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {
    assert isSent();
    viewerServiceClient.addLogBranch(branch.getBranchedIndex(), type, msg,
        caught, helpInfo, logHandle, new Callback<Integer>() {
          @Override
          public void onDone(Integer result) {
            branch.initLogHandle(result);
          }

          @Override
          public void onError(Throwable t) {
            System.err.println("An error occurred while attempting to add a log branch.");
            t.printStackTrace(System.err);
          }
        });
  }

  void sendEntry(int indexOfLogEntry, Type type, String msg, Throwable caught,
      HelpInfo helpInfo) {
    assert isSent();
    viewerServiceClient.addLogEntry(indexOfLogEntry, type, msg, caught,
        helpInfo, logHandle);
  }

  private boolean isSent() {
    return logHandle >= 0;
  }
}
