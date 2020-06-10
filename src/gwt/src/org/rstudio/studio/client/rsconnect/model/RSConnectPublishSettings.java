/*
 * RSConnectPublishSettings.java
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
package org.rstudio.studio.client.rsconnect.model;

import java.util.ArrayList;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JavaScriptObject;

public class RSConnectPublishSettings
{
   public RSConnectPublishSettings(ArrayList<String> deployFiles, 
         ArrayList<String> additionalFiles, 
         ArrayList<String> ignoredFiles,
         boolean asMultiple,
         boolean asStatic)
   {
      deployFiles_ = deployFiles;
      additionalFiles_ = additionalFiles;
      ignoredFiles_ = ignoredFiles;
      asMultiple_ = asMultiple;
      asStatic_ = asStatic;
   }

   public ArrayList<String> getDeployFiles()
   {
      return deployFiles_;
   }

   public ArrayList<String> getAdditionalFiles()
   {
      return additionalFiles_;
   }

   public ArrayList<String> getIgnoredFiles()
   {
      return ignoredFiles_;
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
      obj.setBoolean("as_multiple", getAsMultiple());
      obj.setBoolean("as_static", getAsStatic());
      return obj.cast();
   }

   private final ArrayList<String> deployFiles_;
   private final ArrayList<String> additionalFiles_;
   private final ArrayList<String> ignoredFiles_;
   private final boolean asMultiple_;
   private final boolean asStatic_;
}
