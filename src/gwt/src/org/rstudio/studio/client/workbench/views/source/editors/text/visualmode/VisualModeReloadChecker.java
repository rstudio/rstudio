/*
 * VisualModeReloadState.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.studio.client.panmirror.uitools.PanmirrorPandocFormatConfig;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;


// class that tracks whether the format config we started editing with has changed
// such that we require a reload of the editor (e.g. markdown extensions changed)

public class VisualModeReloadChecker
{
   public VisualModeReloadChecker(TextEditingTarget.Display view)
   {
      view_ = view;
      formatTools_ = new PanmirrorUITools().format;
      config_ = formatTools_.parseFormatConfig(getEditorCode(), true);
   }
   
   public boolean requiresReload()
   {
      PanmirrorPandocFormatConfig config = formatTools_.parseFormatConfig(getEditorCode(), true);
      return !PanmirrorPandocFormatConfig.editorBehaviorConfigEqual(config,  config_);  
   }
   
   private String getEditorCode()
   {
      return VisualModeUtil.getEditorCode(view_);
   }
   
   
   private final PanmirrorUIToolsFormat formatTools_;
   private final PanmirrorPandocFormatConfig config_;
   
   private final TextEditingTarget.Display view_;
}


