package org.rstudio.core.client.widget;


import java.util.ArrayList;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
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
      final int kPageUILeftMargin = 120;
      
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
      backButton_ = new Image(res.wizardBackButton());
      headerPanel_.add(backButton_);
      headerPanel_.setWidgetLeftWidth(backButton_,
                                      kTopMargin - 2, Unit.PX, 
                                      backButton_.getWidth(), Unit.PX);
      headerPanel_.setWidgetTopHeight(backButton_,
                                      kTopMargin, Unit.PX,
                                      backButton_.getHeight(), Unit.PX);
      backButton_.setVisible(false);
      backButton_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            backToSelector();
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
      pageSelectorPanel_ = new FlowPanel();
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
      if (activePage_ != null)
         return activePage_.collectInput();
      else
         return null;
   }

   @Override
   protected boolean validate(T input)
   {
      if (activePage_ != null && input != null)
         return activePage_.validate(input);
      else
         return false;
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
      animate(pageSelectorPanel_, page, true, new Command() {
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
            setOkButtonVisible(true);
            
            // set focus
            FocusHelper.setFocusDeferred(page);
         }
      });
   }
   
   private void backToSelector()
   {
      
      animate(activePage_, pageSelectorPanel_, false, new Command() {
         @Override
         public void execute()
         {
            // update active page
            activePage_ = null;
            
            // update header
            subCaptionLabel_.setVisible(true);
            backButton_.setVisible(false);
            pageCaptionLabel_.setVisible(false);
            
            // make ok button invisible
            setOkButtonVisible(false);
            
            // set focus
            pageSelectorPanel_.getElement().focus();
         }
      });
   }
   
   private class PageSelectorItem extends Composite
   {
      PageSelectorItem(final WizardPage<I,T> page)
      {
         WizardResources res = WizardResources.INSTANCE;
         WizardResources.Styles styles = res.styles();
         
         LayoutPanel layoutPanel = new LayoutPanel();
         layoutPanel.addStyleName(styles.wizardPageSelectorItem());
         
         Image image = new Image(page.getImage());
         layoutPanel.add(image);
         layoutPanel.setWidgetLeftWidth(image, 
                                        10, Unit.PX, 
                                        image.getWidth(), Unit.PX);
         layoutPanel.setWidgetTopHeight(image, 
                                        40-(image.getHeight()/2), Unit.PX, 
                                        image.getHeight(), Unit.PX);
         
        
         FlowPanel captionPanel = new FlowPanel();
         Label titleLabel = new Label(page.getTitle());
         titleLabel.addStyleName(styles.headerLabel());
         captionPanel.add(titleLabel);
         Label subTitleLabel = new Label(page.getSubTitle());
         subTitleLabel.addStyleName(styles.subcaptionLabel());
         captionPanel.add(subTitleLabel);
         layoutPanel.add(captionPanel);
         layoutPanel.setWidgetLeftWidth(captionPanel,
                                        10 + image.getWidth() + 12, Unit.PX,
                                        450, Unit.PX);
         layoutPanel.setWidgetTopHeight(captionPanel,
                                        19, Unit.PX, 
                                        55, Unit.PX);
         
         
         Image arrowImage = new Image(res.wizardDisclosureArrow());
         layoutPanel.add(arrowImage);
         layoutPanel.setWidgetRightWidth(arrowImage, 
                                         20, Unit.PX, 
                                         arrowImage.getWidth(), 
                                         Unit.PX);
         layoutPanel.setWidgetTopHeight(arrowImage,
                                        40-(arrowImage.getHeight()/2), Unit.PX,
                                        arrowImage.getHeight(), Unit.PX);
         
         
         layoutPanel.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            {
               showPage(page);
                
            }
         }, ClickEvent.getType());
       
         
         initWidget(layoutPanel);
      }
   }
   
    
   private final String subCaption_;
   
   private LayoutPanel headerPanel_;
   private Label subCaptionLabel_;
   private Image backButton_;
   private Label pageCaptionLabel_;
   
   
   
   private LayoutPanel bodyPanel_;
   private FlowPanel pageSelectorPanel_;
   private ArrayList<WizardPage<I,T>> pages_ = new ArrayList<WizardPage<I,T>>();
   private WizardPage<I,T> activePage_ = null;
   private boolean isAnimating_ = false;
}
