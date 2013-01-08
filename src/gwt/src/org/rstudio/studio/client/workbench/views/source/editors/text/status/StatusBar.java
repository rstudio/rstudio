/*
 * StatusBar.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

public interface StatusBar
{
   public static final int SCOPE_SECTION = 3;
   public static final int SCOPE_CHUNK = 2;
   public static final int SCOPE_FUNCTION = 1;
   
   StatusBarElement getPosition();
   StatusBarElement getScope();
   StatusBarElement getLanguage();
   void setScopeVisible(boolean visible);
   void setScopeType(int type);
}
