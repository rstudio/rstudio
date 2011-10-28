package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class WizardPage<I,T> extends Composite implements CanFocus
{
   public WizardPage(String title, 
                     String subTitle, 
                     String pageCaption, 
                     ImageResource image,
                     ImageResource largeImage)
   {
      title_ = title;
      subTitle_ = subTitle;
      pageCaption_ = pageCaption;
      image_ = image;
      largeImage_ = largeImage;
      
      WizardResources.Styles styles = WizardResources.INSTANCE.styles();
      
      LayoutPanel layoutPanel = new LayoutPanel();
      
      Image pageImage = new Image(largeImage_);
      layoutPanel.add(pageImage);
      layoutPanel.setWidgetLeftWidth(pageImage,
                                     8, Unit.PX, 
                                     pageImage.getWidth(), Unit.PX);
      layoutPanel.setWidgetTopHeight(pageImage,
                                     10, Unit.PX, 
                                     pageImage.getHeight(), Unit.PX);
      
      
      Widget pageWidget = createWidget();
      pageWidget.addStyleName(styles.wizardPage());
      layoutPanel.add(pageWidget);
      layoutPanel.setWidgetLeftRight(pageWidget,
                                     130, Unit.PX, 
                                     15, Unit.PX);
      layoutPanel.setWidgetTopBottom(pageWidget, 
                                     10, Unit.PX, 
                                     0, Unit.PX);
      
      
      initWidget(layoutPanel);
      
      
      
   }
   
   public String getTitle()
   {
      return title_;
   }
   
   public String getSubTitle()
   {
      return subTitle_;
   }
   
   public String getPageCaption()
   {
      return pageCaption_;
   }
   
   public ImageResource getImage()
   {
      return image_;
   }
   
   public ImageResource getLargeImage()
   {
      return largeImage_;
   }
   
   abstract protected Widget createWidget();
   
   abstract protected void initialize(I initData);
      
   abstract protected T collectInput();
   
   abstract protected boolean validate(T input);
  
   private final String title_;
   private final String subTitle_;
   private final String pageCaption_;
   private final ImageResource image_;
   private final ImageResource largeImage_;
}
