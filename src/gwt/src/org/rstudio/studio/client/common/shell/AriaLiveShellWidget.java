/*
 * AriaLiveShellWidget.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.common.shell;

import com.google.gwt.aria.client.LiveValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

/**
 * Visibly-hidden live region used to report recent console output to a screen reader.
 */
public class AriaLiveShellWidget extends Widget
{
   public AriaLiveShellWidget(UserPrefs prefs)
   {
      prefs_ = prefs;
      lineCount_ = 0;
      setElement(Document.get().createDivElement());
      A11y.setVisuallyHidden(getElement());
      Roles.getStatusRole().setAriaLiveProperty(getElement(), LiveValue.ASSERTIVE);
   }

   public void announce(String text)
   {
      if (lineCount_ > prefs_.screenreaderConsoleAnnounceLimit().getValue())
         return;

      StringBuilder line = new StringBuilder();
      for (int i = 0; i < text.length(); i++)
      {
         char ch = text.charAt(i);
         if (ch == '\n')
         {
            line.append(ch);
            lineCount_++;
            append(line.toString());
            line.setLength(0);

            if (lineCount_ == prefs_.screenreaderConsoleAnnounceLimit().getValue())
            {
               append(constants_.consoleOutputOverLimitMessage());
               lineCount_++;
               return;
            }
         }
         else
         {
            line.append(ch);
         }
      }
      if (line.length() > 0)
      {
         append(line.toString());
      }
   }

   public void clearLiveRegion()
   {
      getElement().setInnerText("");
      lineCount_ = 0;
   }

   private void append(String text)
   {
      getElement().appendChild(Document.get().createTextNode(text));
   }

   private int lineCount_;
   private final UserPrefs prefs_;
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
}