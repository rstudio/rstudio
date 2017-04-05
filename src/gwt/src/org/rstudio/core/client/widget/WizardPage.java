/*
 * WizardPage.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;

import org.rstudio.studio.client.common.HelpLink;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class WizardPage<I,T> extends Composite
                                      implements WizardPageInfo, CanFocus
{
   public WizardPage(String title, 
                     String subTitle, 
                     String pageCaption, 
                     ImageResource image,
                     ImageResource largeImage)
   {
      this(title, subTitle, pageCaption, image, largeImage, null);
   }
   
   public WizardPage(String title, 
                     String subTitle, 
                     String pageCaption, 
                     ImageResource image,
                     ImageResource largeImage,
                     Widget widget)
   {
      title_ = title;
      subTitle_ = subTitle;
      pageCaption_ = pageCaption;
      image_ = image;
      largeImage_ = largeImage;
      
      if (widget != null)
      {
         initWidget(widget);
      }
      else
      {
         LayoutPanel layoutPanel = new LayoutPanel();
         
         if (largeImage_ != null)
         {
            Image pageImage = new Image(largeImage_);
            layoutPanel.add(pageImage);
            layoutPanel.setWidgetLeftWidth(pageImage,
                                           8, Unit.PX, 
                                           pageImage.getWidth(), Unit.PX);
            layoutPanel.setWidgetTopHeight(pageImage,
                                           10, Unit.PX, 
                                           pageImage.getHeight(), Unit.PX);
         }
            
         Widget pageWidget = createWidget();
     
         layoutPanel.add(pageWidget);
         layoutPanel.setWidgetLeftRight(pageWidget,
                                        largeImage_ != null ? 133 : 15, 
                                        Unit.PX, 
                                        15, Unit.PX);
         layoutPanel.setWidgetTopBottom(pageWidget, 
                                        10, Unit.PX, 
                                        0, Unit.PX);
         
         
         initWidget(layoutPanel);
         addStyleName(getWizardPageBackgroundStyle());  
      }
   }

   protected String getWizardPageBackgroundStyle()
   {
      WizardResources.Styles styles = WizardResources.INSTANCE.styles();
      return styles.wizardPageBackground();
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
   
   public ArrayList<WizardPage<I,T>> getSubPages()
   {
      return null;
   }
   
   public void setIntermediateResult(T result)
   {
   }
   
   public void onActivate(ProgressIndicator indicator)
   {
   }

   public void onBeforeActivate(Operation operation, ModalDialogBase wizard)
   {
      operation.execute();
   }

   public void onDeactivate(Operation operation)
   {
      operation.execute();
   }
   
   public void onWizardClosing()
   {
   }

   public HelpLink getHelpLink()
   {
      return null;
   }
   
   abstract protected Widget createWidget();
   
   abstract protected void initialize(I initData);
      
   abstract protected T collectInput();
   
   protected boolean validate(T input)
   {
      return true;
   }
   
   protected void validateAsync(T input, 
         OperationWithInput<Boolean> onValidated)
   {
      onValidated.execute(validate(input));
   }
  
   protected boolean acceptNavigation()
   {
      return true;
   }
   
   private final String title_;
   private final String subTitle_;
   private final String pageCaption_;
   private final ImageResource image_;
   private final ImageResource largeImage_;
}
