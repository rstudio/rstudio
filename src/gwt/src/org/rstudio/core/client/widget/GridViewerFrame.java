/*
 * GridViewer.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.core.client.widget;

import org.rstudio.core.client.dom.WindowEx;

public class GridViewerFrame extends RStudioFrame
{
   private final String id_;
   
   public GridViewerFrame(String id)
   {
      super("grid_resource/gridviewer.html?data_source=callback&id=" + id);
      id_ = id;
      
      setSize("100%", "100%");
   }
   
   public void onAttach()
   {
      super.onAttach();
   }
   
   public String getId()
   {
      return id_;
   }
}
