/*
 * ProfilerEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.profiler;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;

public class ProfilerEditingTargetWidget extends Composite
                                         implements ProfilerPresenter.Display
              
{
   public ProfilerEditingTargetWidget(Commands commands)
   {
      VerticalPanel panel = new VerticalPanel();
      panel.add(new Label("PropA"));
      txtPropA_ = new IntegerBox();
      panel.add(txtPropA_);
      panel.add(new Label("PropB"));
      chkPropB_ = new CheckBox();
      panel.add(chkPropB_); 
      panel.setSize("100%", "100%");

      PanelWithToolbars mainPanel = new PanelWithToolbars(
                                          createToolbar(commands), 
                                          panel);

      initWidget(mainPanel);

   }

   private Toolbar createToolbar(Commands commands)
   {
      Toolbar toolbar = new EditingTargetToolbar(commands);
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands.startProfiler().createToolbarButton());
      toolbar.addLeftWidget(commands.stopProfiler().createToolbarButton());
      return toolbar;
   }
   
   public Widget asWidget()
   {
      return this;
   }
   
   @Override
   public HasValue<Integer> getPropA()
   {
      return txtPropA_;
   }

   @Override
   public HasValue<Boolean> getPropB()
   {
      return chkPropB_;
   }
   
   private IntegerBox txtPropA_;
   private CheckBox chkPropB_;   
}
