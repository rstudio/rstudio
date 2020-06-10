/*
 * Job.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.jobs.model;

import jsinterop.annotations.JsType;

import com.google.gwt.core.client.JsArrayString;

import jsinterop.annotations.JsPackage;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class Job
{
   // the job's unique ID 
   public String id;
   
   // the job's name; not necessarily unique
   public String name;
   
   // the current status of the job
   public String status;
   
   // the job's state (idle, running, completed, etc.)
   public int state;

   // the job's type (local session, launcher)
   public int type;
   
   // launcher cluster (empty for local session jobs)
   public String cluster;
   
   // the number of progress units the job has completed so far
   public int progress;
   
   // the total number of progress units to be completed
   public int max;
   
   // the time the job was recorded in the system
   public int recorded;
   
   // the time the job started execution
   public int started;
   
   // the time the job was completed 
   public int completed;
   
   // the time the job has spent running so far
   public int elapsed;
   
   // the time the browser (client) received the job
   public int received;
   
   // the job's actions
   public JsArrayString actions;

   // whether the job pane should should be shown at start
   public boolean show;
   
   // whether the job should persist its output
   public boolean saveoutput;

   // the job's tags
   public JsArrayString tags;
}
