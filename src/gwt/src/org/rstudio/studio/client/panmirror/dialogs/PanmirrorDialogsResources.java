/*
 * PanmirrorDialogsResources.java
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


package org.rstudio.studio.client.panmirror.dialogs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface PanmirrorDialogsResources extends ClientBundle
{
   public interface Styles extends CssResource
   {
      String dialog();
      String dialogWide();
      String spaced();
      String textArea();
      String checkBox();
      String fullWidth();
      String fullWidthSelect();
      String fullWidthTable();
      String infoLabel();
      String imageDialogTabs();
      String linkDialogTabs();
      String hrefSelect();
      String popup();
      String linkLabel();
      String inlineInfoLabel();
      String horizontalLabel();
      String horizontalInput();
      String lockRatioCheckbox();
      String heightAuto();
      String langSuggestionDisplay();
      String flexTablePreview();
      String flexTablePreviewName();
      String flexTablePreviewValue();
      String disabled();
      String listBox();
   }

   @Source("PanmirrorDialogsStyles.css")
   Styles styles();
   
   @Source("break_link_2x.png")
   ImageResource break_link();
   
   @Source("edit_link_2x.png")
   ImageResource edit_link();
   
   public static PanmirrorDialogsResources INSTANCE = GWT.create(PanmirrorDialogsResources.class);
}
