/*
 * ChunkConsolePreview.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.widget.PreWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ChunkConsolePreview extends Composite
{
   private static ChunkConsolePreviewUiBinder uiBinder = GWT
         .create(ChunkConsolePreviewUiBinder.class);

   interface ChunkConsolePreviewUiBinder
         extends UiBinder<Widget, ChunkConsolePreview>
   {
   }

   public ChunkConsolePreview()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }

   public void addText(String text)
   {
      // short circuit -- this is called frequently so we cache the flag
      if (full_)
         return;
      
      // don't add any text if we've already filled the thumbnail
      if (console_.getOffsetHeight() > console_.getParent().getOffsetHeight())
      {
         full_ = true;
         return;
      }
      
      console_.appendText(text);
   }
   
   @UiField PreWidget console_;

   private boolean full_ = false;
}
