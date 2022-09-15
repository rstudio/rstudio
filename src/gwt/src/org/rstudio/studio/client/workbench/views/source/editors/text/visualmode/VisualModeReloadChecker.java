/*
 * VisualModeReloadState.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.panmirror.format.PanmirrorFormat;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;


// class that tracks whether the format config we started editing with has changed
// such that we require a reload of the editor (e.g. markdown extensions changed)

public class VisualModeReloadChecker
{
   public VisualModeReloadChecker(PanmirrorWidget.FormatSource formatSource)
   {
      formatTools_ = new PanmirrorUITools().format;
      formatSource_ = formatSource;
      format_ = formatSource_.getFormat(formatTools_);  
   }
   
   public boolean requiresReload()
   {
      PanmirrorFormat format = formatSource_.getFormat(formatTools_);
      return !PanmirrorFormat.areEqual(format,  format_);  
   }
      
   private final PanmirrorUIToolsFormat formatTools_;
   private final PanmirrorWidget.FormatSource formatSource_;
   private final PanmirrorFormat format_;
}


