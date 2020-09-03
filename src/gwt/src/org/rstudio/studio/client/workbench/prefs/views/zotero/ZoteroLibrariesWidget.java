/*
 * ZoteroLibrariesWidget.java
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
package org.rstudio.studio.client.workbench.prefs.views.zotero;

import org.rstudio.core.client.widget.CheckBoxList;
import org.rstudio.core.client.widget.SelectWidget;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ZoteroLibrariesWidget extends Composite
{
   public ZoteroLibrariesWidget()
   {
      VerticalPanel panel = new VerticalPanel();
      panel.addStyleName(RES.styles().librariesWidget());
      
      selectedLibs_ = new SelectWidget(
         "Use libraries:", 
         new String[] { "All Libraries", "Selected Libraries" },
         new String[] { ALL_LIBRARIES, SELECTED_LIBRARIES },
         false,
         true,
         false
      );
      selectedLibs_.addStyleName(RES.styles().librariesSelect());
      panel.add(selectedLibs_);   
        
      libraries_ = new CheckBoxList(selectedLibs_.getLabel());
      libraries_.addStyleName(RES.styles().librariesList());
      libraries_.addItem(new CheckBox("My Library"));
      libraries_.addItem(new CheckBox("Group Library"));
      libraries_.addItem(new CheckBox("Thesis Library"));
      panel.add(libraries_);
   
      Roles.getListboxRole().setAriaLabelledbyProperty(libraries_.getElement(),
            Id.of(selectedLibs_.getLabel().getElement()));
      
      manageUI();
      selectedLibs_.addChangeHandler((value) -> {
         manageUI();
      });
        
      initWidget(panel);
   }
   
   private void manageUI()
   {
      libraries_.setVisible(selectedLibs_.getValue().equals(SELECTED_LIBRARIES));
   }
   
   private static ZoteroResources RES = ZoteroResources.INSTANCE;
   
   
   private final static String ALL_LIBRARIES = "all";
   private final static String SELECTED_LIBRARIES = "selected";
   
   private final SelectWidget selectedLibs_;
   private final CheckBoxList libraries_;
}
