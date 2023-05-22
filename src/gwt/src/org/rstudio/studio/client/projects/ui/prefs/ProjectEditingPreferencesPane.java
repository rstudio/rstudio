/*
 * ProjectEditingPreferencesPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.workbench.prefs.model.ProjectPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.views.LineEndingsSelectWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.IconvListResult;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ProjectEditingPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectEditingPreferencesPane(final SourceServerOperations server)
   {
      add(headerLabel("Editing"));

      Label infoLabel = new Label(constants_.projectGeneralInfoLabel());
      infoLabel.addStyleName(PreferencesDialogBaseResources.INSTANCE.styles().infoLabel());
      infoLabel.addStyleName(PreferencesDialogBaseResources.INSTANCE.styles().nudgeRightPlus());
      infoLabel.addStyleName(PreferencesDialogBaseResources.INSTANCE.styles().spaced());
      add(infoLabel);
      
      chkSpacesForTab_ = new CheckBox(constants_.chkSpacesForTabLabel(), false);
      chkSpacesForTab_.addStyleName(RESOURCES.styles().useSpacesForTab());
      add(chkSpacesForTab_);

      numSpacesForTab_ = new NumericValueWidget(constants_.tabWidthLabel(), 1, UserPrefs.MAX_TAB_WIDTH);
      numSpacesForTab_.addStyleName(RESOURCES.styles().numberOfTabs());
      numSpacesForTab_.setWidth("36px");
      add(numSpacesForTab_);

      LayoutGrid useNativePipeLabeled = new LayoutGrid(1, 2);
      useNativePipeOperator_ = new YesNoAskDefault(false);
      useNativePipeLabeled.setWidget(0, 0, new FormLabel(constants_.useNativePipeOperatorLabel(), useNativePipeOperator_));
      useNativePipeLabeled.setWidget(0, 1, useNativePipeOperator_);
      useNativePipeLabeled.addStyleName(RESOURCES.styles().useNativePipeOperator());
      add(spaced(useNativePipeLabeled));

      add(headerLabel("Indexing"));
      
      // source editing options
      enableCodeIndexing_ = new CheckBox(constants_.enableCodeIndexingLabel(), false);
      enableCodeIndexing_.addStyleName(RESOURCES.styles().enableCodeIndexing());
      add(enableCodeIndexing_);
      
      add(spacedBefore(headerLabel("Saving")));
      
      chkAutoAppendNewline_ = new CheckBox(constants_.chkAutoAppendNewlineLabel());
      chkAutoAppendNewline_.addStyleName(RESOURCES.styles().editingOption());
      add(chkAutoAppendNewline_);

      chkStripTrailingWhitespace_ = new CheckBox(constants_.chkStripTrailingWhitespaceLabel());
      chkStripTrailingWhitespace_.addStyleName(RESOURCES.styles().editingOption());
      add(chkStripTrailingWhitespace_);

      lineEndings_ = new LineEndingsSelectWidget(true);
      lineEndings_.addStyleName(RESOURCES.styles().editingOption());
      lineEndings_.addStyleName(RESOURCES.styles().lineEndings());
      add(lineEndings_);

      encoding_ = new TextBoxWithButton(
            constants_.textEncodingLabel(),
            "",
            constants_.changeLabel(),
            null,
            ElementIds.TextBoxButtonId.PROJECT_TEXT_ENCODING,
            true,
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
      return constants_.codingEditingLabel();
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      initialConfig_ = options.getConfig();

      enableCodeIndexing_.setValue(initialConfig_.getEnableCodeIndexing());
      chkSpacesForTab_.setValue(initialConfig_.getUseSpacesForTab());
      numSpacesForTab_.setValue(initialConfig_.getNumSpacesForTab() + "");
      useNativePipeOperator_.setSelectedIndex(initialConfig_.getUseNativePipeOperator());
      chkAutoAppendNewline_.setValue(initialConfig_.getAutoAppendNewline());
      chkStripTrailingWhitespace_.setValue(initialConfig_.getStripTrailingWhitespace());
      lineEndings_.setValue(ProjectPrefs.prefFromLineEndings(initialConfig_.getLineEndings()));
      setEncoding(initialConfig_.getEncoding());
   }

   @Override
   public boolean validate()
   {
      return numSpacesForTab_.validate();
   }

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setEnableCodeIndexing(enableCodeIndexing_.getValue());
      config.setUseSpacesForTab(chkSpacesForTab_.getValue());
      config.setNumSpacesForTab(getTabWidth());
      config.setUseNativePipeOperator(useNativePipeOperator_.getSelectedIndex());
      config.setAutoAppendNewline(chkAutoAppendNewline_.getValue());
      config.setStripTrailingWhitespace(chkStripTrailingWhitespace_.getValue());
      config.setLineEndings(ProjectPrefs.lineEndingsFromPref(lineEndings_.getValue()));
      config.setEncoding(encodingValue_);
      return new RestartRequirement();
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
   private YesNoAskDefault useNativePipeOperator_;
   private CheckBox chkAutoAppendNewline_;
   private CheckBox chkStripTrailingWhitespace_;
   private LineEndingsSelectWidget lineEndings_;
   private TextBoxWithButton encoding_;
   private String encodingValue_;
   private RProjectConfig initialConfig_;
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
}
