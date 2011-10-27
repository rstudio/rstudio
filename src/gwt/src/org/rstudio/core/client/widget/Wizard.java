package org.rstudio.core.client.widget;


import java.util.ArrayList;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class Wizard<I,T> extends ModalDialog<T>
{
   public Wizard(String caption, 
                 String subCaption,
                 I initialData,
                 final ProgressOperationWithInput<T> operation)
   {
      super(caption, operation);
      subCaption_ = subCaption;
      
      setOkButtonCaption("Create Project");
      setOkButtonVisible(false);
   }
   
   protected void addPage(WizardPage<I,T> page)
   {
      pages_.add(page);
   }
   

   @Override
   protected Widget createMainWidget()
   {
      WizardResources.Styles styles = WizardResources.INSTANCE.styles();
      
      VerticalPanel mainWidget = new VerticalPanel();
      mainWidget.addStyleName(styles.mainWidget());
      
      Label subCaptionLabel = new Label(subCaption_);
      subCaptionLabel.addStyleName(styles.headerLabel());
      mainWidget.add(subCaptionLabel);
      
      // main body panel for transitions
      bodyPanel_ = new LayoutPanel();
      bodyPanel_.addStyleName(styles.wizardBodyPanel());
      mainWidget.add(bodyPanel_);
     
      // page selection panel
      pageSelectorPanel_ = new VerticalPanel();
      pageSelectorPanel_.addStyleName(styles.wizardPageSelector());
      pageSelectorPanel_.setSize("100%", "100%");
      for (int i=0; i<pages_.size(); i++)
      {
         PageSelectorItem pageSelector = new PageSelectorItem(pages_.get(i));
         
         if (i==0)
            pageSelector.addStyleName(styles.wizardPageSelectorItemFirst());
         
         if (i == (pages_.size() -1))
            pageSelector.addStyleName(styles.wizardPageSelectorItemLast());
         
         pageSelectorPanel_.add(pageSelector);
      }
      bodyPanel_.add(pageSelectorPanel_);
      bodyPanel_.setWidgetTopBottom(pageSelectorPanel_, 0, Unit.PX, 0, Unit.PX);
      bodyPanel_.setWidgetLeftRight(pageSelectorPanel_, 0, Unit.PX, 0, Unit.PX);
      bodyPanel_.setWidgetVisible(pageSelectorPanel_, true);
    
      // add pages and make them invisible
      for (int i=0; i<pages_.size(); i++)
      {
         WizardPage<I,T> page = pages_.get(i);
         page.setSize("100%", "100%");
         
         bodyPanel_.add(page);
         bodyPanel_.setWidgetTopBottom(page, 0, Unit.PX, 0, Unit.PX);
         bodyPanel_.setWidgetLeftRight(page, 0, Unit.PX, 0, Unit.PX);
         bodyPanel_.setWidgetVisible(page, false);
      }
      
     
      
      return mainWidget;
   }
   
   @Override
   protected T collectInput()
   {
      
      return null;
   }

   @Override
   protected boolean validate(T input)
   {
      
      return true;
   }
   
   
   private class PageSelectorItem extends Composite
   {
      PageSelectorItem(WizardPageInfo pageInfo)
      {
         WizardResources.Styles styles = WizardResources.INSTANCE.styles();
         
         LayoutPanel layoutPanel = new LayoutPanel();
         layoutPanel.addStyleName(styles.wizardPageSelectorItem());
         
         Label label =  new Label(pageInfo.getTitle());
         layoutPanel.add(label);
         
         initWidget(layoutPanel);
      }
   }
   
    
   private final String subCaption_;
   
   private LayoutPanel bodyPanel_;
   private VerticalPanel pageSelectorPanel_;
  
   private ArrayList<WizardPage<I,T>> pages_ = new ArrayList<WizardPage<I,T>>();

}
