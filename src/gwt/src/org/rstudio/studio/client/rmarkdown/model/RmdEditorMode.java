/*
 * RmdEditorMode.java
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

package org.rstudio.studio.client.rmarkdown.model;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.quarto.QuartoHelper;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class RmdEditorMode
{
   public final static String VISUAL = "visual";
   public final static String SOURCE = "source";
   
   public static String getEditorMode(String path, String yaml, SessionInfo sessionInfo)
   {
      String docMode = RmdEditorMode.getDocumentEditorMode(yaml);
      if (docMode != null)
         return docMode;
      
      String projectMode = RmdEditorMode.getProjectEditorMode(path, sessionInfo);
      if (projectMode != null)
         return projectMode;
      
      return null;
   }
   
   public static String getProjectEditorMode(String path, SessionInfo sessionInfo)
   {
      QuartoConfig config = sessionInfo.getQuartoConfig();
      String editor = config.project_editor != null ? config.project_editor.mode : null;
      if (StringUtil.isNullOrEmpty(editor))
         editor = null;
      
      if (path != null)
      {
         if (QuartoHelper.isWithinQuartoProjectDir(path, config))
            return asEditorMode(editor);
         else
            return null;
      }
      else
      {
         return editor;
      }
   }
   
   public static String getDocumentEditorMode(String yaml)
   {
      try
      {  
         if (yaml != null)
         {
            YamlTree yamlTree = new YamlTree(yaml);
            String editor = yamlTree.getChildValue("editor", "type");
            if (!StringUtil.isNullOrEmpty(asEditorMode(editor)))
            {
               return editor.trim();
            } 
            editor = yamlTree.getChildValue("editor", "mode");
            if (!StringUtil.isNullOrEmpty(asEditorMode(editor)))
            {
               return editor.trim();
            } 
            editor = yamlTree.getKeyValue("editor");
            if (!StringUtil.isNullOrEmpty(asEditorMode(editor)))
            {
               return editor.trim();
            }  
           
         }
      }
      catch(Exception ex)
      {
         Debug.logException(ex);
      }
      
      return null;
   }
   
   private static String asEditorMode(String mode)
   {
      if (mode == VISUAL || mode == SOURCE)
         return mode;
      else
         return null;
   }
}
