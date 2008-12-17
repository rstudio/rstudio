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
package com.google.gwt.user.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.AsyncProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * The base implementation for AsyncProxy instances.
 * 
 * @param <T> the type to be returned from the GWT.create call
 */
public abstract class AsyncProxyBase<T> implements AsyncProxy<T> {
  /**
   * Simple parameterized command type.
   * 
   * @param <T> as above
   */
  protected interface ParamCommand<T> {
    void execute(T instance);
  }

  /*
   * These fields are package-protected to allow for easier testing.
   */
  private List<ParamCommand<T>> commands = new ArrayList<ParamCommand<T>>();
  private boolean hasAsyncBeenIssued = false;
  private boolean hasAsyncFailed = false;
  private boolean hasAsyncReturned = false;
  private T instance = null;

  /**
   * Testing method to enable enqueue0 from firing the runAsync call.
   */
  public void enableLoadForTest0() {
    hasAsyncBeenIssued = false;
    hasAsyncFailed = false;
    hasAsyncReturned = false;
  }

  public final T getProxiedInstance() {
    return instance;
  }

  /**
   * To be implemented by the subtype.
   */
  public abstract void setProxyCallback(ProxyCallback<T> callback);

  /**
   * Testing method to prevent enqueue0 from actually firing the runAsync call.
   */
  public void suppressLoadForTest0() {
    hasAsyncBeenIssued = true;
    hasAsyncFailed = false;
    hasAsyncReturned = false;
  }

  /**
   * To be implemented by the subtype to give the code-splitter a new starting
   * point.
   */
  protected abstract void doAsync0();

  /**
   * Called by the generated subtype if the runAsync invocation fails.
   */
  protected final void doFailure0(Throwable t) {
    hasAsyncFailed = true;
    getCallback0().onFailure(t);
  }

  /**
   * Called by the generated subtype to enqueue an action for later replay.
   */
  protected final void enqueue0(ParamCommand<T> cmd) {
    try {
      if (hasAsyncFailed) {
        throw new IllegalStateException("runAsync load previously failed");

      } else if (!hasAsyncBeenIssued) {
        hasAsyncBeenIssued = true;
        commands.add(cmd);
        doAsync0();

      } else if (hasAsyncReturned) {
        assert instance != null;
        cmd.execute(instance);

      } else {
        assert instance == null;
        commands.add(cmd);
      }
    } catch (Throwable t) {
      if (getCallback0() != null) {
        getCallback0().onFailure(t);
      } else {
        GWT.getUncaughtExceptionHandler().onUncaughtException(t);
      }
    }
  }

  /**
   * The callback is maintained by the generated subtype to take advantage of
   * type-tightening.
   */
  protected abstract ProxyCallback<T> getCallback0();

  /**
   * Called by the generated subtype with the new instance of the object.
   */
  protected final void setInstance0(T instance) {
    assert commands != null : "No commands";
    assert hasAsyncBeenIssued : "async request not yet started";
    hasAsyncReturned = true;
    this.instance = instance;
    List<ParamCommand<T>> localCommands = commands;
    commands = null;

    // Can fail below
    if (getCallback0() != null) {
      getCallback0().onInit(instance);
    }
    for (ParamCommand<T> cmd : localCommands) {
      cmd.execute(instance);
    }
    if (getCallback0() != null) {
      getCallback0().onComplete(instance);
    }
  }
}
