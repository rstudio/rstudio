/*
 * Wizard.java
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

import org.rstudio.core.client.CommandWithArg;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
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
      initialData_ = initialData;
      subCaption_ = subCaption;
      
      setOkButtonCaption("Create Project");
      setOkButtonVisible(false);
   }
   
   protected void addPage(WizardPage<I,T> page)
   {
      pages_.add(page);
      
      if (page instanceof WizardNavigationPage<?,?>)
      {
         ((WizardNavigationPage<I,T>) page).setSelectionHandler(
                                    new CommandWithArg<WizardPage<I,T>>() {
            @Override
            public void execute(WizardPage<I, T> page)
            {
               showPage(page);
            }      
         });
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      WizardResources res = WizardResources.INSTANCE;
      WizardResources.Styles styles = res.styles();
      
      VerticalPanel mainWidget = new VerticalPanel();
      mainWidget.addStyleName(styles.mainWidget());
      
      headerPanel_ = new LayoutPanel();
      headerPanel_.addStyleName(styles.headerPanel());
      
      // layout consants
      final int kTopMargin = 5;
      final int kLeftMargin = 8;
      final int kCaptionWidth = 400;
      final int kCaptionHeight = 30;
      final int kPageUILeftMargin = 123;
      
      // first page caption
      subCaptionLabel_ = new Label(subCaption_);
      subCaptionLabel_.addStyleName(styles.headerLabel());
      headerPanel_.add(subCaptionLabel_);
      headerPanel_.setWidgetLeftWidth(subCaptionLabel_,
                                      kTopMargin, Unit.PX, 
                                      kCaptionWidth, Unit.PX);
      headerPanel_.setWidgetTopHeight(subCaptionLabel_,
                                      kLeftMargin, Unit.PX,
                                      kCaptionHeight, Unit.PX);
      
      // second page back button
      ImageResource bkImg = res.wizardBackButton();
      backButton_ = new Label("Back");
      backButton_.addStyleName(styles.wizardBackButton());
      headerPanel_.add(backButton_);
      headerPanel_.setWidgetLeftWidth(backButton_,
                                      kTopMargin - 2, Unit.PX, 
                                      bkImg.getWidth(), Unit.PX);
      headerPanel_.setWidgetTopHeight(backButton_,
                                      kTopMargin - 2, Unit.PX,
                                      bkImg.getHeight(), Unit.PX);
      backButton_.setVisible(false);
      backButton_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            goBack();
         }
      });
      
      // second page caption label
      pageCaptionLabel_ = new Label();
      pageCaptionLabel_.addStyleName(styles.headerLabel());
      headerPanel_.add(pageCaptionLabel_);
      headerPanel_.setWidgetLeftWidth(pageCaptionLabel_,
                                      kPageUILeftMargin, Unit.PX, 
                                      kCaptionWidth, Unit.PX);
      headerPanel_.setWidgetTopHeight(pageCaptionLabel_,
                                      kLeftMargin, Unit.PX,
                                      kCaptionHeight, Unit.PX);
      pageCaptionLabel_.setVisible(false);
      
     
      mainWidget.add(headerPanel_);
      
      // main body panel for transitions
      bodyPanel_ = new LayoutPanel();
      bodyPanel_.addStyleName(styles.wizardBodyPanel());
      bodyPanel_.getElement().getStyle().setProperty("overflowX", "hidden");
      mainWidget.add(bodyPanel_);
     
      // page selection panel
      pageSelector_ = new WizardPageSelector<I,T>(
            pages_, 
            new CommandWithArg<WizardPage<I,T>>() {
         @Override
         public void execute(WizardPage<I, T> page)
         {
            showPage(page);
         }
      });
      bodyPanel_.add(pageSelector_);
      bodyPanel_.setWidgetTopBottom(pageSelector_, 0, Unit.PX, 0, Unit.PX);
      bodyPanel_.setWidgetLeftRight(pageSelector_, 0, Unit.PX, 0, Unit.PX);
      bodyPanel_.setWidgetVisible(pageSelector_, true);
    
      // add pages and make them invisible
      for (int i=0; i<pages_.size(); i++)
      {
         WizardPage<I,T> page = pages_.get(i);
         addAndInitializePage(page);
      }
      
     
      
      return mainWidget;
   }
   
   
   private void addAndInitializePage(WizardPage<I,T> page)
   {
      page.setSize("100%", "100%");
      
      bodyPanel_.add(page);
      bodyPanel_.setWidgetTopBottom(page, 0, Unit.PX, 0, Unit.PX);
      bodyPanel_.setWidgetLeftRight(page, 0, Unit.PX, 0, Unit.PX);
      bodyPanel_.setWidgetVisible(page, false);
      
      page.initialize(initialData_);
      
      // recursively initialize child pages
      if (page instanceof WizardNavigationPage<?,?>)
      {
         WizardNavigationPage<I,T> navPage = (WizardNavigationPage<I,T>)page;
         ArrayList<WizardPage<I,T>> pages = navPage.getPages();
         for (int i=0; i<pages.size(); i++)
            addAndInitializePage(pages.get(i));
      }
   }
   
   @Override
   protected T collectInput()
   {
      WizardPage<I,T> inputPage = activeInputPage();
      if (inputPage != null)
      {
         T input = ammendInput(inputPage.collectInput());
         return input;
      }
      else
         return null;
   }

   @Override
   protected boolean validate(T input)
   {
      WizardPage<I,T> inputPage = activeInputPage();
      if (inputPage != null)
         return inputPage.validate(input);
      else
         return false;
   }
   
   private WizardPage<I,T> activeInputPage()
   {
      if (activePage_ != null && 
          !(activePage_ instanceof WizardNavigationPage<?,?>))
      {
         return activePage_;
      }
      else
      {
         return null;
      }
   }
   
   private void animate(final Widget from, 
                        final Widget to, 
                        boolean rightToLeft,
                        final Command onCompleted) 
   {
      // protect against multiple calls
      if (isAnimating_)
         return;
      
       
      bodyPanel_.setWidgetVisible(to, true);

      int width = getOffsetWidth();

      bodyPanel_.setWidgetLeftWidth(from,
                                    0, Unit.PX,
                                    width, Unit.PX);
      bodyPanel_.setWidgetLeftWidth(to,
                                    rightToLeft ? width : -width, Unit.PX,
                                    width, Unit.PX);
      bodyPanel_.forceLayout();

      bodyPanel_.setWidgetLeftWidth(from,
                                    rightToLeft ? -width : width, Unit.PX,
                                    width, Unit.PX);
      bodyPanel_.setWidgetLeftWidth(to,
                                    0, Unit.PX,
                                    width, Unit.PX);
      
      isAnimating_ = true;
     
      bodyPanel_.animate(300, new AnimationCallback()
      {
         @Override
         public void onAnimationComplete()
         {
            bodyPanel_.setWidgetVisible(from, false);
          
            bodyPanel_.setWidgetLeftRight(to, 0, Unit.PX, 0, Unit.PX);
            bodyPanel_.forceLayout();
            
            isAnimating_ = false;
            
            onCompleted.execute(); 
         }
         @Override
         public void onLayout(Layer layer, double progress)
         {
         }
      });
   }
   
   private void showPage(final WizardPage<I,T> page)
   {
      // ask whether the page will accept the navigation
      if (!page.acceptNavigation())
         return;
      
      // determine behavior based on whether this is standard page or 
      // a navigation page
      Widget fromWidget;
      final boolean okButtonVisible;
      
      // are we navigating from the main selector?
      if (activePage_ == null)
      {
         fromWidget = pageSelector_;
         okButtonVisible = !(page instanceof WizardNavigationPage<?,?>);
         activeParentNavigationPage_ = null;   
      }
      // otherwise we must be navigating from a navigation page
      else 
      {
         fromWidget = activePage_;
         okButtonVisible = true;
         activeParentNavigationPage_ = activePage_;
      }
     
      
      animate(fromWidget, page, true, new Command() {
         @Override
         public void execute()
         {
            // set active page
            activePage_ = page;
            
            // update header
            subCaptionLabel_.setVisible(false);
            backButton_.setVisible(true);
            pageCaptionLabel_.setText(page.getPageCaption());
            pageCaptionLabel_.setVisible(true);
            
            // make ok button visible
            setOkButtonVisible(okButtonVisible);
            
            // call hook
            onPageActivated(page, okButtonVisible);
            
            // set focus
            FocusHelper.setFocusDeferred(page);
         }
      });
   }
   
  
   private void goBack()
   {
      final boolean isNavigationPage = activeParentNavigationPage_ != null;
      
      // determine behavior based on whether we are going back to a
      // navigation page or a selector page
      Widget toWidget;
      if (activeParentNavigationPage_ != null)
      {
         toWidget = activeParentNavigationPage_;
      }
      else
      {
         toWidget = pageSelector_;
      }
      
      final String pageCaptionLabel = isNavigationPage ? 
                        activeParentNavigationPage_.getPageCaption() : "";
      
      final WizardPage<I,T> newActivePage =
         isNavigationPage ? activeParentNavigationPage_ : null;
      
      final CanFocus focusWidget = (CanFocus)toWidget;
      
      activeParentNavigationPage_ = null;
      
      animate(activePage_, toWidget, false, new Command() {
         @Override
         public void execute()
         {
            // update active page
            activePage_ = newActivePage;
            
            // update header
            subCaptionLabel_.setVisible(!isNavigationPage);
            backButton_.setVisible(isNavigationPage);
            pageCaptionLabel_.setVisible(isNavigationPage);
            pageCaptionLabel_.setText(pageCaptionLabel);
           
            // make ok button invisible
            setOkButtonVisible(false);
            
            // call hook
            onSelectorActivated();
            
            // set focus
            focusWidget.focus();
         }
      });
   }
   
   protected void onPageActivated(WizardPage<I,T> page, boolean okButtonVisible)
   {
   }
    

   protected void onSelectorActivated()
   {
   }

   protected T ammendInput(T input)
   {
      return input;
   }
    
 
   
   private final I initialData_; 
   
   private final String subCaption_;
   
   private LayoutPanel headerPanel_;
   private Label subCaptionLabel_;
   private Label backButton_;
   private Label pageCaptionLabel_;
   
   
   
   private LayoutPanel bodyPanel_;
   private WizardPageSelector<I,T> pageSelector_;
   private ArrayList<WizardPage<I,T>> pages_ = new ArrayList<WizardPage<I,T>>();
   private WizardPage<I,T> activePage_ = null;
   private WizardPage<I,T> activeParentNavigationPage_ = null;
   private boolean isAnimating_ = false;
}
