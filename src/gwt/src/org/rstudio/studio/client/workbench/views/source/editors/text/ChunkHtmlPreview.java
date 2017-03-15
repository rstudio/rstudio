/*
 * ChunkHtmlPreview.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ChunkHtmlPreview extends Composite
{

   private static ChunkHtmlPreviewUiBinder uiBinder = GWT
         .create(ChunkHtmlPreviewUiBinder.class);
   
   public interface Resources extends ClientBundle
   {
      @Source("HtmlWidgetIcon_2x.png")
      ImageResource htmlWidgetIcon2x();
   }

   interface ChunkHtmlPreviewUiBinder extends UiBinder<Widget, ChunkHtmlPreview>
   {
   }

   public ChunkHtmlPreview()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
}
