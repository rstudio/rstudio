/*
 * TextBoxWithCue.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.TextBox;
import org.rstudio.core.client.dom.DomUtils;

public class TextBoxWithCue extends TextBox
                            implements CanSetControlId
{
   public TextBoxWithCue() 
   {
      init("", getElement());
   }

   public TextBoxWithCue(String cueText)
   {
      init(cueText, getElement());
   }

   public TextBoxWithCue(String cueText, Element element)
   {
      super(element);
      init(cueText, element);
   }
   
   private void init(String cueText, Element element)
   {
      setCueText(cueText);
      DomUtils.disableSpellcheck(element);
   }

   public String getCueText()
   {
      return cueText_;
   }
   
   public void setCueText(String cueText)
   {
      cueText_ = cueText;
      DomUtils.setPlaceholder(this, cueText);
   }
   
   @Override
   public void setElementId(String id)
   {
      getElement().setId(id);
   }

   private String cueText_;
}
