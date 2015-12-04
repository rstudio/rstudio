/*
 * GlobalKeyMap.java
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
package org.rstudio.core.client.command;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.DirectedGraph;
import org.rstudio.core.client.command.KeyboardShortcut.KeyCombination;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;

public class GlobalKeyMap
{
   public interface BindableCommand
   {
      public void execute();
      public boolean isEnabled();
   }
   
   public static GlobalKeyMap INSTANCE = new GlobalKeyMap();
   private GlobalKeyMap() {}
   
   public void addBinding(KeySequence keys, BindableCommand command)
   {
      DirectedGraph<KeyCombination, List<BindableCommand>> node =
            data_.ensureNode(keys.getData());
      
      if (node.getData() == null)
         node.setData(new ArrayList<BindableCommand>());
      
      node.getData().add(command);
   }
   
   public List<BindableCommand> getBindings(KeySequence keys)
   {
      DirectedGraph<KeyCombination, List<BindableCommand>> node =
            data_.findNode(keys.getData());
      
      if (node == null)
         return null;
      
      return node.getData();
   }
   
   public BindableCommand getActiveBinding(KeySequence keys)
   {
      List<BindableCommand> commands = getBindings(keys);
      
      if (commands == null)
         return null;
      
      for (BindableCommand command : commands)
         if (command.isEnabled())
            return command;
      
      return null;
   }
   
   private final DirectedGraph<KeyCombination, List<BindableCommand>> data_ =
         new DirectedGraph<KeyCombination, List<BindableCommand>>();
}
