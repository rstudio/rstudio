package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;

public class ProjectEditingPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectEditingPreferencesPane(final SourceServerOperations server)
   {
      // source editing options
      enableCodeIndexing_ = new CheckBox("Index R source files (for code search/navigation)", false);
      enableCodeIndexing_.addStyleName(RESOURCES.styles().enableCodeIndexing());
      add(enableCodeIndexing_);
      
      chkSpacesForTab_ = new CheckBox("Insert spaces for tab", false);
      chkSpacesForTab_.addStyleName(RESOURCES.styles().useSpacesForTab());
      add(chkSpacesForTab_);
      
      numSpacesForTab_ = new NumericValueWidget("Tab width");
      numSpacesForTab_.addStyleName(RESOURCES.styles().numberOfTabs());
      add(numSpacesForTab_);
      
      encoding_ = new TextBoxWithButton(
            "Text encoding:",
            "Change...",
            new ClickHandler()
            {
               public void onClick(ClickEvent event)
               {
                  server.iconvlist(new SimpleRequestCallback<IconvListResult>()
                  {
                     @Override
                     public void onResponseReceived(IconvListResult response)
                     {
                        new ChooseEncodingDialog(
                              response.getCommon(),
                              response.getAll(),
                              encodingValue_,
                              false,
                              false,
                              new OperationWithInput<String>()
                              {
                                 public void execute(String encoding)
                                 {
                                    if (encoding == null)
                                       return;

                                    setEncoding(encoding);
                                 }
                              }).showModal();
                     }
                  });

               }
            });
      encoding_.setWidth("250px");
      encoding_.addStyleName(RESOURCES.styles().encodingChooser());
      
      add(encoding_);
      
   }
   
   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconEdit();
   }

   @Override
   public String getName()
   {
      return "Editing";
   }

   @Override
   protected void initialize(RProjectConfig prefs)
   {
      initialPrefs_ = prefs;
      
      enableCodeIndexing_.setValue(prefs.getEnableCodeIndexing());
      chkSpacesForTab_.setValue(prefs.getUseSpacesForTab());
      numSpacesForTab_.setValue(prefs.getNumSpacesForTab() + "");
      setEncoding(prefs.getEncoding());
   }
   
   @Override
   public boolean validate()
   {
      return numSpacesForTab_.validate("Tab width"); 
   }

   @Override
   public void onApply(RProjectConfig prefs)
   {
      prefs.setEnableCodeIndexing(enableCodeIndexing_.getValue());
      prefs.setUseSpacesForTab(chkSpacesForTab_.getValue());
      prefs.setNumSpacesForTab(getTabWidth());
      prefs.setEncoding(encodingValue_);
   }
   
   private void setEncoding(String encoding)
   {
      encodingValue_ = encoding;
      encoding_.setText(encoding);
   }
   
   private int getTabWidth()
   {
      try
      {
        return Integer.parseInt(numSpacesForTab_.getValue());
      }
      catch (Exception e)
      {
         // should never happen since validate would have been called
         // prior to exiting the dialog. revert to original setting
         return initialPrefs_.getNumSpacesForTab();
      }
   }
   
   private CheckBox enableCodeIndexing_;
   private CheckBox chkSpacesForTab_;
   private NumericValueWidget numSpacesForTab_;
   private TextBoxWithButton encoding_;
   private String encodingValue_;
   private RProjectConfig initialPrefs_;

}
