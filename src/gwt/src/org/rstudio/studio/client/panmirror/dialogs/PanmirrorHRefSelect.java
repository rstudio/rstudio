/*
 * PanmirrorHRefSelect.java
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


package org.rstudio.studio.client.panmirror.dialogs;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.FormListBox;
import org.rstudio.core.client.widget.FormTextBox;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkHeadingTarget;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkTargets;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkType;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;



public class PanmirrorHRefSelect extends Composite implements CanFocus
{
   public PanmirrorHRefSelect(PanmirrorLinkTargets targets, 
                              PanmirrorLinkCapabilities capabilities)
   {
      
      targets_ = targets;
      capabilities_ = capabilities;
       
      controls_ = new HorizontalPanel();
      controls_.setWidth("100%");
      
      type_ = new FormListBox();
      type_.setElementId(ElementIds.getElementId(ElementIds.VISUAL_MD_LINK_TYPE));
      type_.addItem("URL", Integer.toString(PanmirrorLinkType.URL));
      
      if (capabilities.headings && targets.headings.length > 0)
         type_.addItem("Heading", Integer.toString(PanmirrorLinkType.Heading));
      if (targets.ids.length > 0)
         type_.addItem("ID",Integer.toString(PanmirrorLinkType.ID));
      controls_.add(type_);
      type_.getElement().getStyle().setMarginRight(6, Unit.PX);
      controls_.setCellHorizontalAlignment(type_, HasHorizontalAlignment.ALIGN_LEFT);
        
      hrefContainer_ = new SimplePanel();
      hrefContainer_.setWidth("100%");
      controls_.add(hrefContainer_);
      controls_.setCellWidth(hrefContainer_, "100%");
      
      href_ = new FormTextBox();
      href_.setElementId(ElementIds.getElementId(ElementIds.VISUAL_MD_LINK_HREF));
      Roles.getTextboxRole().setAriaLabelProperty(href_.getElement(), "HRef");
      href_.setWidth("100%");
      
      headings_ = new FormListBox(); 
      headings_.setElementId(ElementIds.getElementId(ElementIds.VISUAL_MD_LINK_SELECT_HEADING));
      Roles.getListboxRole().setAriaLabelProperty(headings_.getElement(), "Heading");
      headings_.setWidth("100%");
      for (PanmirrorLinkHeadingTarget target : targets_.headings)
      {
         if (target.text.trim().length() > 0)
            headings_.addItem(target.text);
      }
      
      ids_ = new FormListBox();
      ids_.setElementId(ElementIds.getElementId(ElementIds.VISUAL_MD_LINK_SELECT_ID));
      Roles.getListboxRole().setAriaLabelProperty(ids_.getElement(), "IDs");
      ids_.setWidth("100%");
      for (String id : targets_.ids)
      {
         ids_.addItem("#" + id);
      }
      
      syncHRefType();
      type_.addChangeHandler((event) -> {
         syncHRefType();
      });
      
      VerticalPanel container = new VerticalPanel();
      container.add(new FormLabel("Link To:", type_));
      container.add(controls_);
      
      initWidget(container);
   }
   
   
   public void setHRef(int type, String href)
   {
      // screen out sets of extended types if our capabilities don't support them
      if (type == PanmirrorLinkType.ID && (!capabilities_.attributes || targets_.ids.length == 0)) {
         type = PanmirrorLinkType.URL;
      } else if (type == PanmirrorLinkType.Heading && (!capabilities_.headings || targets_.headings.length == 0)) {
         type = PanmirrorLinkType.URL;
      }
      
      type_.setSelectedIndex(type);
      syncHRefType();
      
      if (getType() == PanmirrorLinkType.URL) 
      {
         href_.setText(href);
      }
      else if (getType() == PanmirrorLinkType.Heading)
      {   
         for (int i = 0; i<targets_.headings.length; i++)
         {
            if (targets_.headings[i].text.equalsIgnoreCase(href))
            {
               headings_.setSelectedIndex(i);
               break;
            }
         }
         if (!headings_.getSelectedValue().equalsIgnoreCase(href)) {
            headings_.addItem(href);
            headings_.setSelectedIndex(headings_.getItemCount() - 1); 
         }
      }
      else if (getType() == PanmirrorLinkType.ID)
      {
         for (int i = 0; i<targets_.ids.length; i++)
         {
            if (targets_.ids[i].equalsIgnoreCase(href))
            {
               ids_.setSelectedIndex(i);
               break;
            }
         }
         if (!ids_.getSelectedValue().equalsIgnoreCase(href)) {
            ids_.addItem(href);
            ids_.setSelectedIndex(ids_.getItemCount() - 1); 
         }
      }  
   }
   
   public int getType()
   {
      return Integer.parseInt(type_.getSelectedValue());
   }
   
   public String getHRef()
   {
      int type = getType();
      if (type == PanmirrorLinkType.URL) 
      {
         return href_.getText().trim();
      }
      else if (type == PanmirrorLinkType.Heading)
      {
         return headings_.getSelectedItemText();
      }
      else if (type == PanmirrorLinkType.ID)
      {
         return ids_.getSelectedItemText();
      }  
      else
      {
         return "";
      }
   }
   
   
   public HandlerRegistration addTypeChangedHandler(ChangeHandler handler)
   {
      return type_.addChangeHandler(handler);
   }
   
   @Override
   public void focus()
   {
      if (hrefContainer_.getWidget() instanceof Focusable)
         FocusHelper.setFocusDeferred((Focusable)hrefContainer_.getWidget());
   }
   
   private void syncHRefType()
   {
      int type = getType();
      if (type == PanmirrorLinkType.URL) 
      {
         hrefContainer_.setWidget(href_);
      }
      else if (type == PanmirrorLinkType.Heading)
      {
         hrefContainer_.setWidget(headings_);
      }
      else if (type == PanmirrorLinkType.ID)
      {
         hrefContainer_.setWidget(ids_);
      }  
   }
   
 
   private final PanmirrorLinkTargets targets_;
   private final PanmirrorLinkCapabilities capabilities_;
   
   private final HorizontalPanel controls_;
   private final SimplePanel hrefContainer_;
   private final FormListBox type_;
   private final FormTextBox href_; 
   private final FormListBox headings_;
   private final FormListBox ids_;
   
}
