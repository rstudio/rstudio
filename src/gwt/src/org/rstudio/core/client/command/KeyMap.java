/*
 * KeyMap.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CommandWith2Args;
import org.rstudio.core.client.DirectedGraph;
import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.DirectedGraph.DefaultConstructor;
import org.rstudio.core.client.DirectedGraph.ForEachNodeCommand;
import org.rstudio.core.client.command.KeyboardShortcut.KeyCombination;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.container.SafeMap;

public class KeyMap
{
   public static enum KeyMapType {
      ADDIN, EDITOR, APPLICATION;
   }
   
   public interface CommandBinding
   {
      public String getId();
      public void execute();
      public boolean isEnabled();
      public boolean isUserDefinedBinding();
      public AppCommand.Context getContext();
   }
   
   public KeyMap()
   {
      graph_ = new DirectedGraph<KeyCombination, List<CommandBinding>>(new DefaultConstructor<List<CommandBinding>>()
      {
         @Override
         public List<CommandBinding> create()
         {
            return new ArrayList<CommandBinding>();
         }
      });
      
      idToNodeMap_ = new SafeMap<String, List<DirectedGraph<KeyCombination, List<CommandBinding>>>>();
   }
   
   public void addBinding(KeySequence keys, CommandBinding command)
   {
      DirectedGraph<KeyCombination, List<CommandBinding>> node = graph_.ensureNode(keys.getData());
      
      if (node.getValue() == null)
         node.setValue(new ArrayList<CommandBinding>());
      node.getValue().add(0, command);
      
      if (!idToNodeMap_.containsKey(command.getId()))
         idToNodeMap_.put(command.getId(), new ArrayList<DirectedGraph<KeyCombination, List<CommandBinding>>>());
      idToNodeMap_.get(command.getId()).add(node);
   }
   
   public void setBindings(KeySequence keys, CommandBinding command)
   {
      clearBindings(command);
      addBinding(keys, command);
   }
   
   public void setBindings(List<KeySequence> keyList, CommandBinding command)
   {
      clearBindings(command);
      for (KeySequence keys : keyList)
         addBinding(keys, command);
   }
   
   public void clearBindings(CommandBinding command)
   {
      if (!idToNodeMap_.containsKey(command.getId()))
         return;
      List<DirectedGraph<KeyCombination, List<CommandBinding>>> nodes = idToNodeMap_.get(command.getId());
      
      for (DirectedGraph<KeyCombination, List<CommandBinding>> node : nodes)
      {
         List<CommandBinding> bindings = node.getValue();
         if (bindings == null || bindings.isEmpty())
            continue;
         
         List<CommandBinding> filtered = new ArrayList<CommandBinding>();
         for (CommandBinding binding : bindings)
            if (binding.getId() != command.getId())
               filtered.add(binding);
         node.setValue(filtered);
      }
      
      idToNodeMap_.remove(command.getId());
   }
   
   public List<CommandBinding> getBindings(KeySequence keys)
   {
      DirectedGraph<KeyCombination, List<CommandBinding>> node = graph_.findNode(keys.getData());
      
      if (node == null)
         return null;
      
      return node.getValue();
   }
   
   public List<KeySequence> getBindings(CommandBinding command)
   {
      return getBindings(command.getId());
   }
   
   public List<KeySequence> getBindings(String id)
   {
      List<KeySequence> keys = new ArrayList<KeySequence>();
      List<DirectedGraph<KeyCombination, List<CommandBinding>>> bindings = idToNodeMap_.get(id);
      if (bindings == null)
         return keys;
      
      for (int i = 0, n = bindings.size(); i < n; i++)
         keys.add(new KeySequence(bindings.get(i).getKeyChain()));
      
      return keys;
   }
   
   public CommandBinding getActiveBinding(KeySequence keys)
   {
      List<CommandBinding> commands = getBindings(keys);
      
      if (commands == null)
         return null;
      
      for (CommandBinding command : commands)
         if (command.isEnabled())
            return command;
      
      return null;
   }
   
   public boolean isPrefix(KeySequence keys)
   {
      DirectedGraph<KeyCombination, List<CommandBinding>> node = graph_.findNode(keys.getData());
      if (node == null)
         return false;
      
      final Mutable<Boolean> pending = new Mutable<Boolean>(false);
      node.forEachNode(new ForEachNodeCommand<KeyCombination, List<CommandBinding>>()
      {
         @Override
         public boolean continueExecution(DirectedGraph<KeyCombination, List<CommandBinding>> node)
         {
            List<CommandBinding> bindings = node.getValue();
            if (bindings == null || bindings.isEmpty())
               return true;
            
            for (CommandBinding binding : bindings)
            {
               if (binding.isEnabled())
               {
                  pending.set(true);
                  return false;
               }
            }
            
            return true;
         }
      });
      
      return pending.get();
   }
   
   public void forEachBinding(final CommandWith2Args<KeySequence, List<CommandBinding>> command)
   {
      graph_.forEachNode(new ForEachNodeCommand<KeyCombination, List<CommandBinding>>()
      {
         @Override
         public boolean continueExecution(DirectedGraph<KeyCombination, List<CommandBinding>> node)
         {
            command.execute(
                  new KeySequence(node.getKeyChain()),
                  node.getValue());
            
            return true;
         }
      });
   }
   
   // Private members ----
   
   // The actual graph used for dispatching key sequences to commands.
   private final DirectedGraph<KeyCombination, List<CommandBinding>> graph_;
   
   // Map used so we can quickly discover what bindings are active for a particular command.
   private final SafeMap<String, List<DirectedGraph<KeyCombination, List<CommandBinding>>>> idToNodeMap_;
}
