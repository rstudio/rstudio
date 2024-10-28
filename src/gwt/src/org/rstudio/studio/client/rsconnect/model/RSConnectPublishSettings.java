/*
 * RSConnectPublishSettings.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.rsconnect.model;

import java.util.List;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;

import com.google.gwt.core.client.JavaScriptObject;

public class RSConnectPublishSettings
{
   public RSConnectPublishSettings(List<String> deployFiles, 
                                   List<String> additionalFiles, 
                                   List<String> ignoredFiles,
                                   List<String> envVars,
                                   boolean asMultiple,
                                   boolean asStatic)
   {
      deployFiles_ = deployFiles;
      additionalFiles_ = additionalFiles;
      ignoredFiles_ = ignoredFiles;
      envVars_ = envVars;
      asMultiple_ = asMultiple;
      asStatic_ = asStatic;
   }

   public RSConnectPublishSettings(List<String> deployFiles)
   {
      this(deployFiles, null, null, null, false, true);
   }
   
   public List<String> getDeployFiles()
   {
      return deployFiles_;
   }

   public List<String> getAdditionalFiles()
   {
      return additionalFiles_;
   }

   public List<String> getIgnoredFiles()
   {
      return ignoredFiles_;
   }
   
   public List<String> getEnvVars()
   {
      return envVars_;
   }
   
   public boolean getAsMultiple()
   {
      return asMultiple_;
   }

   public boolean getAsStatic()
   {
      return asStatic_;
   }
   
   public JavaScriptObject toJso()
   {
      JsObject obj = JsObject.createJsObject();
      obj.setJsArrayString("deploy_files", 
            JsArrayUtil.toJsArrayString(getDeployFiles()));
      obj.setJsArrayString("additional_files", 
            JsArrayUtil.toJsArrayString(getAdditionalFiles()));
      obj.setJsArrayString("ignored_files", 
            JsArrayUtil.toJsArrayString(getIgnoredFiles()));
      obj.setJsArrayString("env_vars",
            JsUtil.toJsArrayString(envVars_));
      obj.setBoolean("as_multiple", getAsMultiple());
      obj.setBoolean("as_static", getAsStatic());
      return obj.cast();
   }

   private final List<String> deployFiles_;
   private final List<String> additionalFiles_;
   private final List<String> ignoredFiles_;
   private final List<String> envVars_;
   private final boolean asMultiple_;
   private final boolean asStatic_;
}
