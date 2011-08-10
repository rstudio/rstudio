/*
 * RProjectConfig.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class RProjectConfig extends JavaScriptObject
{
   public static final int DEFAULT_VALUE = 0;
   public static final int YES_VALUE = 1;
   public static final int NO_VALUE = 2;
   public static final int ASK_VALUE = 3;
   
   protected RProjectConfig()
   {
   }
   
   public native static final RProjectConfig create(int restoreWorkspace,
                                                    int saveWorkspace,
                                                    int alwaysSaveHistory,
                                                    boolean useSpacesForTab,
                                                    int numSpacesForTab,
                                                    String encoding) /*-{
      var config = new Object();
      config.version = 1.0;
      config.restore_workspace = restoreWorkspace;
      config.save_workspace = saveWorkspace;
      config.always_save_history = alwaysSaveHistory;
      config.use_spaces_for_tab = useSpacesForTab;
      config.num_spaces_for_tab = numSpacesForTab;
      config.encoding = encoding;
      return config;
   }-*/;

   public native final double getVersion() /*-{
      return this.version;
   }-*/;

   public native final int getRestoreWorkspace() /*-{
      return this.restore_workspace;
   }-*/;
   
   public native final int getSaveWorkspace() /*-{
      return this.save_workspace;
   }-*/;
   
   public native final int getAlwaysSaveHistory() /*-{
      return this.always_save_history;
   }-*/;   
   
   public native final boolean getUseSpacesForTab() /*-{
      return this.use_spaces_for_tab;
   }-*/;  
   
   public native final int getNumSpacesForTab() /*-{
      return this.num_spaces_for_tab;
   }-*/;  
   
   public native final String getEncoding() /*-{
      return this.default_encoding;
   }-*/;
}
