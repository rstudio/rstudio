/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.codegen.server;

import java.io.PrintWriter;

/**
 * Wrapper for a {@link PrintWriter} that adds the ability to abort creation
 * and an onClose hook
 * <p>
 * Experimental API - subject to change.
 */
public class AbortablePrintWriter extends PrintWriter {

  private boolean isClosed = false;

  /**
   * Wrap a {@link PrintWriter} instance.
   * 
   * @param pw
   * @throws RuntimeException if there are reflection errors accessing the out
   *     field in pw
   */
  public AbortablePrintWriter(PrintWriter pw) {
    super(pw);
  }

  /**
   * Abort creation of this output.
   */
  public void abort() {
    if (!isClosed) {
      flush();
      super.close();
      isClosed = true;
      onClose(true);
    }
  }

  @Override
  public void close() {
    if (!isClosed) {
      flush();
      super.close();
      isClosed = true;
      onClose(false);
    }
  }

  /**
   * Called exactly once when this {@link PrintWriter} is closed or aborted.
   * 
   * @param aborted
   */
  protected void onClose(boolean aborted) {
    // Do nothing by default.
  }
}
