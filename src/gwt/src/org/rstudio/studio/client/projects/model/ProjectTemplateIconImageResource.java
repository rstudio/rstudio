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

   @SuppressWarnings("deprecation")
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
