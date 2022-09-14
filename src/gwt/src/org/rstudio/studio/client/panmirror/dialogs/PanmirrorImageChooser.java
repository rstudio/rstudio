
/*
 * PanmirrorImageChooser.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.panmirror.dialogs;

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds.TextBoxButtonId;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIContext;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public class PanmirrorImageChooser extends TextBoxWithButton {

   
   public PanmirrorImageChooser(PanmirrorUIContext uiContext, RMarkdownServerOperations server)
   {
      super(constants_.imageChooserLabel(), "", constants_.browseLabel(), null, TextBoxButtonId.CHOOSE_IMAGE, false, null);
      PanmirrorDialogsUtil.setFullWidthStyles(this);
      disableSpellcheck();
      
      addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            FileSystemItem defaultDir = FileSystemItem.createDir(uiContext.getDefaultResourceDir.get());
            
            // use previously selected directory for this defaultDir if we have one
            FileSystemItem initialDir = FileSystemItem.createDir(
               previousImageDirs_.getOrDefault(defaultDir.getPath(), defaultDir.getPath())
            );
               
            RStudioGinjector.INSTANCE.getFileDialogs().openFile(
               constants_.chooseImageCaption(),
               RStudioGinjector.INSTANCE.getRemoteFileSystemContext(),
               initialDir,
               new ProgressOperationWithInput<FileSystemItem>()
               {
                  public void execute(FileSystemItem input,
                                      ProgressIndicator indicator)
                  {
                     if (input == null)
                        return;
                     
                     // save mapping of defaultDir to imageDir
                     previousImageDirs_.put(defaultDir.getPath(), input.getParentPathString());

                     // compute relative path
                     String mappedPath = uiContext.mapPathToResource.map(input.getPath());
                     if (mappedPath != null) 
                     {
                        setText(mappedPath);
                        indicator.onCompleted();
                     }
                     else
                     {
                        JsArrayString images = JsArrayString.createArray().cast();
                        images.push(input.getPath());
                        FileSystemItem defaultDir = FileSystemItem.createDir(uiContext.getDefaultResourceDir.get());
                        String imagesDir = defaultDir.completePath("images");
                        server.rmdImportImages(images, imagesDir, new SimpleRequestCallback<JsArrayString>() {
                           @Override
                           public void onResponseReceived(JsArrayString resolvedImages)
                           {
                              if (resolvedImages.length() > 0)
                              {
                                 String mappedPath = uiContext.mapPathToResource.map(resolvedImages.get(0));
                                 setText(mappedPath);
                              }
                              indicator.onCompleted();
                           }
                        });
                     }

                  }
               });
         }
      });
   }
   
   private static HashMap<String,String> previousImageDirs_ = new HashMap<String,String>();
   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);
}
