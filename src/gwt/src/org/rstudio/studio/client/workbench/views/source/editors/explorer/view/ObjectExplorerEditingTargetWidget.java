/*
 * ObjectExplorerEditingTargetWidget.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.explorer.view;

import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.inject.Inject;

public class ObjectExplorerEditingTargetWidget extends Composite
{
   public ObjectExplorerEditingTargetWidget(ObjectExplorerHandle handle,
                                            SourceDocument document)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      mainWidget_ = new DockLayoutPanel(Unit.PX);
      toolbar_ = new EditingTargetToolbar(commands_, true);
      grid_ = new ObjectExplorerDataGrid(handle, document);
      resizePanel_ = new ResizeLayoutPanel();
      
      cbAttributes_ = new CheckBox();
      
      refreshButton_ = new ToolbarButton(
            RES.refresh2x(),
            new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  grid_.refresh();
               }
            });
      
      filterWidget_ = new SearchWidget(new SuggestOracle()
      {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
         }
      });
      
      initToolbar();
      initMainWidget();
      
      PanelWithToolbars panel = new PanelWithToolbars(
            toolbar_,
            mainWidget_);
      
      initWidget(panel);
   }
   
   @Inject
   private void initialize(Commands commands)
   {
      commands_ = commands;
   }
   
   public void onActivate()
   {
      grid_.redraw();
   }
   
   public void onDeactivate()
   {
      // TODO
   }
   
   private void initToolbar()
   {
      cbAttributes_.setText("Show Attributes");
      cbAttributes_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            grid_.toggleShowAttributes(cbAttributes_.getValue());
         }
      });
      
      toolbar_.addLeftWidget(cbAttributes_);
      
      filterWidget_.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            grid_.setFilter(event.getValue());
         }
      });
      
      refreshButton_.addStyleName(RES.styles().refreshButton());
      
      toolbar_.addRightWidget(filterWidget_);
      toolbar_.addRightWidget(refreshButton_);
   }
   
   private void initMainWidget()
   {
      resizePanel_.add(grid_);
      resizePanel_.addResizeHandler(new ResizeHandler()
      {
         @Override
         public void onResize(ResizeEvent event)
         {
            grid_.onResize();
         }
      });
      
      mainWidget_.setSize("100%", "100%");
      mainWidget_.add(resizePanel_);
   }
   
   private final DockLayoutPanel mainWidget_;
   private final Toolbar toolbar_;
   private final ResizeLayoutPanel resizePanel_;
   private final ObjectExplorerDataGrid grid_;
   
   private final ToolbarButton refreshButton_;
   private final CheckBox cbAttributes_;
   private final SearchWidget filterWidget_;
   
   // Injected
   private Commands commands_;
   
   // Resources, etc ----
   public interface Resources extends ClientBundle
   {
      @Source("images/refresh_2x.png")
      ImageResource refresh2x();
      
      @Source("ObjectExplorerEditingTargetWidget.css")
      Styles styles();
   }
   
   public interface Styles extends CssResource
   {
      String refreshButton();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.styles().ensureInjected();
   }
   
}
