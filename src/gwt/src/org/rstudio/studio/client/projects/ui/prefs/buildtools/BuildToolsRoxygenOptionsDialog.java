/*
 * BuildToolsRoxygenOptionsDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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


package org.rstudio.studio.client.projects.ui.prefs.buildtools;


import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.projects.model.RProjectAutoRoxygenizeOptions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;

public class BuildToolsRoxygenOptionsDialog extends ModalDialog<BuildToolsRoxygenOptions>
{
   public interface Binder extends UiBinder<Widget, 
                                            BuildToolsRoxygenOptionsDialog>
   {
   }
   
   public BuildToolsRoxygenOptionsDialog(
               BuildToolsRoxygenOptions options,
               OperationWithInput<BuildToolsRoxygenOptions> operation)
   {
      super("Roxygen Options", Roles.getDialogRole(), operation);
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
      chkRocletRd_.setValue(options.getRocletRd());
      chkRocletCollate_.setValue(options.getRocletCollate());
      chkRocletNamespace_.setValue(options.getRocletNamespace());
      chkRocletVignette_.setValue(options.getRocletVignette());
      RProjectAutoRoxygenizeOptions runOptions = options.getAutoRoxygenize();
      chkRunForCheckPackage_.setValue(runOptions.getRunOnCheck());
      chkRunForBuildPackage_.setValue(runOptions.getRunOnPackageBuilds());
      chkRunForBuildAndReload_.setValue(runOptions.getRunOnBuildAndReload());
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected BuildToolsRoxygenOptions collectInput()
   {
      return new BuildToolsRoxygenOptions(
            chkRocletRd_.getValue(),
            chkRocletCollate_.getValue(),
            chkRocletNamespace_.getValue(),
            chkRocletVignette_.getValue(),
        RProjectAutoRoxygenizeOptions.create(
            chkRunForCheckPackage_.getValue(),
            chkRunForBuildPackage_.getValue(),
            chkRunForBuildAndReload_.getValue()));
   }


   @Override
   protected boolean validate(BuildToolsRoxygenOptions input)
   {
      return true;
   }

   
   @UiField
   CheckBox chkRocletRd_;
   @UiField
   CheckBox chkRocletCollate_;
   @UiField
   CheckBox chkRocletNamespace_;
   @UiField
   CheckBox chkRocletVignette_;
   @UiField
   CheckBox chkRunForCheckPackage_;
   @UiField
   CheckBox chkRunForBuildPackage_;
   @UiField
   CheckBox chkRunForBuildAndReload_;
   
   
   private Widget mainWidget_; 
}
