/*
 * CompilePdfEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.compilepdf.events;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CompilePdfEvent extends GwtEvent<CompilePdfEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onCompilePdf(CompilePdfEvent event);
   }

   public CompilePdfEvent(FileSystemItem targetFile,
                          String rootDocument,
                          SourceLocation sourceLocation,
                          String completedAction,
                          boolean useInternalPreview)
   {
      targetFile_ = targetFile;
      rootDocument_ = rootDocument;
      sourceLocation_ = sourceLocation;
      completedAction_ = completedAction;
      useInternalPreview_ = useInternalPreview;
   }
   
   public FileSystemItem getTargetFile()
   {
      return targetFile_;
   }
   
   public String getRootDocument()
   {
      return rootDocument_;
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
   
   private final FileSystemItem targetFile_;
   private final String rootDocument_;
   private final SourceLocation sourceLocation_;
   private final String completedAction_;
   private final boolean useInternalPreview_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
