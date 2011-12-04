/*
 * DiffFrame.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTableView;

public class DiffFrame extends Composite
{
   interface Resources extends ClientBundle
   {
      @Source("DiffFrame.css")
      Styles styles();

      @Source("images/diffHeaderTile.png")
      @ImageOptions(repeatStyle = ImageResource.RepeatStyle.Horizontal)
      ImageResource diffHeaderTile();
   }

   interface Styles extends CssResource
   {
      String header();
      String fileIcon();
      String headerLabel();
      String viewFilePanel();
      String viewFileSeparator();
      String viewFileHyperlink();
   }

   interface Binder extends UiBinder<Widget, DiffFrame>
   {}

   public DiffFrame(ImageResource icon,
                    String filename1,
                    String filename2,
                    String commitId,
                    LineTableView diff)
   {
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      fileIcon_.setResource(
            RStudioGinjector.INSTANCE.getFileTypeRegistry().getIconForFile(
                  FileSystemItem.createFile(filename2 == null ? filename1 : filename2)));

      headerLabel_.setText(filename1);
      
      separatorImage_.setResource(ThemeResources.INSTANCE.toolbarSeparator());
      separatorImage_.addStyleName(RES.styles().viewFileSeparator());
      
      viewFileHyperlink_.setAlwaysUnderline(false);
      viewFileHyperlink_.setText("View file @ " + commitId);
      viewFileHyperlink_.addStyleName(RES.styles().viewFileHyperlink());
      
      container_.add(diff);
   }

   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   @UiField
   FlowPanel container_;
   @UiField
   Label headerLabel_;
   @UiField
   Image fileIcon_;
   @UiField
   Image separatorImage_;
   @UiField
   HyperlinkLabel viewFileHyperlink_;
   
   private static final Resources RES = GWT.<Resources>create(Resources.class);
}
