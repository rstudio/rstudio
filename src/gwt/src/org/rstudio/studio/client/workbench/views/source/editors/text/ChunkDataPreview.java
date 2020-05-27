/*
 * ChunkDataPreview.java
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

import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkDataPreview extends Composite
                              implements EditorThemeListener
{
   private static ChunkDataPreviewUiBinder uiBinder = GWT
         .create(ChunkDataPreviewUiBinder.class);

   interface ChunkDataPreviewUiBinder extends UiBinder<Widget, ChunkDataPreview>
   {
   }
   
   public interface DataPreviewStyle extends CssResource
   {
      String data();
   }
   
   public ChunkDataPreview(JavaScriptObject data, NotebookFrameMetadata metadata)
   {
      data_ = new ChunkDataWidget(data, metadata, ChunkOutputSize.Default);

      initWidget(uiBinder.createAndBindUi(this));
      dataHost_.add(data_);
   }
   
   @Override
   public void onEditorThemeChanged(Colors colors)
   {
      data_.onEditorThemeChanged(colors);
   }

   @UiField SimplePanel dataHost_;
   @UiField DataPreviewStyle style;
   
   private final ChunkDataWidget data_;
}
