/*
 * NewProjectContext.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.model;

import org.rstudio.studio.client.quarto.model.QuartoCapabilities;

import com.google.gwt.core.client.JavaScriptObject;

public class NewProjectContext extends JavaScriptObject
{
   protected NewProjectContext()
   {
   }
   
   public native final boolean isRcppAvailable() /*-{
      return this.rcpp_available;
   }-*/;
   
   public native final boolean isPackratAvailable() /*-{
      return this.packrat_available;
   }-*/;
   
   public native final QuartoCapabilities getQuartoCapabilities() /*-{
      return this.quarto_capabilities;
   }-*/;
   
   public native final String getWorkingDirectory() /*-{
      return this.working_directory;
   }-*/;
}
