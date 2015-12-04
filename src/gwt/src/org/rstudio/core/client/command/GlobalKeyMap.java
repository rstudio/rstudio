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
      DirectedGraph<KeyCombination, List<BindableCommand>> node = data_;
      for (int i = 0; i < keys.size(); i++)
         node = node.getChild(keys.get(i));
      
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
