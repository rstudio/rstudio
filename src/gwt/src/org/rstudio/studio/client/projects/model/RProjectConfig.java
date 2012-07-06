/*
 * RProjectConfig.java
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
   
   public native static final RProjectConfig createEmpty() /*-{
      var config = new Object();
      config.version = 1.0;
      return config;
   }-*/;
   
   public native static final RProjectConfig create(int restoreWorkspace,
                                                    int saveWorkspace,
                                                    int alwaysSaveHistory,
                                                    boolean enableCodeIndexing,
                                                    boolean useSpacesForTab,
                                                    int numSpacesForTab,
                                                    String encoding,
                                                    String defaultSweaveEngine,
                                                    String defaultLatexProgram,
                                                    String rootDocument,
                                                    String buildType,
                                                    String packagePath,
                                                    String makefilePath,
                                                    String makefileArgs,
                                                    String customScriptPath) /*-{
      var config = new Object();
      config.version = 1.0;
      config.restore_workspace = restoreWorkspace;
      config.save_workspace = saveWorkspace;
      config.always_save_history = alwaysSaveHistory;
      config.enable_code_indexing = enableCodeIndexing;
      config.use_spaces_for_tab = useSpacesForTab;
      config.num_spaces_for_tab = numSpacesForTab;
      config.default_encoding = encoding;
      config.default_sweave_engine = defaultSweaveEngine;
      config.default_latex_program = defaultLatexProgram;
      config.root_document = rootDocument;
      config.build_type = buildType;
      config.package_path = packagePath;
      config.makefile_path = makefilePath;
      config.makefile_args = makefileArgs;
      config.custom_script_path = customScriptPath;
      return config;
   }-*/;

   public native final double getVersion() /*-{
      return this.version;
   }-*/;

   public native final int getRestoreWorkspace() /*-{
      return this.restore_workspace;
   }-*/;
   
   public native final void setRestoreWorkspace(int restoreWorkspace) /*-{
      this.restore_workspace = restoreWorkspace;
   }-*/;
   
   public native final int getSaveWorkspace() /*-{
      return this.save_workspace;
   }-*/;
   
   public native final void setSaveWorkspace(int saveWorkspace) /*-{
      this.save_workspace = saveWorkspace;
   }-*/;
   
   public native final int getAlwaysSaveHistory() /*-{
      return this.always_save_history;
   }-*/;   
   
   public native final void setAlwaysSaveHistory(int alwaysSaveHistory) /*-{
      this.always_save_history = alwaysSaveHistory;
   }-*/;   
   
   public native final boolean getEnableCodeIndexing() /*-{
      return this.enable_code_indexing;
   }-*/;  
   
   public native final void setEnableCodeIndexing(boolean enableCodeIndexing) /*-{
      this.enable_code_indexing = enableCodeIndexing;
   }-*/;  
  
   public native final boolean getUseSpacesForTab() /*-{
      return this.use_spaces_for_tab;
   }-*/;  
   
   public native final void setUseSpacesForTab(boolean useSpacesForTab) /*-{
      this.use_spaces_for_tab = useSpacesForTab;
   }-*/; 
   
   public native final int getNumSpacesForTab() /*-{
      return this.num_spaces_for_tab;
   }-*/;  
   
   public native final void setNumSpacesForTab(int numSpacesForTab) /*-{
      this.num_spaces_for_tab = numSpacesForTab;
   }-*/;  
   
   public native final String getEncoding() /*-{
      return this.default_encoding;
   }-*/;
   
   public native final void setEncoding(String defaultEncoding) /*-{
      this.default_encoding = defaultEncoding;
   }-*/;
   
   public native final String getDefaultSweaveEngine() /*-{
      return this.default_sweave_engine;
   }-*/;

   public native final void setDefaultSweaveEngine(String defaultSweaveEngine) /*-{
      this.default_sweave_engine = defaultSweaveEngine;
   }-*/;
   
   public native final String getDefaultLatexProgram() /*-{
      return this.default_latex_program;
   }-*/;

   public native final void setDefaultLatexProgram(String defaultLatexProgram) /*-{
      this.default_latex_program = defaultLatexProgram;
   }-*/;
   
   public native final String getRootDocument() /*-{
      return this.root_document;
   }-*/;

   public native final void setRootDocument(String rootDocument) /*-{
      this.root_document = rootDocument;
   }-*/;
   
   public static final String BUILD_TYPE_NONE = "None";
   public static final String BUILD_TYPE_PACKAGE = "Package";
   public static final String BUILD_TYPE_MAKEFILE = "Makefile";
   public static final String BUILD_TYPE_CUSTOM = "Custom";
   
   public native final String getBuildType() /*-{
      return this.build_type;
   }-*/;

   public native final void setBuildType(String buildType) /*-{
      this.build_type = buildType;
   }-*/;
   
   public native final String getPackagePath() /*-{
      return this.package_path;
   }-*/;

   public native final void setPackagePath(String packagePath) /*-{
      this.package_path = packagePath;
   }-*/;
   
   public native final String getMakefilePath() /*-{
      return this.makefile_path;
   }-*/;

   public native final void setMakefilePath(String makefilePath) /*-{
      this.makefile_path = makefilePath;
   }-*/;
   
   public native final String getCustomScriptPath() /*-{
      return this.custom_script_path;
   }-*/;

   public native final void setCustomScriptPath(String customScriptPath) /*-{
      this.custom_script_path = customScriptPath;
   }-*/;
   
}
