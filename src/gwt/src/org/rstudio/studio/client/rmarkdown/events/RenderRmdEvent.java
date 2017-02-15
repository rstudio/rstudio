/*
 * RenderRmdEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

package org.rstudio.studio.client.rmarkdown.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class RenderRmdEvent extends CrossWindowEvent<RenderRmdEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      void onRenderRmd(RenderRmdEvent event);
   }
   
   public RenderRmdEvent()
   {
   }

   public RenderRmdEvent(String sourceFile, 
                         int sourceLine,
                         String format,
                         String encoding,
                         String paramsFile,
                         boolean asTempfile,
                         int type,
                         String existingOutputFile,
                         String workingDirectory,
                         String viewerType)
   {
      sourceFile_ = sourceFile;
      sourceLine_ = sourceLine;
      format_ = format;
      encoding_ = encoding;
      paramsFile_ = paramsFile;
      asTempfile_ = asTempfile;
      type_ = type;
      existingOutputFile_ = existingOutputFile;
      workingDirectory_ = workingDirectory;
      viewerType_ = viewerType;
   }

   public String getSourceFile()
   {
      return sourceFile_;
   }
   
   public int getSourceLine()
   {
      return sourceLine_;
   }
   
   public String getFormat()
   {
      return format_;
   }
   
   public String getEncoding()
   {
      return encoding_;
   }
   
   public String getParamsFile()
   {
      return paramsFile_;
   }
   
   public boolean asTempfile()
   {
      return asTempfile_;
   }
    
   public int getType()
   {
      return type_;
   }
   
   public String getExistingOutputFile()
   {
      return existingOutputFile_;
   }
   
   public String getWorkingDir()
   {
      return workingDirectory_;
   }
   
   public String getViewerType()
   {
      return viewerType_;
   }
    
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRenderRmd(this);
   }
   
   private String sourceFile_;
   private int sourceLine_;
   private String format_;
   private String encoding_;
   private String paramsFile_;
   private boolean asTempfile_;
   private int type_;
   private String existingOutputFile_;
   private String workingDirectory_;
   private String viewerType_;
   
   public final static String WORKING_DIR_PROP = "working_dir";

   public static final Type<Handler> TYPE = new Type<Handler>();
}