/*
 * TerminalList.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.console.ConsoleProcess.ConsoleProcessFactory;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalBusyEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalCwdEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalReceivedConsoleProcessInfoEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSubprocEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * List of terminals, with sufficient metadata to display a list of
 * available terminals and reconnect to them.
 */
public class TerminalList implements Iterable<String>,
                                     TerminalCwdEvent.Handler,
                                     TerminalReceivedConsoleProcessInfoEvent.Handler
{
   private static class TerminalListData
   {
      TerminalListData(ConsoleProcessInfo cpi, boolean hasSession)
      {
         cpi_ = cpi;
         sessionCreated_ = hasSession;
      }

      ConsoleProcessInfo getCPI()
      {
         return cpi_;
      }

      void setSessionCreated()
      {
         sessionCreated_ = true;
      }

      boolean getSessionCreated()
      {
         return sessionCreated_;
      }

      private final ConsoleProcessInfo cpi_;
      private boolean sessionCreated_;
   }

   protected TerminalList()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      eventBus_.addHandler(TerminalCwdEvent.TYPE, this);
      eventBus_.addHandler(TerminalReceivedConsoleProcessInfoEvent.TYPE, this);
   }

   @Inject
   private void initialize(Provider<ConsoleProcessFactory> pConsoleProcessFactory,
                           EventBus events)
   {
      pConsoleProcessFactory_ = pConsoleProcessFactory;
      eventBus_ = events;
   }

   /**
    * Add metadata from supplied TerminalSession
    * @param term terminal to add
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

      if (!StringUtil.equals(current.getTitle(), title))
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
    */
   private void setChildProcs(String handle, boolean childProcs)
   {
      ConsoleProcessInfo current = getMetadataForHandle(handle);
      if (current == null)
         return;

      if (current.getHasChildProcs() != childProcs)
      {
         current.setHasChildProcs(childProcs);
      }
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

      if (!StringUtil.equals(current.getCwd(), cwd))
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
    * @param caption new caption
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
         if (StringUtil.equals(item.getValue().getCPI().getHandle(), handle))
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
    * Obtain handle for given caption.
    * @param caption to find
    * @return handle if found, or null
    */
   public String handleForCaption(String caption)
   {
      for (final java.util.Map.Entry<String, TerminalListData> item : terminals_.entrySet())
      {
         if (StringUtil.equals(item.getValue().getCPI().getCaption(), caption))
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
   public ConsoleProcessInfo getMetadataForHandle(String handle)
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
      if (BrowseCap.isWindowsDesktop())
         return false;

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
      if (BrowseCap.isWindowsDesktop())
         return false;

      for (final TerminalListData item : terminals_.values())
      {
         if (item.getCPI().getHasChildProcs())
         {
            return true;
         }
      }
      return false;
   }

   /**
    * Orchestrates the creation and connection of a terminal.
    *
    * @param session TerminalSession widget to contain the terminal emulator. Xterm.js requires
    *                the Widget's element be visible (have dimensions)
    * @param callback done, Boolean if successful, String if failed
    */
   public void startTerminal(TerminalSession session,
                             final ResultCallback<Boolean, String> callback)
   {
      // When terminals are created via the R API, guard against creation of multiple
      // TerminalSession objects for the same terminal. For terminals created via the
      // UI, we already guard against this via TerminalPane.creatingTerminal_.
      TerminalListData existing = getFullMetadataForHandle(session.getProcInfo().getHandle());
      if (existing != null && existing.getSessionCreated())
      {
         callback.onSuccess(false);
         return;
      }

      // initialize xterm.js
      session.open(() -> {
         if (existing != null)
         {
            existing.setSessionCreated();
         }

         // connect the emulator to server-side process
         session.connect(new ResultCallback<Boolean, String>()
         {
            @Override
            public void onSuccess(Boolean connected)
            {
               if (connected)
                  updateTerminalBusyStatus();
               callback.onSuccess(connected);
            }

            @Override
            public void onFailure(String msg)
            {
               callback.onFailure(msg);
            }
         });
      });
   }

   /**
    * Broadcast event which indicates if any terminals are busy.
    */
   private void updateTerminalBusyStatus()
   {
      eventBus_.fireEvent(new TerminalBusyEvent(haveSubprocs()));
   }

   @Override
   public Iterator<String> iterator()
   {
      return terminals_.keySet().iterator();
   }

   public void updateTerminalSubprocsStatus(TerminalSubprocEvent event)
   {
      setChildProcs(event.getHandle(), event.hasSubprocs());
      updateTerminalBusyStatus();
   }

   @Override
   public void onTerminalCwd(TerminalCwdEvent event)
   {
      setCwd(event.getHandle(), event.getCwd());
   }

   @Override
   public void onTerminalReceivedConsoleProcessInfo(TerminalReceivedConsoleProcessInfoEvent event)
   {
      addTerminal(event.getData(), true);
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
   private final LinkedHashMap<String, TerminalListData> terminals_ = new LinkedHashMap<>();

   // Injected ----
   private Provider<ConsoleProcessFactory> pConsoleProcessFactory_;
   private EventBus eventBus_;
}
