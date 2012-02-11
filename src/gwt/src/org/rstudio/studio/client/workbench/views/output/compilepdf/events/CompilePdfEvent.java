/*
 * CompilePdfEvent.java
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
package org.rstudio.studio.client.workbench.views.output.compilepdf.events;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CompilePdfEvent extends GwtEvent<CompilePdfEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onCompilePdf(CompilePdfEvent event);
   }

   public CompilePdfEvent(FileSystemItem targetFile, String completedAction)
   {
      targetFile_ = targetFile;
      completedAction_ = completedAction;
   }
   
   public FileSystemItem getTargetFile()
   {
      return targetFile_;
   }
   
   public String getCompletedAction()
   {
      return completedAction_;
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
   private String completedAction_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
