/*
 * RProjectPackratOptions.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RProjectPackratOptions extends JavaScriptObject
{
   protected RProjectPackratOptions()
   {
   }
   
   public native static final RProjectPackratOptions createEmpty() /*-{
      var options = new Object();
      options.use_packrat = false;
      options.auto_snapshot = true;
      options.vcs_ignore_lib = true;
      options.vcs_ignore_src = false;
      return options;
   }-*/;
   
   public native final boolean getUsePackrat() /*-{
      return this.use_packrat;
   }-*/;
   
   public native final void setUsePackrat(boolean usePackrat) /*-{
      this.use_packrat = usePackrat;
   }-*/;
   
   public native final boolean getAutoSnapshot() /*-{
      return this.auto_snapshot;
   }-*/;
   
   public native final void setAutoSnapshot(boolean autoSnapshot) /*-{
      this.auto_snapshot = autoSnapshot;
   }-*/;
   
   public native final boolean getVcsIgnoreLib() /*-{
      return this.vcs_ignore_lib;
   }-*/;  
   
   public native final void setVcsIgnoreLib(boolean ignoreLib) /*-{
      this.vcs_ignore_lib = ignoreLib;
   }-*/;

   public native final boolean getVcsIgnoreSrc() /*-{
      return this.vcs_ignore_src;
   }-*/;  
   
   public native final void setVcsIgnoreSrc(boolean ignoreSrc) /*-{
      this.vcs_ignore_src = ignoreSrc;
   }-*/;
}
