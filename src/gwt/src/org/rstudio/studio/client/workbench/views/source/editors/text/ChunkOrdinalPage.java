/*
 * ChunkOrdinalWidget.java
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

import com.google.gwt.user.client.ui.Widget;

public class ChunkOrdinalPage extends ChunkOutputPage
{
   public ChunkOrdinalPage(int ordinal)
   {
      super(ordinal);
      thumbnail_ = new ChunkOrdinalWidget();
   }

   @Override
   public Widget thumbnailWidget()
   {
      return thumbnail_;
   }

   @Override
   public Widget contentWidget()
   {
      // ordinals have no content
      return null;
   }

   @Override
   public void onSelected()
   {
      // these are not selectable
   }
   
   private final Widget thumbnail_;
}
