/*
 * CodeBrowserContextWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;

public class CodeBrowserContextWidget extends Composite 
                                      implements HasSelectionHandlers<String>
{
   public CodeBrowserContextWidget(
               final CodeBrowserEditingTargetWidget.Styles styles)
   {
      HorizontalPanel panel = new HorizontalPanel();
      
      captionLabel_ = new Label();
      captionLabel_.addStyleName(styles.captionLabel());
      panel.add(captionLabel_);
      
     
      ClickHandler clickHandler = new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            if (dropDownImage_.isVisible())
            {
               CodeBrowserPopupMenu menu = new CodeBrowserPopupMenu();
               
               JsArrayString methods = functionDef_.getMethods();
               for (int i=0; i < methods.length(); i++)
               {
                  final String method = methods.get(i);
                  MenuItem mi = new MenuItem(method, new Command() {
                     @Override
                     public void execute()
                     {
                        SelectionEvent.fire(CodeBrowserContextWidget.this, 
                                            method) ;   
                     }
                  });
                  mi.getElement().getStyle().setPaddingRight(20, Unit.PX);                  
                  menu.addItem(mi);
               }
               
               menu.showRelativeTo(nameLabel_);
               menu.getElement().getStyle().setPaddingTop(3, Unit.PX);
            }
         }
      };
      
      
      nameLabel_ = new Label();
      nameLabel_.addStyleName(styles.menuElement());
      nameLabel_.addStyleName(styles.functionName());
      nameLabel_.addClickHandler(clickHandler);
      panel.add(nameLabel_);
      
      namespaceLabel_ = new Label();
      namespaceLabel_.addStyleName(styles.menuElement());
      namespaceLabel_.addStyleName(styles.functionNamespace());
      namespaceLabel_.addClickHandler(clickHandler);
      panel.add(namespaceLabel_);
      
      dropDownImage_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.mediumDropDownArrow2x()));
      dropDownImage_.addStyleName(styles.menuElement());
      dropDownImage_.addStyleName(styles.dropDownImage());
      dropDownImage_.addClickHandler(clickHandler);
      panel.add(dropDownImage_);
      dropDownImage_.setVisible(false);
      
      initWidget(panel);
   }
   
   @Override
   public HandlerRegistration addSelectionHandler(
                                       SelectionHandler<String> handler)
   {
      return handlers_.addHandler(SelectionEvent.getType(), handler);
   }
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event) ;
   }
   
   public void setCurrentFunction(SearchPathFunctionDefinition functionDef)
   {
      functionDef_ = functionDef;
      
      nameLabel_.setText(functionDef.getName());
      namespaceLabel_.setText("(" + functionDef.getNamespace() + ")");
           
      if (functionDef.getMethods().length() > 0)
      {
         captionLabel_.setText("Method:");
         dropDownImage_.setVisible(true);
      }
      else
      {
         captionLabel_.setText("Function:");
         dropDownImage_.setVisible(false);
      }
      
      
   }
   
   private class CodeBrowserPopupMenu extends ScrollableToolbarPopupMenu
   {
      @Override
      protected int getMaxHeight()
      {
         return Window.getClientHeight() - captionLabel_.getAbsoluteTop() -
                captionLabel_.getOffsetHeight() - 50;
      }
      
   }
   
   private SearchPathFunctionDefinition functionDef_;
   private Label captionLabel_;
   private Label nameLabel_;
   private Label namespaceLabel_;
   private Image dropDownImage_;
   
   private final HandlerManager handlers_ = new HandlerManager(null);
}
