/*
 * RSConnectPublishInput.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

package org.rstudio.studio.client.rsconnect.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;

public class RSConnectPublishInput
{
   public RSConnectPublishInput(RSConnectActionEvent originatingEvent)
   {
      originatingEvent_ = originatingEvent;
   }
   
   public boolean isShiny() 
   {
      return isShiny_;
   }
   
   public void setIsShiny(boolean isShiny) 
   {
      isShiny_ = isShiny;
   }
   
   public FileSystemItem getSourceRmd()
   {
      return sourceRmd_;
   }
   
   public void setSourceRmd(FileSystemItem sourceRmd)
   {
      sourceRmd_ = sourceRmd;
   }
   
   public boolean isConnectUIEnabled() 
   {
      return isConnectUIEnabled_;
   }
   
   public void setConnectUIEnabled(boolean enabled)
   {
      isConnectUIEnabled_ = enabled;
   }
   
   public boolean isExternalUIEnabled()
   {
      return isExternalUIEnabled_;
   }
   
   public void setExternalUIEnabled(boolean enabled)
   {
      isExternalUIEnabled_ = enabled;
   }
   
   public boolean isMultiRmd()
   {
      return isMultiRmd_;
   }
   
   public void setIsMultiRmd(boolean isMulti)
   {
      isMultiRmd_ = isMulti;
   }
   
   public boolean isSelfContained()
   {
      return isSelfContained_;
   }
   
   public void setIsSelfContained(boolean isSelfContained)
   {
      isSelfContained_ = isSelfContained;
   }
   
   public int getContentType()
   {
      return getOriginatingEvent().getContentType();
   }
   
   public RSConnectActionEvent getOriginatingEvent()
   {
      return originatingEvent_;
   }
   
   private boolean isShiny_;
   private boolean isConnectUIEnabled_;
   private boolean isExternalUIEnabled_;
   private boolean isMultiRmd_;
   private boolean isSelfContained_;
   private FileSystemItem sourceRmd_;
   private RSConnectActionEvent originatingEvent_;
}
