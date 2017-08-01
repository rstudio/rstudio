/*
 * TerminalList.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.console.ConsoleProcess.ConsoleProcessFactory;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalBusyEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalCwdEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSubprocEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * List of terminals, with sufficient metadata to display a list of
 * available terminals and reconnect to them.
 */
public class TerminalList implements Iterable<String>,
                                     TerminalSubprocEvent.Handler,
                                     TerminalCwdEvent.Handler
{
   private static class TerminalListData
   {
      public TerminalListData(ConsoleProcessInfo cpi, boolean hasSession)
      {
         cpi_ = cpi;
         sessionCreated_ = hasSession;
      }
      
      public ConsoleProcessInfo getCPI()
      {
         return cpi_;
      }

      public void setSessionCreated()
      {
         sessionCreated_ = true;
      }
      
      public boolean getSessionCreated()
      {
         return sessionCreated_;
      }
      
      private ConsoleProcessInfo cpi_;
      private boolean sessionCreated_;
   }
   
   protected TerminalList() 
   {
      RStudioGinjector.INSTANCE.injectMembers(this); 
      eventBus_.addHandler(TerminalSubprocEvent.TYPE, this);
      eventBus_.addHandler(TerminalCwdEvent.TYPE, this);
   }

   @Inject
   private void initialize(Provider<ConsoleProcessFactory> pConsoleProcessFactory,
                           EventBus events,
                           UIPrefs uiPrefs)
   {
      pConsoleProcessFactory_ = pConsoleProcessFactory;
      eventBus_ = events;
      uiPrefs_ = uiPrefs;
   }

   /**
    * Add metadata from supplied TerminalSession
    * @param terminal terminal to add
    */
   public void addTerminal(TerminalSession term)
   {
      addTerminal(term.getProcInfo(), true);
   }

   /**
    * Add metadata from supplied ConsoleProcessInfo
    * @param procInfo metadata to add
    */
   public void addTerminal(ConsoleProcessInfo procInfo, boolean hasSession)
   {
      terminals_.put(procInfo.getHandle(), new TerminalListData(procInfo, hasSession));
      updateTerminalBusyStatus();
   }

   /**
    * Change terminal title.
    * @param handle handle of terminal
    * @param title new title
    * @return true if title was changed, false if it was unchanged
    */
   public boolean retitleTerminal(String handle, String title)
   {
      ConsoleProcessInfo current = getMetadataForHandle(handle);
      if (current == null)
      {
         return false;
      }

      if (!current.getTitle().equals(title))
      {
         current.setTitle(title);
         return true;
      }
      return false;
   }

   /**
    * update has subprocesses flag
    * @param handle terminal handle
    * @param childProcs new subprocesses flag value
    * @return true if changed, false if unchanged
    */
   private boolean setChildProcs(String handle, boolean childProcs)
   {
      ConsoleProcessInfo current = getMetadataForHandle(handle);
      if (current == null)
      {
         return false;
      }

      if (current.getHasChildProcs() != childProcs)
      {
         current.setHasChildProcs(childProcs);
         return true;
      }
      return false;
   }

   /**
    * update current working directory
    * @param handle terminal handle
    * @param cwd new directory
    */
   private void setCwd(String handle, String cwd)
   {
      ConsoleProcessInfo current = getMetadataForHandle(handle);
      if (current == null)
         return;

      if (current.getCwd() != cwd)
      {
         current.setCwd(cwd);
      }
   }

   /**
    * update zombie flag
    * @param handle terminal handle
    * @param zombie new zombie flag setting
    */
   public void setZombie(String handle, boolean zombie)
   {
      ConsoleProcessInfo current = getMetadataForHandle(handle);
      if (current == null)
         return;
      current.setZombie(zombie);
   }

   public void setExitCode(String handle, int exitCode)
   {
      ConsoleProcessInfo current = getMetadataForHandle(handle);
      if (current == null)
         return;
      current.setExitCode(exitCode);
   }

   /**
    * update caption
    * @param handle terminal handle
    * @param zombie new caption
    */
   public void setCaption(String handle, String caption)
   {
      ConsoleProcessInfo current = getMetadataForHandle(handle);
      if (current == null)
         return;
      current.setCaption(caption);
   }

   /**
    * Remove given terminal from the list
    * @param handle terminal handle
    */
   void removeTerminal(String handle)
   {
      terminals_.remove(handle);
      updateTerminalBusyStatus();
   }

   /**
    * Kill all known terminal server processes, remove them from the server-
    * side list, and from the client-side list.
    */
   void terminateAll()
   {
      for (final java.util.Map.Entry<String, TerminalListData> item : terminals_.entrySet())
      {
         pConsoleProcessFactory_.get().interruptAndReap(item.getValue().getCPI().getHandle());
      }
      terminals_.clear();
      updateTerminalBusyStatus();
   }

   /**
    * Number of terminals in cache.
    * @return number of terminals tracked by this object
    */
   public int terminalCount()
   {
      return terminals_.size();
   }

   /**
    * Return 0-based index of a terminal in the list.
    * @param handle terminal to find
    * @return 0-based index of terminal, -1 if not found
    */
   public int indexOfTerminal(String handle)
   {
      int i = 0;
      for (final java.util.Map.Entry<String, TerminalListData> item : terminals_.entrySet())
      {
         if (item.getValue().getCPI().getHandle().equals(handle))
         {
            return i;
         }
         i++;
      }

      return -1;
   }

   /**
    * Return terminal handle at given 0-based index
    * @param i zero-based index
    * @return handle of terminal at index, or null if invalid index
    */
   public String terminalHandleAtIndex(int i)
   {
      int j = 0;
      for (final java.util.Map.Entry<String, TerminalListData> item : terminals_.entrySet())
      {
         if (i == j)
         {
            return item.getValue().getCPI().getHandle();
         }
         j++;
      }
      return null;
   }

   /**
    * Determine if a caption is already in use
    * @param caption to check
    * @return true if caption is not in use (i.e. a new terminal can use it)
    */
   public boolean isCaptionAvailable(String caption)
   {
      for (final java.util.Map.Entry<String, TerminalListData> item : terminals_.entrySet())
      {
         if (item.getValue().getCPI().getCaption().equals(caption))
         {
            return false;
         }
      }

      return true;
   }

   /**
    * Obtain handle for given caption.
    * @param caption to find
    * @return handle if found, or null
    */
   public String handleForCaption(String caption)
   {
      for (final java.util.Map.Entry<String, TerminalListData> item : terminals_.entrySet())
      {
         if (item.getValue().getCPI().getCaption().equals(caption))
         {
            return item.getValue().getCPI().getHandle();
         }
      }
      return null;
   }
   
   /**
    * Obtain autoclose mode for a given terminal handle.
    * @param handle handle to query
    * @return autoclose mode; if terminal not in list, returns AUTOCLOSE_DEFAULT
    */
   public int autoCloseForHandle(String handle)
   {
      ConsoleProcessInfo meta = getMetadataForHandle(handle);
      if (meta == null)
         return ConsoleProcessInfo.AUTOCLOSE_DEFAULT;
      else
         return meta.getAutoCloseMode();
   }

   /**
    * Get ConsoleProcessInfo for terminal with given handle.
    * @param handle handle of terminal of interest
    * @return terminal metadata or null if not found
    */
   private ConsoleProcessInfo getMetadataForHandle(String handle)
   {
      TerminalListData data = getFullMetadataForHandle(handle);
      if (data == null)
         return null;
      return data.getCPI();
   }

   /**
    * Get metadata for terminal with given handle.
    * @param handle handle of terminal of interest
    * @return terminal metadata or null if not found
    */
   private TerminalListData getFullMetadataForHandle(String handle)
   {
      return terminals_.get(handle);
   }

   /**
    * Initiate startup of a new terminal
    */
   public void createNewTerminal(final ResultCallback<Boolean, String> callback)
   {
      ConsoleProcessInfo info = ConsoleProcessInfo.createNewTerminalInfo(
            uiPrefs_.terminalTrackEnvironment().getValue());
      startTerminal(info, callback);
   }

   /**
    * Reconnect to a known terminal.
    * @param handle
    * @return true if terminal was known and reconnect initiated
    */
   public boolean reconnectTerminal(String handle)
   {
      ConsoleProcessInfo existing = getMetadataForHandle(handle);
      if (existing == null)
      {
         return false;
      }

      existing.setHandle(handle);
      startTerminal(existing, new ResultCallback<Boolean, String>()
      {
         @Override
         public void onSuccess(Boolean connected) 
         {
         }

         @Override
         public void onFailure(String msg)
         {
            Debug.log(msg);
         }
      });

      return true;
   }

   /**
    * @param handle handle to find
    * @return caption for that handle or null if no such handle
    */
   public String getCaption(String handle)
   {
      ConsoleProcessInfo data = getMetadataForHandle(handle);
      if (data == null)
      {
         return null;
      }
      return data.getCaption();
   }

   /**
    * @param handle handle to find
    * @return does terminal have subprocesses
    */
   public boolean getHasSubprocs(String handle)
   {
      ConsoleProcessInfo data = getMetadataForHandle(handle);
      if (data == null)
      {
         return true;
      }
      return data.getHasChildProcs();
   }

   /**
    * @return true if any of the terminal shells have subprocesses
    */
   public boolean haveSubprocs()
   {
      for (final TerminalListData item : terminals_.values())
      {
         if (item.getCPI().getHasChildProcs())
         {
            return true;
         }
      }
      return false;
   }

   private void startTerminal(ConsoleProcessInfo info, 
                              final ResultCallback<Boolean, String> callback)
   {
      // When terminals are created via the R API, guard against creation of multiple
      // TerminalSession objects for the same terminal. For terminals created via the
      // UI, we already guard against this via TerminalPane.creatingTerminal_.
      TerminalListData existing = getFullMetadataForHandle(info.getHandle());
      if (existing != null && existing.getSessionCreated())
      {
         callback.onSuccess(false);
         return;
      }

      TerminalSession newSession = new TerminalSession(
            info, uiPrefs_.blinkingCursor().getValue(), true /*focus*/);

      if (existing != null)
      {
         existing.setSessionCreated();
      }
      newSession.connect(callback);
      updateTerminalBusyStatus();
   }

   private void updateTerminalBusyStatus()
   {
      eventBus_.fireEvent(new TerminalBusyEvent(haveSubprocs()));
   }

   @Override
   public Iterator<String> iterator()
   {
      return terminals_.keySet().iterator();
   }

   @Override
   public void onTerminalSubprocs(TerminalSubprocEvent event)
   {
      setChildProcs(event.getHandle(), event.hasSubprocs());
      updateTerminalBusyStatus();
   }

   @Override
   public void onTerminalCwd(TerminalCwdEvent event)
   {
      setCwd(event.getHandle(), event.getCwd());
   }
   
   public String debug_dumpTerminalList()
   {
      StringBuilder dump = new StringBuilder();
     
      dump.append("Terminal List Count: ");
      dump.append(terminalCount());
      dump.append("\n");
      for (int i = 0; i < terminalCount(); i++)
      {
         dump.append("Handle: '");
         String handle = terminalHandleAtIndex(i);
         dump.append(handle);
         dump.append("' Caption: '");

         TerminalListData data = getFullMetadataForHandle(handle);
         dump.append(data.getCPI().getCaption());
         dump.append("' Session Created: ");
         dump.append(data.getSessionCreated());
         dump.append("\n");
      }
      return dump.toString();
   }

   /**
    * Map of terminal handles to terminal metadata; order they are added
    * is the order they will be iterated.
    */
   private LinkedHashMap<String, TerminalListData> terminals_ = 
                new LinkedHashMap<String, TerminalListData>();

   // Injected ----  
   private Provider<ConsoleProcessFactory> pConsoleProcessFactory_;
   private EventBus eventBus_;
   private UIPrefs uiPrefs_;
}