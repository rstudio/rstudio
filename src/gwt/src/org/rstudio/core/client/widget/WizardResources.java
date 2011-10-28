package org.rstudio.core.client.widget;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;

public interface WizardResources extends ClientBundle
{
   interface Styles extends CssResource
   {
      String mainWidget();
      String headerLabel();
      String headerPanel();
      String subcaptionLabel();
      String wizardBodyPanel();
      String wizardPageSelector();
      String wizardPageSelectorItem();
      String wizardPageSelectorItemFirst();
      String wizardPageSelectorItemLast();
      String wizardPageBackground();
      String wizardBackButton();
   }
   
   @Source("Wizard.css")
   Styles styles(); 
   
   ImageResource wizardBackButton();
   ImageResource wizardDisclosureArrow();
   
   
   @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
   ImageResource wizardPageSelectorBackground();
   
   @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
   ImageResource wizardPageSelectorBackgroundFirst();
   
   @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
   ImageResource wizardPageSelectorBackgroundLast();
   
   @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
   ImageResource wizardPageBackground();
   
   static WizardResources INSTANCE = 
                        (WizardResources)GWT.create(WizardResources.class);
}
