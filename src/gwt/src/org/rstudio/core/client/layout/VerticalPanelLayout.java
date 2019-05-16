/*
 * VerticalPanelLayout.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.core.client.layout;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Use in place of VerticalPanel so assistive technology sees that tables are being
 * used for layout (presentation), and not for displaying tabular data.
 */
public class VerticalPanelLayout extends VerticalPanel
{
   public VerticalPanelLayout()
   {
      super();
      Roles.getPresentationRole().set(getTable());
   }
}
