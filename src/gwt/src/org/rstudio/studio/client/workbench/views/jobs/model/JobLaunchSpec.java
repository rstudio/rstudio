/*
 * JobLaunchSpec.java
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

import com.google.gwt.core.client.JavaScriptObject;

public class JobLaunchSpec extends JavaScriptObject
{
   protected JobLaunchSpec() {}
   
   public final native String path() /*-{
      return this.path;
   }-*/;
   
   public final native String code() /*-{
      return this.code;
   }-*/;
   
   public final native String encoding() /*-{
      return this.encoding;
   }-*/;
   
   public final native void setEncoding(String encoding) /*-{
      this.encoding = encoding;
   }-*/;
   
   public final native String workingDir() /*-{
      return this.working_dir;
   }-*/;
   
   public final native boolean importEnv() /*-{
      return this.import_env;
   }-*/;
   
   public final native String exportEnv() /*-{
      return this.export_env;
   }-*/;
   
   public final native static JobLaunchSpec create(
      String name,
      String path,
      String encoding,
      String workingDir,
      boolean importEnv,
      String exportEnv) /*-{
      return { "name": name,
               "path": path,
               "code": "",
               "encoding": encoding,
               "working_dir": workingDir,
               "import_env": importEnv,
               "export_env": exportEnv };
   }-*/;

   public final native static JobLaunchSpec create(
      String name,
      String code,
      String workingDir,
      boolean importEnv,
      String exportEnv) /*-{
      return { "name": name,
               "code": code,
               "path": "",
               "encoding": "",
               "working_dir": workingDir,
               "import_env": importEnv,
               "export_env": exportEnv };
   }-*/;
}
