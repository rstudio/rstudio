/*
 * CppPreferencesPane.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

public class CppPreferencesPane extends PreferencesPane {

    @Inject
    public CppPreferencesPane(PreferencesDialogResources res)
    {
        res_ = res;

        VerticalTabPanel development = new VerticalTabPanel(ElementIds.CPP_DEVELOPMENT_PREFS);
        development.add(headerLabel(constants_.cppDevelopmentTitle()));

        cppTemplate_ = new SelectWidget(
            constants_.developmentCppTemplate(),
            new String[] {
                  "Rcpp", 
                  "cpp11", 
                  constants_.developmentEmptyLabel()
            },
            new String[] {
               "Rcpp", "cpp11", "empty"
            },
            false,
            true,
            false);
         development.add(cppTemplate_);
         

        DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel(constants_.tabPackagesPanelTitle());
        tabPanel.setSize("435px", "533px");
        tabPanel.add(development, constants_.developmentManagementPanelTitle(), development.getBasePanelId());
        tabPanel.selectTab(0);
        add(tabPanel);
    }

    @Override
    public ImageResource getIcon()
    {
        return new ImageResource2x(res_.iconCpp2x());
    }

    @Override
    public String getName() 
    {
        return constants_.tabCppPanelTitle();
    }

    @Override
    protected void initialize(UserPrefs prefs) 
    {
        cppTemplate_.setValue(prefs.cppTemplate().getValue());
    }

    @Override
    public RestartRequirement onApply(UserPrefs prefs) 
    {
        RestartRequirement restartRequirement = super.onApply(prefs);

        prefs.cppTemplate().setGlobalValue(cppTemplate_.getValue());
        
        return restartRequirement;
    }
    
    private final SelectWidget cppTemplate_;
   
    private final PreferencesDialogResources res_;
    private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
