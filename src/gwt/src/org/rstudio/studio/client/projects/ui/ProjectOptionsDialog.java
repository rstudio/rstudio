package org.rstudio.studio.client.projects.ui;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.projects.model.RProjectConfig;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ProjectOptionsDialog extends ModalDialog<RProjectConfig>
{
   public ProjectOptionsDialog(
         RProjectConfig initialSettings,
         ProgressOperationWithInput<RProjectConfig> operation)
   {
      super("Project Options", operation);
      initialSettings_ = initialSettings;
   }
   

   @Override
   protected RProjectConfig collectInput()
   {
      return RProjectConfig.create(restoreWorkspace_.getSelectedValue(), 
                                   saveWorkspace_.getSelectedValue(), 
                                   alwaysSaveHistory_.getSelectedValue());
   }

   @Override
   protected boolean validate(RProjectConfig input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel mainPanel = new VerticalPanel();
      
      Label generalLabel = new Label("Workspace and History");
      generalLabel.addStyleName(RESOURCES.styles().headerLabel());
      mainPanel.add(generalLabel);
      
      // restore workspace
      restoreWorkspace_ = new YesNoAskDefault(
                            "Restore .RData into workspace at startup",
                            false);
      restoreWorkspace_.setSelectedValue(initialSettings_.getRestoreWorkspace());
      mainPanel.add(restoreWorkspace_);
      
      // save workspace
      saveWorkspace_ = new YesNoAskDefault(
                           "Save workspace to .RData on exit",
                           true);
      saveWorkspace_.setSelectedValue(initialSettings_.getSaveWorkspace());
      mainPanel.add(saveWorkspace_);

      // always save history
      alwaysSaveHistory_ = new YesNoAskDefault(
                           "Always save history (even when not saving .RData)",
                           false);
      alwaysSaveHistory_.setSelectedValue(initialSettings_.getAlwaysSaveHistory());
      mainPanel.add(alwaysSaveHistory_);
      
      return mainPanel;
   }
   
   private class YesNoAskDefault extends SelectWidget
   {
      public YesNoAskDefault(String label, boolean includeAsk)
      {
         super(label, 
               includeAsk ? new String[] {USE_DEFAULT, YES, NO, ASK} :
                            new String[] {USE_DEFAULT, YES, NO},
               true);
      }
      
      public void setSelectedValue(int value)
      {
         ListBox listBox = getListBox();
         if (value < listBox.getItemCount())
            listBox.setSelectedIndex(value);
         else
            listBox.setSelectedIndex(0);
      }
      
      public int getSelectedValue()
      {
         return getListBox().getSelectedIndex();
      }
   }
   
   static interface Styles extends CssResource
   {
      String headerLabel();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("ProjectOptionsDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private static final String USE_DEFAULT = "(Default)";
   private static final String YES = "Yes";
   private static final String NO = "No";
   private static final String ASK ="Ask";
   
   
   private YesNoAskDefault restoreWorkspace_;
   private YesNoAskDefault saveWorkspace_;
   private YesNoAskDefault alwaysSaveHistory_;
   private final RProjectConfig initialSettings_;
}
