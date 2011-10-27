package org.rstudio.core.client.widget;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Composite;

public abstract class WizardPage<I,T> extends Composite
                                      implements WizardPageInfo
{
   public WizardPage(String title, String subTitle, ImageResource image)
   {
      title_ = title;
      subTitle_ = subTitle;
      image_ = image;
   }
   
   @Override
   public String getTitle()
   {
      return title_;
   }
   
   @Override
   public String getSubTitle()
   {
      return subTitle_;
   }
   
   @Override
   public ImageResource getImage()
   {
      return image_;
   }
   
   protected Element getFocusElement()
   {
      return getElement();
   }
 
   abstract protected void initialize(I initData);
      
   abstract protected T collectInput();
   
   abstract protected boolean validate(T input);
  
   private final String title_;
   private final String subTitle_;
   private final ImageResource image_;
}
