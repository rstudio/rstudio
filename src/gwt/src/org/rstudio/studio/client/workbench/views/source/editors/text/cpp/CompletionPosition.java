/*
 * CompletionPosition.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

public class CompletionPosition
{
   public enum Scope { Global, Namespace, Member, File }
    
   public CompletionPosition(Position position, String userText, Scope scope)
   {
      position_ = position;
      userText_ = userText;
      scope_ = scope;
   }
   
   public Position getPosition()
   {
      return position_;
   }
   
   public String getUserText()
   {
      return userText_;
   }
   
   public Scope getScope()
   {
      return scope_;
   }
   
   public final boolean isSupersetOf(CompletionPosition other)
   {
      return (getPosition().compareTo(other.getPosition()) == 0) &&
             (getScope() == other.getScope()) &&
             other.getUserText().startsWith(getUserText());
   }
   
   private final Position position_;
   private final String userText_;
   private final Scope scope_;
   
}
