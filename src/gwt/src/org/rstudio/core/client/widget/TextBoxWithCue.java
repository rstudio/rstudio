/*
 * TextBoxWithCue.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.TextBox;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;

public class TextBoxWithCue extends TextBox
{
   public TextBoxWithCue(String cueText)
   {
      cueText_ = cueText;
   }

   public TextBoxWithCue(String cueText, Element element)
   {
      super(element);
      cueText_ = cueText;
   }

   public String getCueText()
   {
      return cueText_;
   }
   
   public void setCueText(String cueText)
   {
      cueText_ = cueText;
   }

   @Override
   public String getText()
   {
      return isCueMode() ? "" : super.getText();
   }

   @Override
   protected void onAttach()
   {
      super.onAttach();
      if (!StringUtil.isNullOrEmpty(cueText_))
         hookEvents();
   }

   @Override
   protected void onDetach()
   {
      super.onDetach();
      unhookEvents();
   }

   private void hookEvents()
   {
      unhookEvents();

      FocusHandler focusHandler = new FocusHandler()
      {
         public void onFocus(FocusEvent event)
         {
            if (DomUtils.hasFocus(getElement()))
            {
               if (isCueMode())
               {
                  setText("");
                  removeStyleName(CUE_STYLE);
               }
            }
         }
      };

      BlurHandler blurHandler = new BlurHandler()
      {
         public void onBlur(BlurEvent event)
         {
            if (getText().length() == 0)
            {
               addStyleName(CUE_STYLE);
               setText(cueText_);
            }
         }
      };

      registrations_ = new HandlerRegistration[] {
            addFocusHandler(focusHandler),
            addBlurHandler(blurHandler),
            WindowEx.addFocusHandler(focusHandler),
            WindowEx.addBlurHandler(blurHandler)
      };

      blurHandler.onBlur(null);
   }

   private boolean isCueMode()
   {
      return (getStyleName() + " ").indexOf(CUE_STYLE + " ") >= 0;
   }

   private void unhookEvents()
   {
      if (registrations_ != null)
      {
         for (HandlerRegistration reg : registrations_)
            reg.removeHandler();
         registrations_ = null;
      }
   }

   private String cueText_;
   private final String CUE_STYLE = "cueText";
   private HandlerRegistration[] registrations_;
}
