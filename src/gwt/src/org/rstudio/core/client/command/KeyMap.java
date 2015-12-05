/*
 * KeyMap.java
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

// A KeyMap provides a two-way lookup between a KeySequence, and a BindableCommand:
// - Given a key sequence, one can discover commands bound to that key sequence,
// - Given a command, one can discover what key sequences it is bound to.
import com.google.gwt.dev.util.collect.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.DirectedGraph;
import org.rstudio.core.client.DirectedGraph.DefaultConstructor;
import org.rstudio.core.client.command.KeyboardShortcut.KeyCombination;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;

public class KeyMap
{
   public interface BindableCommand
   {
      public void execute();
      public boolean isEnabled();
   }
   
   public KeyMap()
   {
      graph_ = new DirectedGraph<KeyCombination, List<BindableCommand>>(new DefaultConstructor<List<BindableCommand>>()
      {
         @Override
         public List<BindableCommand> create()
         {
            return new ArrayList<BindableCommand>();
         }
      });
      
      commandToNodeMap_ = new HashMap<BindableCommand, List<DirectedGraph<KeyCombination, List<BindableCommand>>>>();
   }
   
   public void addBinding(KeySequence keys, BindableCommand command)
   {
      DirectedGraph<KeyCombination, List<BindableCommand>> node = graph_.ensureNode(keys.getData());
      
      if (node.getValue() == null)
         node.setValue(new ArrayList<BindableCommand>());
      node.getValue().add(command);
      
      if (!commandToNodeMap_.containsKey(command))
         commandToNodeMap_.put(command, new ArrayList<DirectedGraph<KeyCombination, List<BindableCommand>>>());
      commandToNodeMap_.get(command).add(node);
   }
   
   public void clearBindings(BindableCommand command)
   {
      List<DirectedGraph<KeyCombination, List<BindableCommand>>> nodes = commandToNodeMap_.get(command);
      for (DirectedGraph<KeyCombination, List<BindableCommand>> node : nodes)
      {
         List<BindableCommand> commands = node.getValue();
         if (commands == null || commands.isEmpty())
            continue;
         
         while (commands.remove(command))
         {
         }
      }
      
      commandToNodeMap_.remove(command);
   }
   
   public List<BindableCommand> getBindings(KeySequence keys)
   {
      DirectedGraph<KeyCombination, List<BindableCommand>> node = graph_.findNode(keys.getData());
      
      if (node == null)
         return null;
      
      return node.getValue();
   }
   
   public List<KeySequence> getBindings(BindableCommand command)
   {
      List<KeySequence> keys = new ArrayList<KeySequence>();
      
      List<DirectedGraph<KeyCombination, List<BindableCommand>>> bindings = commandToNodeMap_.get(command);
      if (bindings == null)
         return keys;
      
      for (int i = 0, n = bindings.size(); i < n; i++)
         keys.add(new KeySequence(bindings.get(i).getKeyChain()));
      
      return keys;
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
   
   // Private members ----
   
   // The actual graph used for dispatching key sequences to commands.
   private final DirectedGraph<KeyCombination, List<BindableCommand>> graph_;
   
   // Map used so we can quickly discover what bindings are active for a particular command.
   private final Map<BindableCommand, List<DirectedGraph<KeyCombination, List<BindableCommand>>>> commandToNodeMap_;
}
