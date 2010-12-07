/*
 * DesktopActionsWidget.java
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
package org.rstudio.studio.client.workbench.views.plots.ui.impl;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.FilenameTransform;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.plots.ui.ActionsWidget;

public class DesktopActionsWidget extends ActionsWidget
{
   public DesktopActionsWidget()
   {
      FlowPanel panel = new FlowPanel();
      ThemedButton copyButton = new ThemedButton("Copy", new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            copySelectedImage();
         }
      });
      panel.add(copyButton);

      ThemedButton saveButton = new ThemedButton("Save As PNG...",
                                                 new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            RStudioGinjector.INSTANCE.getFileDialogs().saveFile(
                  "Save Plot",
                  fsContext_,
                  fsContext_.itemForName("plot.png", false, false),
                  new FilenameTransform()
                  {
                     public String transform(String filename)
                     {
                        if (filename.toLowerCase().endsWith(".png"))
                           return filename;
                        else
                           return filename + ".png";
                     }
                  },
                  new ProgressOperationWithInput<FileSystemItem>()
                  {
                     public void execute(FileSystemItem input,
                                         final ProgressIndicator indicator)
                     {
                        if (input == null)
                           return;
                        server_.exportPlot(
                              input, width_, height_,
                              new ServerRequestCallback<Void>()
                              {
                                 @Override
                                 public void onResponseReceived(Void response)
                                 {
                                    indicator.onCompleted();
                                 }

                                 @Override
                                 public void onError(ServerError error)
                                 {
                                    indicator.onError(error.getUserMessage());
                                 }
                              });
                     }
                  }
            );
         }
      });
      panel.add(saveButton);

      initWidget(panel);
   }

   private void copySelectedImage()
   {
      final WindowEx win = imagePreview_.getElement().<IFrameElementEx>cast()
            .getContentWindow();

      Document doc = win.getDocument();
      NodeList<Element> images = doc.getElementsByTagName("img");
      if (images.getLength() == 0)
         return;
      ElementEx img = images.getItem(0).cast();

      Desktop.getFrame().copyImageToClipboard(img.getClientLeft(),
                                              img.getClientTop(),
                                              img.getClientWidth(),
                                              img.getClientHeight());
   }

   @Override
   public void onPlotChanged(String plotDownloadUrl, int width, int height)
   {
      width_ = width;
      height_ = height;
   }

   @Override
   public boolean shouldPositionOnTopRight()
   {
      return true;
   }

   private int width_;
   private int height_;
   private FileSystemContext fsContext_ =
         RStudioGinjector.INSTANCE.getRemoteFileSystemContext();
}
