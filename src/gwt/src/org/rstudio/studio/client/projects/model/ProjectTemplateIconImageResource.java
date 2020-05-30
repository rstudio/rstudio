/*
 * ProjectTemplateIconImageResource.java
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
package org.rstudio.studio.client.projects.model;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;

public class ProjectTemplateIconImageResource implements ImageResource
{
   public ProjectTemplateIconImageResource(String name, String data)
   {
      name_ = name;
      uri_  = "data:image/png;base64," + data;
      safeUri_ = new SafeUri()
      {
         @Override
         public String asString()
         {
            return uri_;
         }
      };
   }

   @Override
   public String getName()
   {
      return name_;
   }

   @Override
   public int getHeight()
   {
      return 24;
   }

   @Override
   public int getLeft()
   {
      return 0;
   }

   @Override
   public SafeUri getSafeUri()
   {
      return safeUri_;
   }

   @Override
   public int getTop()
   {
      return 0;
   }

   @Deprecated
   @Override
   public String getURL()
   {
      return uri_;
   }

   @Override
   public int getWidth()
   {
      return 24;
   }

   @Override
   public boolean isAnimated()
   {
      return false;
   }
   
   private final String name_;
   private final String uri_;
   private final SafeUri safeUri_;
}
