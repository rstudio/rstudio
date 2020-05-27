/*
 * SourceFileSavedEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

/**
 * Fired when an explicit "Save As", or Save of a previously unsaved file,
 * occurs. Does NOT fire when a Save occurs on a previously saved file.
 */
@JavaScriptSerializable
public class SourceFileSavedEvent
             extends CrossWindowEvent<SourceFileSavedHandler>
{
   public static final Type<SourceFileSavedHandler> TYPE = 
         new Type<SourceFileSavedHandler>();
   
   public SourceFileSavedEvent()
   {
   }

   public SourceFileSavedEvent(String docId, String path)
   {
      docId_ = docId;
      path_ = path;
   }

   public String getPath()
   {
      return path_;
   }
   
   public String getDocId()
   {
      return docId_;
   }

   @Override
   public Type<SourceFileSavedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(SourceFileSavedHandler handler)
   {
      handler.onSourceFileSaved(this);
   }

   private String path_;
   private String docId_;
}
