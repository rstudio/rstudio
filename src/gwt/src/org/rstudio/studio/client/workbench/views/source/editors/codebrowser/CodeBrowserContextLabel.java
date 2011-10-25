/*
 * CodeBrowserContextLabel.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.codebrowser;

// TODO: implement menu lookup/switching (optimize?)

// TODO: refactor into correct widgets

// TODO: automatically show foo.default for generic method (do this carefully!)

// TODO: timeSeries F2 doesn't yield anyting (because is method?)

// TODO: super-slow lookup of print methods

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;

public class CodeBrowserContextLabel extends Composite
{
   public CodeBrowserContextLabel(CodeBrowserEditingTargetWidget.Styles styles)
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
               ToolbarPopupMenu menu = new ToolbarPopupMenu();
               JsArrayString s3Methods = functionDef_.getS3Methods();
               for (int i=0; i < s3Methods.length(); i++)
               {
                  MenuItem mi = new MenuItem(s3Methods.get(i), new Command() {
                     @Override
                     public void execute()
                     {
                        // TODO Auto-generated method stub
                        
                     }
                  });
                  
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
      
      dropDownImage_ = new Image(ThemeResources.INSTANCE.mediumDropDownArrow());
      dropDownImage_.addStyleName(styles.menuElement());
      dropDownImage_.addStyleName(styles.dropDownImage());
      dropDownImage_.addClickHandler(clickHandler);
      panel.add(dropDownImage_);
      dropDownImage_.setVisible(false);
      
      initWidget(panel);
   }
   
   public void setCurrentFunction(SearchPathFunctionDefinition functionDef)
   {
      functionDef_ = functionDef;
      
      nameLabel_.setText(functionDef.getName());
      namespaceLabel_.setText("(" + functionDef.getNamespace() + ")");
      
      JsArrayString s3Methods = functionDef.getS3Methods();
      boolean hasMethods = s3Methods.length() > 0;
      
      if (hasMethods)
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
   
   private SearchPathFunctionDefinition functionDef_;
   private Label captionLabel_;
   private Label nameLabel_;
   private Label namespaceLabel_;
   private Image dropDownImage_;
}
