/*
 * DataEditingTargetWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.data;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.urlcontent.UrlContentEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;

public class DataEditingTargetWidget extends Composite
   implements UrlContentEditingTarget.Display
{
   interface Resources extends ClientBundle
   {
      @Source("DataEditingTargetWidget.css")
      Styles styles();
   }
   private static Resources resources = GWT.create(Resources.class);

   public interface Styles extends CssResource
   {
      String description();

      String statusBar();
      String statusBarDisplayed();
      String statusBarOmitted();
   }

   static
   {
      resources.styles().ensureInjected();
   }

   public DataEditingTargetWidget(Commands commands, DataItem dataItem)
   {
      Styles styles = resources.styles();

      commands_ = commands;

      frame_ = new Frame(dataItem.getContentUrl());
      frame_.setSize("100%", "100%");

      Widget mainWidget = frame_;

      if (dataItem.getDisplayedObservations() != dataItem.getTotalObservations())
      {
         FlowPanel statusBar = new FlowPanel();
         statusBar.setStylePrimaryName(styles.statusBar());
         statusBar.setSize("100%", "100%");
         Label label1 = new Label(
               "Displayed "
               + StringUtil.formatGeneralNumber(dataItem.getDisplayedObservations())
               + " rows of "
               + StringUtil.formatGeneralNumber(dataItem.getTotalObservations()));
         int omitted = dataItem.getTotalObservations()
                       - dataItem.getDisplayedObservations();
         Label label2 = new Label("(" +
                                  StringUtil.formatGeneralNumber(omitted) +
                                  " omitted)");

         label1.addStyleName(styles.statusBarDisplayed());
         label2.addStyleName(styles.statusBarOmitted());

         statusBar.add(label1);
         statusBar.add(label2);

         DockLayoutPanel dockPanel = new DockLayoutPanel(Unit.PX);
         dockPanel.addSouth(statusBar, 20);
         dockPanel.add(frame_);
         dockPanel.setSize("100%", "100%");
         mainWidget = dockPanel;
      }

      PanelWithToolbars panel = new PanelWithToolbars(createToolbar(dataItem,
                                                                  styles),
                                                    mainWidget);

      initWidget(panel);

   }

   private Toolbar createToolbar(DataItem dataItem, Styles styles)
   {
      Label description = new Label(
            StringUtil.formatGeneralNumber(dataItem.getTotalObservations())
            + " observations of " +
            StringUtil.formatGeneralNumber(dataItem.getVariables())
            + " variables",
            false);
      description.addStyleName(styles.description());

      Toolbar toolbar = new EditingTargetToolbar(commands_);
      toolbar.addLeftWidget(commands_.popoutDoc().createToolbarButton());
      toolbar.addRightWidget(description);
      
      return toolbar;
   }

   public void print()
   {
      IFrameElementEx frameEl = (IFrameElementEx) frame_.getElement().cast();
      frameEl.getContentWindow().print();
   }

   public Widget asWidget()
   {
      return this;
   }

   private final Commands commands_;
   private Frame frame_;
}
