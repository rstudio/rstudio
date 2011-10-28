package org.rstudio.core.client.widget;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;

public abstract class WizardPage<I,T> extends Composite
{
   public WizardPage(String title, String subTitle, ImageResource image)
   {
      title_ = title;
      subTitle_ = subTitle;
      image_ = image;
   }
   
   public String getTitle()
   {
      return title_;
   }
   
   public String getSubTitle()
   {
      return subTitle_;
   }
   
   public ImageResource getImage()
   {
      return image_;
   }
 
   abstract protected void initialize(I initData);
      
   abstract protected T collectInput();
   
   abstract protected boolean validate(T input);
  
   private final String title_;
   private final String subTitle_;
   private final ImageResource image_;
}
