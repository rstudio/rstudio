/*
 * RSConnectPublishInput.java
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

package org.rstudio.studio.client.rsconnect.model;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.rsconnect.RSConnect;
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
   
   public boolean hasConnectAccount()
   {
      return hasConnectAccount_;
   }
   
   public void setHasConnectAccount(boolean hasAccount)
   {
      hasConnectAccount_ = hasAccount;
   }
   
   public void setConnectUIEnabled(boolean enabled)
   {
      isConnectUIEnabled_ = enabled;
   }
   
   public boolean isExternalUIEnabled()
   {
      return isExternalUIEnabled_;
   }
   
   public boolean hasDocOutput()
   {
      return originatingEvent_ != null &&
            originatingEvent_.getFromPreview() != null &&
            !StringUtil.isNullOrEmpty(
                  originatingEvent_.getFromPreview().getOutputFile());
   }
   
   public String getDocOutput()
   {
      if (!hasDocOutput())
         return null;
      return originatingEvent_.getFromPreview().getOutputFile();
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
   
   public void setIsSelfContained(boolean selfContained)
   {
      isSelfContained_ = selfContained;
   }
   
   public void setDescription(String description)
   {
      description_ = description;
   }
   
   public String getDescription()
   {
      return description_;
   }
   
   public int getContentType()
   {
      return getOriginatingEvent().getContentType();
   }
   
   public RSConnectActionEvent getOriginatingEvent()
   {
      return originatingEvent_;
   }
   
   public String getWebsiteDir()
   {
      return websiteDir_;
   }
   
   public void setWebsiteDir(String dir)
   {
      websiteDir_ = dir;
   }
   
   public boolean isWebsiteRmd()
   {
      return !StringUtil.isNullOrEmpty(websiteDir_);
   }
   
   public String getWebsiteOutputDir()
   {
      return websiteOutputDir_;
   }
   
   public void setWebsiteOutputDir(String dir)
   {
      websiteOutputDir_ = dir;
   }
 
   public boolean isStaticDocInput()
   {
      // plots and presentations are always static
      if (getContentType() == RSConnect.CONTENT_TYPE_PLOT ||
          getContentType() == RSConnect.CONTENT_TYPE_PRES ||
          getContentType() == RSConnect.CONTENT_TYPE_HTML)
         return true;
      if (getContentType() != RSConnect.CONTENT_TYPE_DOCUMENT)
         return false;
      if (getSourceRmd() == null)
         return true;
      final String ext = getSourceRmd().getExtension().toLowerCase();
      return ext == ".html" || ext == ".md";
   }
   
   private boolean isShiny_;
   private boolean isConnectUIEnabled_;
   private boolean isExternalUIEnabled_;
   private boolean isMultiRmd_;
   private boolean isSelfContained_;
   private boolean hasConnectAccount_;
   private FileSystemItem sourceRmd_;
   private RSConnectActionEvent originatingEvent_;
   private String description_ = null;
   private String websiteDir_;
   private String websiteOutputDir_;
}
