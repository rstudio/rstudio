/*
 * CompilePdfEvent.java
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
package org.rstudio.studio.client.workbench.views.output.compilepdf.events;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class CompilePdfEvent extends CrossWindowEvent<CompilePdfEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onCompilePdf(CompilePdfEvent event);
   }

   public CompilePdfEvent()
   {
   }

   public CompilePdfEvent(FileSystemItem targetFile,
                          String encoding,
                          SourceLocation sourceLocation,
                          String completedAction,
                          boolean useInternalPreview)
   {
      targetFile_ = targetFile;
      encoding_ = encoding;
      sourceLocation_ = sourceLocation;
      completedAction_ = completedAction;
      useInternalPreview_ = useInternalPreview;
   }

   public FileSystemItem getTargetFile()
   {
      return targetFile_;
   }

   public String getEncoding()
   {
      return encoding_;
   }

   public SourceLocation getSourceLocation()
   {
      return sourceLocation_;
   }

   public String getCompletedAction()
   {
      return completedAction_;
   }

   public boolean useInternalPreview()
   {
      return useInternalPreview_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onCompilePdf(this);
   }

   private FileSystemItem targetFile_;
   private String encoding_;
   private SourceLocation sourceLocation_;
   private String completedAction_;
   private boolean useInternalPreview_;

   public static final Type<Handler> TYPE = new Type<>();
}
