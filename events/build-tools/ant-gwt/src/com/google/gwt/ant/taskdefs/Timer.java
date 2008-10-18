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
package com.google.gwt.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

import java.util.Vector;

/**
 * A timer task for ant, which reports the total time to execute the contained
 * tasks.  It reports both a millisecond total and hour:minute:second.fraction
 * formats.
 */
public class Timer extends Task implements TaskContainer {

  private static final int MS_IN_HOUR = 60 * 60 * 1000;
  private static final int MS_IN_MINUTE =  60 * 1000;
  private static final int MS_IN_SECOND =  1000;

  private Vector<Task> nested;
  private String name;

  public Timer() {
    super();
    nested = new Vector<Task>();
    name = "";
  }

  public void addTask(Task newtask) {
    nested.addElement(newtask);
  }

  public void execute() throws BuildException {
    long start = System.currentTimeMillis();
    for (Task task : nested) {
      task.perform();
    }

    long durationMsec = (System.currentTimeMillis() - start);
    long duration = durationMsec; 
    long hrs = duration / MS_IN_HOUR;
    duration -= hrs * MS_IN_HOUR;
    long min = duration  / MS_IN_MINUTE;
    duration -= min * MS_IN_MINUTE;
    long sec = duration / MS_IN_SECOND;
    duration -= sec * MS_IN_SECOND;
    long msec = duration;

    log("timer " + name + " timed " + durationMsec + " ms. " +
        String.format("(%02d:%02d:%02d.%03d)", hrs, min, sec, msec)); 
  }

  public void setName(String newname) {
    name = newname;
  }
}
