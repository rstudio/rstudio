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

public class RSConnectPublishInput
{
   public RSConnectPublishInput(int contentType, boolean isShiny, 
         FileSystemItem sourceRmd, boolean isMultiRmd, 
         boolean isConnectUIEnabled, boolean isExternalUIEnabled)
   {
      contentType_ = contentType;
      isShiny_ = isShiny;
      sourceRmd_ = sourceRmd;
      isMultiRmd_ = isMultiRmd;
      isConnectUIEnabled_ = isConnectUIEnabled;
      isExternalUIEnabled_ = isExternalUIEnabled;
   }
   
   public boolean isShiny() 
   {
      return isShiny_;
   }
   
   public FileSystemItem getSourceRmd()
   {
      return sourceRmd_;
   }
   
   public boolean isConnectUIEnabled() 
   {
      return isConnectUIEnabled_;
   }
   
   public boolean isExternalUIEnabled()
   {
      return isExternalUIEnabled_;
   }
   
   public boolean isMultiRmd()
   {
      return isMultiRmd_;
   }
   
   public int getContentType()
   {
      return contentType_;
   }
   
   private final boolean isShiny_;
   private final boolean isConnectUIEnabled_;
   private final boolean isExternalUIEnabled_;
   private final boolean isMultiRmd_;
   private final FileSystemItem sourceRmd_;
   private final int contentType_;
}
