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
package com.google.gwt.user.client;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;

/**
 * An <code>IncrementalCommand</code> is a command that is broken into one or
 * more substeps, each substep brings the whole command nearer to completion.
 * The command is complete when <code>execute()</code> returns
 * <code>false</code>.
 * 
 * {@example com.google.gwt.examples.IncrementalCommandExample}
 * 
 * @deprecated Replaced by {@link RepeatingCommand} and
 *             {@link com.google.gwt.core.client.Scheduler#scheduleIncremental
 *             Scheduler.scheduleIncremental()}
 */
@Deprecated
public interface IncrementalCommand extends RepeatingCommand {
  /**
   * Causes the <code>IncrementalCommand</code> to execute its encapsulated
   * behavior.
   * 
   * @return <code>true</code> if the command has more work to do,
   *         <code>false</code> otherwise
   */
  boolean execute();
}
