/*
 * ScalarEdit.java
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
package org.rstudio.studio.client.workbench.views.workspace.table;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HTML;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.workspace.events.ValueChangeRequestEvent;
import org.rstudio.studio.client.workbench.views.workspace.events.ValueChangeRequestHandler;

public class ScalarEdit<TValue> extends HTML implements InlineEditor.Display<TValue>,
                                                        ClickHandler
{
   public ScalarEdit(GlobalDisplay globalDisplay,
                     ScalarConversionStrategy<TValue> convert, 
                     TValue initialValue)
   {
      globalDisplay_ = globalDisplay ;
      convert_ = convert ;
      
      setStylePrimaryName(ThemeStyles.INSTANCE.scalarEdit()) ;
      
      addClickHandler(this) ;
      addDomHandler(new BlurHandler() {
         public void onBlur(BlurEvent event)
         {
            if (!ignoreBlur_)
               endEdit(true, false, false) ;
         }
      }, BlurEvent.getType()) ;
      addDomHandler(new KeyDownHandler() {
         public void onKeyDown(KeyDownEvent event)
         {
            switch (event.getNativeKeyCode())
            {
            case KeyCodes.KEY_ENTER:
               if (convert_.allowEnter())
                  return ;
               endEdit(true, true, true) ;
               event.preventDefault() ;
               break ;
            case KeyCodes.KEY_ESCAPE:
               endEdit(false, false, true) ;
               event.preventDefault() ;
               break ;
            }
         }
      }, KeyDownEvent.getType()) ;
      
      value_ = initialValue ;
      setText(convert_.convertToDisplayString(value_)) ;
   }
   
   public HandlerRegistration addValueChangeRequestHandler(
                                      ValueChangeRequestHandler<TValue> handler)
   {
      return addHandler(handler, ValueChangeRequestEvent.TYPE) ;
   }

   private void beginEdit()
   {
      if (!isEditing() && !pending_)
      {
         setText(convert_.convertToEditString(value_)) ;
         setEditMode(true) ;
      }
      getElement().focus() ;
   }
   
   private void endEdit(boolean attemptCommit, 
                        boolean commitEvenIfNoChange, 
                        boolean forceBlur)
   {
      if (!isEditing())
         return ;
      
      if (!attemptCommit)
      {
         setEditMode(false) ;
         setText(convert_.convertToDisplayString(value_)) ;
         
         if (forceBlur)
            ((ElementEx) getElement()).blur() ;
      }
      else
      {
         TValue newValue = null;
         String errorMsg = null ;
         try
         {
             newValue = convert_.convertToValue(getText()) ;
             if (newValue == null ||
                 (!commitEvenIfNoChange && value_.equals(newValue)))
             {
                endEdit(false, false, forceBlur) ;
                return ;
             }
         }
         catch (Exception e)
         {
            Debug.log(e.toString()) ;
            errorMsg = e.getMessage() ;
         }
         
         if (errorMsg != null)
         {
            ignoreBlur_ = true ;
            globalDisplay_.showErrorMessage("Error updating value",
                                           errorMsg,
                                           new Operation() {
                                             public void execute()
                                             {
                                                ((ElementEx)getElement()).focus() ;
                                                ignoreBlur_ = false ;
                                             }
                                          }) ;
            return ;
         }

         setEditMode(false) ;
         setPending() ;
         
         fireEvent(new ValueChangeRequestEvent<TValue>(newValue)) ;
         
         if (forceBlur)
            ((ElementEx) getElement()).blur() ;
      }
   }
   
   private boolean isEditing()
   {
      return "true".equals(getElement().getAttribute("contentEditable")) ;
   }
   
   private void setEditMode(boolean editable)
   {
      getElement().setAttribute("contentEditable",
                                editable ? "true" : "false") ;
      if (editable)
         addStyleName(ThemeStyles.INSTANCE.editing()) ;
      else
         removeStyleName(ThemeStyles.INSTANCE.editing()) ;
   }
   
   private void setPending()
   {
      assert !isEditing() ;
      addStyleName(ThemeStyles.INSTANCE.editPending()) ;
      pending_ = true ;
   }

   public void onClick(ClickEvent event)
   {
      event.preventDefault();
      event.stopPropagation();
      beginEdit() ;
   }

   private TValue value_ ;
   private boolean pending_ ;
   private boolean ignoreBlur_ ;
   private final ScalarConversionStrategy<TValue> convert_ ;
   private final GlobalDisplay globalDisplay_ ;
}
