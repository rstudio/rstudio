package org.rstudio.core.client.widget;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class MiniDialogPopupPanel extends DecoratedPopupPanel
{
   public MiniDialogPopupPanel()
   {
      super();
      commonInit();
   }

   public MiniDialogPopupPanel(boolean autoHide)
   {
      super(autoHide);
      commonInit();
   }

   public MiniDialogPopupPanel(boolean autoHide, boolean modal)
   {
      super(autoHide, modal);
      commonInit();
   }

   private void commonInit()
   {
      addStyleName(ThemeStyles.INSTANCE.miniDialogPopupPanel());
      
      verticalPanel_ = new VerticalPanel();
      verticalPanel_.setStyleName(ThemeStyles.INSTANCE.miniDialogContainer());
      
      // title bar
      HorizontalPanel titleBar = new HorizontalPanel();
      titleBar.setWidth("100%");
      
      captionLabel_ = new Label();
      captionLabel_.setStyleName(ThemeStyles.INSTANCE.miniDialogCaption());
      titleBar.add(captionLabel_);
      titleBar.setCellHorizontalAlignment(captionLabel_, 
                                          HasHorizontalAlignment.ALIGN_LEFT);
     
      HorizontalPanel toolsPanel = new HorizontalPanel();
      toolsPanel.setStyleName(ThemeStyles.INSTANCE.miniDialogTools());
      ToolbarButton hideButton = new ToolbarButton(
            ThemeResources.INSTANCE.closeChevron(),
            new ClickHandler() { 
               public void onClick(ClickEvent event)
               {
                  MiniDialogPopupPanel.this.hideMiniDialog();
               }
            });
      hideButton.setTitle("Close");
      toolsPanel.add(hideButton);
      titleBar.add(toolsPanel);
      titleBar.setCellHorizontalAlignment(toolsPanel,
                                          HasHorizontalAlignment.ALIGN_RIGHT);
      
      verticalPanel_.add(titleBar);
      
      // main widget
      verticalPanel_.add(createMainWidget());
      
      setWidget(verticalPanel_);
   }
   
   public void setCaption(String caption)
   {
      captionLabel_.setText(caption);
   }
   
   protected abstract Widget createMainWidget();
   
   protected void hideMiniDialog()
   {
      hide();
   }
   
   private VerticalPanel verticalPanel_;
   private Label captionLabel_ ;

}
