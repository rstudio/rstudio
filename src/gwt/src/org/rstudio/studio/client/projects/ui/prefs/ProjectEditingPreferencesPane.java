/*
 * ProjectEditingPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.prefs.views.LineEndingsSelectWidget;
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
      enableCodeIndexing_ = new CheckBox("Index source files (for code search/navigation)", false);
      enableCodeIndexing_.addStyleName(RESOURCES.styles().enableCodeIndexing());
      add(enableCodeIndexing_);
      
      chkSpacesForTab_ = new CheckBox("Insert spaces for tab", false);
      chkSpacesForTab_.addStyleName(RESOURCES.styles().useSpacesForTab());
      add(chkSpacesForTab_);
      
      numSpacesForTab_ = new NumericValueWidget("Tab width");
      numSpacesForTab_.addStyleName(RESOURCES.styles().numberOfTabs());
      add(numSpacesForTab_);
      
      chkAutoAppendNewline_ = new CheckBox("Ensure that source files end with newline");
      chkAutoAppendNewline_.addStyleName(RESOURCES.styles().editingOption());
      add(chkAutoAppendNewline_);
      
      chkStripTrailingWhitespace_ = new CheckBox("Strip trailing horizontal whitespace when saving");
      chkStripTrailingWhitespace_.addStyleName(RESOURCES.styles().editingOption());
      add(chkStripTrailingWhitespace_);
      
      lineEndings_ = new LineEndingsSelectWidget(true);
      lineEndings_.addStyleName(RESOURCES.styles().editingOption());
      lineEndings_.addStyleName(RESOURCES.styles().lineEndings());
      add(lineEndings_);
      
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
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconCodeEditing2x());
   }

   @Override
   public String getName()
   {
      return "Code Editing";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      initialConfig_ = options.getConfig();
      
      enableCodeIndexing_.setValue(initialConfig_.getEnableCodeIndexing());
      chkSpacesForTab_.setValue(initialConfig_.getUseSpacesForTab());
      numSpacesForTab_.setValue(initialConfig_.getNumSpacesForTab() + "");
      chkAutoAppendNewline_.setValue(initialConfig_.getAutoAppendNewline());
      chkStripTrailingWhitespace_.setValue(initialConfig_.getStripTrailingWhitespace());
      lineEndings_.setIntValue(initialConfig_.getLineEndings());
      setEncoding(initialConfig_.getEncoding());
   }
   
   @Override
   public boolean validate()
   {
      return numSpacesForTab_.validate("Tab width"); 
   }

   @Override
   public boolean onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setEnableCodeIndexing(enableCodeIndexing_.getValue());
      config.setUseSpacesForTab(chkSpacesForTab_.getValue());
      config.setNumSpacesForTab(getTabWidth());
      config.setAutoAppendNewline(chkAutoAppendNewline_.getValue());
      config.setStripTrailingWhitespace(chkStripTrailingWhitespace_.getValue());
      config.setLineEndings(lineEndings_.getIntValue());
      config.setEncoding(encodingValue_);
      return false;
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
         return initialConfig_.getNumSpacesForTab();
      }
   }
   
   private CheckBox enableCodeIndexing_;
   private CheckBox chkSpacesForTab_;
   private NumericValueWidget numSpacesForTab_;
   private CheckBox chkAutoAppendNewline_;
   private CheckBox chkStripTrailingWhitespace_;
   private LineEndingsSelectWidget lineEndings_;
   private TextBoxWithButton encoding_;
   private String encodingValue_;
   private RProjectConfig initialConfig_;

}
