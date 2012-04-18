/*
 * TerminalOptions.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;

public class TerminalOptions extends JavaScriptObject
{
   protected TerminalOptions() {}
 
   public native final String getTerminalPath() /*-{
      return this.terminal_path;
   }-*/;
   
   public native final String getWorkingDirectory() /*-{
      return this.working_directory;
   }-*/;
   
   public native final String getExtraPathEntries() /*-{
      return this.extra_path_entries;
   }-*/;
}
