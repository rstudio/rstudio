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
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.common.HelpLink;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class Wizard<I,T> extends ModalDialog<T>
{
   public Wizard(String caption, 
                 String okCaption,
                 I initialData,
                 WizardPage<I, T> firstPage,
                 final ProgressOperationWithInput<T> operation)
   {
      super(caption, operation);

      initialData_ = initialData;
      okCaption_ = okCaption;
      firstPage_ = firstPage;
      activePage_ = firstPage;
      
      resetOkButtonCaption();
      setOkButtonVisible(false);
      
      addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> arg0)
         {
            cleanupPage(firstPage_);
         }
      });

      // add next button
      nextButton_ = new ThemedButton("Next", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            if (activePage_ instanceof WizardIntermediatePage<?,?>) 
            {
               final WizardIntermediatePage<I, T> page = 
                     (WizardIntermediatePage<I, T>) activePage_;
               
               // collect input from this page asynchronously and advance when
               // we have input
               page.collectIntermediateInput(getProgressIndicator(), 
                     new OperationWithInput<T>()
                     {
                        @Override
                        public void execute(final T input)
                        {
                           // prevent re-entrancy
                           if (validating_)
                              return;
                           
                           validating_ = true;
                           try
                           {
                              page.validateAsync(input, 
                                    new OperationWithInput<Boolean>()
                              {
                                 @Override
                                 public void execute(Boolean valid)
                                 {
                                    validating_ = false;
                                    if (valid)
                                    {
                                       intermediateResult_ = input;
                                       page.advance();
                                    }
                                 }
                              });
                           }
                           catch (Exception e)
                           {
                              validating_ = false;
                              Debug.logException(e);
                           }
                        }
                     });
            }
         }
      });
      nextButton_.setVisible(false);
      addActionButton(nextButton_);
   }

   public void updateHelpLink(HelpLink helpLink) {
      if (helpLink == null) {
         if (helpLink_ != null) removeLeftWidget(helpLink_);
         helpLink_ = null;
         return;
      }

      if (helpLink_ == null) {
         WizardResources.Styles styles = WizardResources.INSTANCE.styles();

         helpLink_ = new HelpLink(
               "",
               "",
               false);
         helpLink_.addStyleName(styles.helpLink());
         addLeftWidget(helpLink_);
      }

      helpLink_.setCaption(helpLink.getCaption());
      helpLink_.setLink(helpLink.getLink(), helpLink.isRStudioLink());
   }

   @Override
   protected void onUnload()
   {
      activePage_.onDeactivate(new Operation() {
         public void execute() {
         }
      });

      super.onUnload();
   }
   
   @Override
   protected Widget createMainWidget()
   {
      WizardResources res = WizardResources.INSTANCE;
      WizardResources.Styles styles = res.styles();
      
      VerticalPanel mainWidget = new VerticalPanel();
      mainWidget.addStyleName(getMainWidgetStyle());
      
      headerPanel_ = new LayoutPanel();
      headerPanel_.addStyleName(styles.headerPanel());
      
      // layout constants
      final int kTopMargin = 5;
      final int kLeftMargin = 8;
      final int kCaptionWidth = 400;
      final int kCaptionHeight = 30;
      final int kPageUILeftMargin = 123;
      
      // first page caption
      subCaptionLabel_ = new Label(firstPage_.getPageCaption());
      subCaptionLabel_.addStyleName(styles.headerLabel());
      headerPanel_.add(subCaptionLabel_);
      headerPanel_.setWidgetLeftWidth(subCaptionLabel_,
                                      kTopMargin, Unit.PX, 
                                      kCaptionWidth, Unit.PX);
      headerPanel_.setWidgetTopHeight(subCaptionLabel_,
                                      kLeftMargin, Unit.PX,
                                      kCaptionHeight, Unit.PX);
      
      // second page back button
      ImageResource2x bkImg = new ImageResource2x(res.wizardBackButton2x());
      backButton_ = new Label("Back");
      backButton_.addStyleName(styles.wizardBackButton());
      backButton_.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
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
      ArrayList<String> wizardBodyStyles = getWizardBodyStyles();
      for (String styleName: wizardBodyStyles)
         bodyPanel_.addStyleName(styleName);
      bodyPanel_.getElement().getStyle().setProperty("overflowX", "hidden");
      mainWidget.add(bodyPanel_);
     
      // add first page (and all sub-pages recursively)
      addAndInitializePage(firstPage_, true);
      
      setNextButtonState(firstPage_);

      return mainWidget;
   }
   
   private void addAndInitializePage(WizardPage<I,T> page, boolean visible)
   {
      page.setSize("100%", "100%");
      
      bodyPanel_.add(page);
      bodyPanel_.setWidgetTopBottom(page, 0, Unit.PX, 0, Unit.PX);
      bodyPanel_.setWidgetLeftRight(page, 0, Unit.PX, 0, Unit.PX);
      bodyPanel_.setWidgetVisible(page, visible);
      
      page.initialize(initialData_);
      
      CommandWithArg<WizardPage<I,T>> showPageCmd = 
            new CommandWithArg<WizardPage<I,T>>() 
      {
         @Override
         public void execute(WizardPage<I, T> page)
         {
            showPage(page);
         };
      };

      if (page instanceof WizardNavigationPage<?,?>)
      {
         ((WizardNavigationPage<I,T>) page).setSelectionHandler(showPageCmd);
      }
      else if (page instanceof WizardIntermediatePage<?,?>) 
      {
         ((WizardIntermediatePage<I,T>) page).setNextHandler(showPageCmd);
      }

      // recursively initialize child pages
      ArrayList<WizardPage<I,T>> subPages = page.getSubPages();
      if (subPages != null)
      {
         for (int i = 0; i < subPages.size(); i++)
         {
            addAndInitializePage(subPages.get(i), false);
         }
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
   protected void validateAsync(T input, 
         final OperationWithInput<Boolean> onValidated)
   {
      WizardPage<I,T> inputPage = activeInputPage();
      if (inputPage != null)
      {
         validating_ = true;
         try
         {
            inputPage.validateAsync(input, new OperationWithInput<Boolean>()
            {
               @Override
               public void execute(Boolean input)
               {
                  validating_ = false;
                  onValidated.execute(input);
               }
            });
         }
         catch (Exception e)
         {
            Debug.logException(e);
            validating_ = false;
         }
      }
      else
         onValidated.execute(false);
   }
   
   @Override
   public void showModal()
   {
      super.showModal();

      // set up state for the first page (some of this is ordinarily reached
      // via navigation)
      if (firstPage_ != null)
      {
         setOkButtonVisible(pageIsFinal(firstPage_));
         firstPage_.onActivate(getProgressIndicator());
      }
   }
   
   protected WizardPage<I,T> getFirstPage()
   {
      return firstPage_;
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

      page.onBeforeActivate(new Operation() {
         public void execute() {
            // give the page the currently accumulated result, if any
            page.setIntermediateResult(intermediateResult_);
                  
            // determine behavior based on whether this is standard page or 
            // a navigation page
            final boolean okButtonVisible = pageIsFinal(page);
            activeParentNavigationPage_ = activePage_;
            
            activePage_.onDeactivate(new Operation() {
               public void execute() {
                  animate(activePage_, page, true, new Command() {
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
                        
                        // if this is an intermediate page, make Next visible
                        setNextButtonState(page);
                        
                        // let wizard and page know that the new page is active
                        onPageActivated(page, okButtonVisible);
                        page.onActivate(getProgressIndicator());
                        
                        // set focus
                        FocusHelper.setFocusDeferred(page);
                     }
                  });
               }
            });
         }
      }, this);
   }
   
  
   private void goBack()
   {
      final boolean isNavigationPage = activeParentNavigationPage_ != null;

      // determine behavior based on whether we are going back to a
      // navigation page or a selector page
      final Widget toWidget = isNavigationPage ? 
         activeParentNavigationPage_ :
         firstPage_;
      
      final String pageCaptionLabel = isNavigationPage ? 
                        activeParentNavigationPage_.getPageCaption() : "";
      
      final WizardPage<I,T> newActivePage =
         isNavigationPage ? activeParentNavigationPage_ : firstPage_;
      
      final CanFocus focusWidget = (CanFocus)toWidget;
      
      activeParentNavigationPage_ = null;
      
      onPageDeactivated(activePage_);
      activePage_.onDeactivate(new Operation() {
         public void execute() {
            animate(activePage_, toWidget, false, new Command() {
               @Override
               public void execute()
               {
                  // update active page
                  activePage_ = newActivePage;
                  
                  // update header
                  subCaptionLabel_.setVisible(newActivePage == firstPage_);
                  pageCaptionLabel_.setVisible(
                        newActivePage != firstPage_ && isNavigationPage);
                  pageCaptionLabel_.setText(pageCaptionLabel);
                  
                  setNextButtonState(newActivePage);
                  backButton_.setVisible(
                        newActivePage != firstPage_);

                  // make ok button invisible
                  setOkButtonVisible(false);
                  
                  // call hook
                  onSelectorActivated();
                  
                  // set focus
                  focusWidget.focus();
               }
            });
         }
      });
   }
   
   protected void onPageActivated(WizardPage<I,T> page, boolean okButtonVisible)
   {
   }

   protected void onPageDeactivated(WizardPage<I,T> page)
   {
   }

   protected void onSelectorActivated()
   {
   }

   protected T ammendInput(T input)
   {
      return input;
   }

   protected String getMainWidgetStyle()
   {
      return WizardResources.INSTANCE.styles().mainWidget();
   }
    
   protected ArrayList<String> getWizardBodyStyles()
   {
      ArrayList<String> classes = new ArrayList<String>();
      classes.add(WizardResources.INSTANCE.styles().wizardBodyPanel());
      return classes;
   }
   
   private void resetOkButtonCaption()
   {
      setOkButtonCaption(okCaption_);
   }
 
   private boolean pageIsFinal(WizardPage<I, T> page)
   {
      return page.getSubPages() == null ||
            page.getSubPages().size() == 0;
   }
   
   private void setNextButtonState(WizardPage<I, T> page)
   {
      boolean isIntermediate = page instanceof WizardIntermediatePage<?,?>;
      nextButton_.setVisible(isIntermediate);
      setDefaultOverrideButton(isIntermediate ? nextButton_ : null);
   }
   
   private void cleanupPage(WizardPage<I,T> page)
   {
      if (page == null)
         return;

      // notify child pages first (do cleanup in reverse order of construction)
      ArrayList<WizardPage<I,T>> subPages = page.getSubPages();
      if (subPages != null)
      {
         for (int i = 0; i < subPages.size(); i++)
         {
            cleanupPage(subPages.get(i));
         }
      }
      
      // clean this page
      page.onWizardClosing();
   }
   
   
   private final I initialData_; 
   private T intermediateResult_;
   
   private final String okCaption_;
   
   private LayoutPanel headerPanel_;
   private Label subCaptionLabel_;
   private Label backButton_;
   private Label pageCaptionLabel_;
   private ThemedButton nextButton_;
   
   private LayoutPanel bodyPanel_;
   private WizardPage<I,T> firstPage_ = null;
   private WizardPage<I,T> activePage_ = null;
   private WizardPage<I,T> activeParentNavigationPage_ = null;
   private boolean isAnimating_ = false;
   private boolean validating_ = false;
   private HelpLink helpLink_ = null;
}
