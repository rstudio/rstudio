/*
 * NewQuartoProjectPage.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.projects.ui.newproject;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.projects.model.NewProjectInput;
import org.rstudio.studio.client.projects.model.NewQuartoProjectOptions;
import org.rstudio.studio.client.quarto.model.QuartoCapabilities;
import org.rstudio.studio.client.quarto.model.QuartoConstants;
import org.rstudio.studio.client.quarto.model.QuartoJupyterKernel;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;

public class NewQuartoProjectPage extends NewDirectoryPage
{
   public NewQuartoProjectPage()
   {
      super("Quarto Project", 
            "Create a new Quarto project",
            "Create Quarto Project",
            new ImageResource2x(NewProjectResources.INSTANCE.quartoIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.quartoIconLarge2x()));
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      loadAndPersistClientState();
   }
   
   @Inject
   private void initialize(Session session)
   {
      session_ = session;
   }
      
   
   
   @Override
   protected void onAddTopPanelWidgets(HorizontalPanel panel)
   {
      projectTypeSelect_ = new SelectWidget("Type:",
            new String[] {"(Default)", "Website", "Book" },
            new String[] {
                  QuartoConstants.PROJECT_DEFAULT,
                  QuartoConstants.PROJECT_WEBSITE,
                  QuartoConstants.PROJECT_BOOK
            },
            false);  
      projectTypeSelect_.getListBox().getElement().addClassName(
         NewProjectResources.INSTANCE.styles().quartoProjectTypeSelect());
      
      panel.add(projectTypeSelect_);
   }
    
 
   @Override
   protected void onAddMiddleWidgets()
   {
      addSpacer();
      addSpacer();
      
      HorizontalPanel panel = new HorizontalPanel();     
      
      engineSelect_ = new SelectWidget("Engine:", 
            new String[] {"(None)", "Knitr", "Jupyter"},
            new String[] {
              QuartoConstants.ENGINE_MARKDOWN, 
              QuartoConstants.ENGINE_KNITR, 
              QuartoConstants.ENGINE_JUPYTER
            }, false, true, false);
      engineSelect_.getElement().addClassName(
         NewProjectResources.INSTANCE.styles().quartoEngineSelect());
      panel.add(engineSelect_);
      
      
      kernelSelect_ = new SelectWidget("Kernel:");
      panel.add(kernelSelect_);
      
      addWidget(panel);
   
   }
   
   @Override
   protected void onAddBottomWidgets()
   {
      venvPanel_ = new HorizontalPanel();
      chkUseVenv_ = new CheckBox("Use venv with packages: ");
      ElementIds.assignElementId(chkUseVenv_,
         ElementIds.idWithPrefix(getTitle(), ElementIds.NEW_PROJECT_VENV));
      venvPanel_.add(chkUseVenv_);
      txtVenvPackages_ = new TextBox();
      txtVenvPackages_.addStyleName(NewProjectResources.INSTANCE.styles().quartoVenvPackages());
      DomUtils.disableSpellcheck(txtVenvPackages_);
      FontSizer.applyNormalFontSize(txtVenvPackages_.getElement());
      ElementIds.assignElementId(txtVenvPackages_,
         ElementIds.idWithPrefix(getTitle(), ElementIds.NEW_PROJECT_VENV_PACKAGES));
      venvPanel_.add(txtVenvPackages_);
      addWidget(venvPanel_);
   }
   
   @Override 
   protected void initialize(NewProjectInput input)
   {
      super.initialize(input);
      
      // set type
      projectTypeSelect_.setValue(lastOptions_.getType());
      
      // set engine
      engineSelect_.setValue(lastOptions_.getEngine());
      engineSelect_.addChangeHandler((event) -> {
         manageControls();
      });
      
      // popuplate kernel list from caps
      quartoCaps_ = input.getContext().getQuartoCapabilities();
      JsArray<QuartoJupyterKernel> kernels = quartoCaps_.jupyterKernels();
      
      String[] kernelNames = new String[kernels.length()];
      String[] kernelDisplayNames = new String[kernels.length()];
      for (int i=0; i<kernels.length(); i++)
      {
         QuartoJupyterKernel kernel = kernels.get(i);
         kernelNames[i] = kernel.getName();
         kernelDisplayNames[i] = kernel.getDisplayName();
      }
      kernelSelect_.setChoices(kernelDisplayNames, kernelNames);
      kernelSelect_.setValue(lastOptions_.getKernel());
   
      chkUseVenv_.setValue(canUseVenv() && !StringUtil.isNullOrEmpty(lastOptions_.getVenv()));
      txtVenvPackages_.setValue(lastOptions_.getPackages());
      
      manageControls();
      
   }
   
   private void manageControls()
   {
      boolean isKnitr =  engineSelect_.getValue().equals(QuartoConstants.ENGINE_KNITR);
      boolean isJupyter = engineSelect_.getValue().equals(QuartoConstants.ENGINE_JUPYTER);
      kernelSelect_.setVisible(isJupyter);
      venvPanel_.setVisible(isJupyter && canUseVenv());
      setUseRenvVisible(isKnitr);
   }
   
   private boolean canUseVenv()
   {
      return quartoCaps_ != null &&
             quartoCaps_.getPythonCapabilities() != null &&
             quartoCaps_.getPythonCapabilities().getVenv();
   }
   
 
   @Override
   protected NewQuartoProjectOptions getNewQuartoProjectOptions()
   {
      lastOptions_ = NewQuartoProjectOptions.create(
            projectTypeSelect_.getValue(), 
            engineSelect_.getValue(), 
            kernelSelect_.getValue(), 
            chkUseVenv_.getValue() ? "venv" : "",
            txtVenvPackages_.getText().trim()
      );
      
      return lastOptions_;
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      session_.persistClientState();
   }

   private SelectWidget projectTypeSelect_;
   private SelectWidget engineSelect_;
   private SelectWidget kernelSelect_;
   private CheckBox chkUseVenv_;
   private TextBox txtVenvPackages_;
   private HorizontalPanel venvPanel_;
   private Session session_;
   private QuartoCapabilities quartoCaps_ = null;

   
   private class ClientStateValue extends JSObjectStateValue
   {
      public ClientStateValue()
      {
         super("quarto",
               "new-project",
               ClientState.PERSISTENT,
               session_.getSessionInfo().getClientState(),
               false);
      }

      @Override
      protected void onInit(JsObject value)
      {
         lastOptions_ = (value == null) ?
            NewQuartoProjectOptions.createDefault() :
               NewQuartoProjectOptions.create(
                        value.getString("type"),
                        value.getString("engine"),
                        value.getString("kernel"),
                        value.getString("venv"),
                        value.getString("packages")
                  );
      }
 
      @Override
      protected JsObject getValue()
      {
         return lastOptions_.cast();
      }
   }

   private final void loadAndPersistClientState()
   {
      if (clientStateValue_ == null)
         clientStateValue_ = new ClientStateValue();
   }
   private static ClientStateValue clientStateValue_;
   private static NewQuartoProjectOptions lastOptions_ = NewQuartoProjectOptions.createDefault();

}
