/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.SwingUI.TabPanelCollection;
import com.google.gwt.dev.shell.WrapLayout;
import com.google.gwt.dev.util.BrowserInfo;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * A panel which contains all modules in one browser tab.
 */
public class ModuleTabPanel extends JPanel {

  /**
   * A session has a unique session key within a module tab panel, and is
   * identified to the user by the timestamp it was first seen.
   *
   * <p>Within a session, there will be one or more modules, each with their
   * own ModulePanel.
   */
  public class Session {

    private final long createTimestamp;

    /**
     * Map from display names in the dropdown box to module panels.
     */
    private final Map<String, ModulePanel> displayNameToModule;

    private final IdentityHashMap<ModulePanel, SessionModule> moduleSessionMap;

    private SessionModule lastSelectedModule;

    /**
     * Map of module names to the number of times that module has been seen.
     */
    private final Map<String, Integer> moduleCounts;

    /**
     * List, in display order, of entries in the module dropdown box.
     */
    private final List<SessionModule> modules;

    private final String sessionKey;

    public Session(String sessionKey) {
      this.sessionKey = sessionKey;
      createTimestamp = System.currentTimeMillis();
      displayNameToModule = new HashMap<String, ModulePanel>();
      moduleSessionMap = new IdentityHashMap<ModulePanel, SessionModule>();
      modules = new ArrayList<SessionModule>();
      moduleCounts = new HashMap<String, Integer>();
    }

    public synchronized void addModule(String moduleName,
        ModulePanel panel) {
      Integer moduleCount = moduleCounts.get(moduleName);
      if (moduleCount == null) {
        moduleCount = 0;
      }
      moduleCounts.put(moduleName, moduleCount + 1);
      String shortModuleName = getShortModuleName(moduleName);
      if (moduleCount > 0) {
        shortModuleName += " (" + moduleCount + ")";
      }
      SessionModule sessionModule = SessionModule.create(sessionKey,
          panel, shortModuleName);
      modules.add(sessionModule);
      displayNameToModule.put(shortModuleName, panel);
      moduleSessionMap.put(panel, sessionModule);
      // add this item with the key we will use with cardLayout later
      deckPanel.add(panel, sessionModule.getStringKey());
      if (this == currentSession) {
        moduleDropdown.addItem(sessionModule);
        if (moduleDropdown.getItemCount() > 1) {
          moduleDropdownPanel.setEnabled(true);
          moduleDropdownPanel.setVisible(true);
        }
        selectModule(sessionModule);
      }
    }

    public void buildModuleDropdownContents() {
      if (this == currentSession) {
        moduleDropdown.removeAllItems();
        SessionModule firstModule = null;
        for (SessionModule sessionModule : modules) {
         moduleDropdown.addItem(sessionModule);
          if (firstModule == null) {
            firstModule = sessionModule;
          }
        }
        if (moduleDropdown.getItemCount() > 1) {
          moduleDropdownPanel.setEnabled(true);
          moduleDropdownPanel.setVisible(true);
        } else {
          moduleDropdownPanel.setEnabled(false);
          moduleDropdownPanel.setVisible(false);
        }
        if (lastSelectedModule != null) {
          selectModule(lastSelectedModule);
        } else if (firstModule != null) {
          selectModule(firstModule);
        }
      }
    }

    public void disconnectModule(ModulePanel modulePanel) {
      /*
       * TODO(jat): for now, only disconnected modules can be closed.  When
       *     SWT is ripped out and we can pass OOPHM-specific classes through
       *     BrowseWidgetHost.createModuleSpaceHost, we will need to be able
       *     to shutdown the connection from here if it is not already
       *     disconnected.
       */
      SessionModule sessionModule = moduleSessionMap.get(modulePanel);
      moduleSessionMap.remove(modulePanel);
      deckPanel.remove(modulePanel);
      modules.remove(sessionModule);
      switch (modules.size()) {
        case 0: // we just closed the last module in this session
          closeSession(this);
          break;
        case 1: // only one module left, hide dropdown
          moduleDropdownPanel.setEnabled(false);
          moduleDropdownPanel.setVisible(false);
          break;
        default:
          if (lastSelectedModule == sessionModule) {
            // if we closed the active module, switch to the most recent remaining
            lastSelectedModule = modules.get(modules.size() - 1);
          }
          buildModuleDropdownContents();
          break;
      }
    }

    public Collection<String> getActiveModules() {
      ArrayList<String> activeModules = new ArrayList<String>();
      for (SessionModule sessionModule : modules) {
        String displayName = sessionModule.toString();
        Disconnectable module = sessionModule.getModulePanel();
        if (!module.isDisconnected()) {
          activeModules.add(displayName);
        }
      }
      return Collections.unmodifiableList(activeModules);
    }

    public String getDisplayName() {
      return DateFormat.getDateTimeInstance().format(new Date(createTimestamp));
    }

    public String getSessionKey() {
      return sessionKey;
    }

    public boolean hasActiveModules() {
      for (SessionModule sessionModule : modules) {
        Disconnectable module = sessionModule.getModulePanel();
        if (!module.isDisconnected()) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return getDisplayName();
    }

    private String getShortModuleName(String moduleName) {
      int idx = moduleName.lastIndexOf('.');
      if (idx < 0) {
        return moduleName;
      } else {
        return moduleName.substring(idx + 1);
      }
    }

    private void selectModule(SessionModule sessionModule) {
      cardLayout.show(deckPanel, sessionModule.getStringKey());
      lastSelectedModule = sessionModule;
      moduleDropdown.setSelectedItem(sessionModule);
    }
  }

  /**
   * Renderer used to show entries in the module dropdown box.
   */
  private static class SessionModuleRenderer extends BasicComboBoxRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      // the superclass just returns this, so we don't save the result and
      // cast it back to a label
      super.getListCellRendererComponent(list, value, index,
          isSelected, cellHasFocus);
      if (value instanceof SessionModule) {
        SessionModule sessionModule = (SessionModule) value;
        if (sessionModule.getModulePanel().isDisconnected()) {
          setForeground(DISCONNECTED_DROPDOWN_COLOR);
          setFont(getFont().deriveFont(Font.ITALIC));
        }
        // TODO(jat): set font to bold/etc if the window has new messages
      }
      return this;
    }
  }

  /**
   * Renderer used to show entries in the session dropdown box.
   */
  private static class SessionRenderer extends BasicComboBoxRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      // the superclass just returns this, so we don't save the result and
      // cast it back to a label
      super.getListCellRendererComponent(list, value, index,
          isSelected, cellHasFocus);
      if (value instanceof Session) {
        Session session = (Session) value;
        if (!session.hasActiveModules()) {
          setForeground(DISCONNECTED_DROPDOWN_COLOR);
          setFont(getFont().deriveFont(Font.ITALIC));
        }
        // TODO(jat): set font to bold/etc if new modules were added
      }
      return this;
    }
  }

  public static final Color DISCONNECTED_DROPDOWN_COLOR = Color.decode("0x808080");

  private CardLayout cardLayout;

  private Session currentSession;

  private JPanel deckPanel;

  private JComboBox moduleDropdown;

  private JComboBox sessionDropdown;

  private final Map<String, Session> sessions = new HashMap<String, Session>();

  private final TabPanelCollection tabPanelCollection;

  private JPanel topPanel;

  private JPanel sessionDropdownPanel;

  private JPanel moduleDropdownPanel;

  /**
   * Create a panel which will be a top-level tab in the OOPHM UI.  Each of
   * these tabs will contain one or more sessions, and within that one or
   * more module instances.
   *
   * @param userAgent
   * @param remoteSocket
   * @param url
   * @param agentIconBytes
   * @param tabPanelCollection
   * @param moduleName used just for the tab name in the event that the plugin
   *     is an older version that doesn't supply the url
   */
  public ModuleTabPanel(String userAgent, String remoteSocket, String url,
      byte[] agentIconBytes, TabPanelCollection tabPanelCollection,
      String moduleName) {
    super(new BorderLayout());
    this.tabPanelCollection = tabPanelCollection;
    topPanel = new JPanel();
    sessionDropdownPanel = new JPanel(new WrapLayout());
    sessionDropdownPanel.add(new JLabel("Session: "));
    sessionDropdown = new JComboBox();
    sessionDropdown.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Session session = (Session) sessionDropdown.getSelectedItem();
        selectSession(session);
      }

    });
    sessionDropdown.setRenderer(new SessionRenderer());
    sessionDropdownPanel.add(sessionDropdown);
    sessionDropdownPanel.setEnabled(false);
    sessionDropdownPanel.setVisible(false);
    topPanel.add(sessionDropdownPanel);
    moduleDropdownPanel = new JPanel(new WrapLayout());
    moduleDropdownPanel.add(new JLabel("Module: "));
    moduleDropdown = new JComboBox();
    moduleDropdown.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SessionModule sessionModule = (SessionModule)
            moduleDropdown.getSelectedItem();
        if (sessionModule != null) {
          // may be null when removeAllItems is called
          currentSession.selectModule(sessionModule);
        }
      }
    });
    moduleDropdown.setRenderer(new SessionModuleRenderer());
    moduleDropdownPanel.add(moduleDropdown);
    moduleDropdownPanel.setEnabled(false);
    moduleDropdownPanel.setVisible(false);
    topPanel.add(moduleDropdownPanel);
    add(topPanel, BorderLayout.NORTH);
    cardLayout = new CardLayout();
    deckPanel = new JPanel(cardLayout);
    add(deckPanel);

    // Construct the tab title and tooltip
    String tabTitle = url;
    if (tabTitle == null) {
      int idx = moduleName.lastIndexOf('.');
      tabTitle = moduleName.substring(idx + 1);
      url = "";
    } else {
      try {
        URL parsedUrl = new URL(url);
        tabTitle = getTabTitle(parsedUrl);
        // rebuild the URL omitting query params and the hash
        StringBuilder buf = new StringBuilder();
        buf.append(parsedUrl.getProtocol()).append(':');
        if (parsedUrl.getAuthority() != null
            && parsedUrl.getAuthority().length() > 0) {
          buf.append("//").append(parsedUrl.getAuthority());
        }
        if (parsedUrl.getPath() != null) {
          buf.append(parsedUrl.getPath());
        }
        buf.append(' '); // space for tooltip below
        url = buf.toString();
      } catch (MalformedURLException e1) {
        // Ignore and just use the full URL
      }
    }

    ImageIcon browserIcon = null;
    if (agentIconBytes != null) {
      browserIcon = new ImageIcon(agentIconBytes);
    }
    String shortName = BrowserInfo.getShortName(userAgent);
    if (browserIcon == null) {
      if (shortName != null) {
        tabTitle += " (" + shortName + ")";
      }
    }
    tabPanelCollection.addTab(this, browserIcon, tabTitle, url + "from "
        + remoteSocket + " on " + userAgent);
  }

  public synchronized ModulePanel addModuleSession(Type maxLevel,
      String moduleName,
      String sessionKey,
      File logFile) {
    Session session = findOrCreateSession(sessionKey);

    ModulePanel panel = new ModulePanel(maxLevel, moduleName, session, logFile);
    return panel;
  }

  private synchronized void addSession(Session session) {
    sessionDropdown.addItem(session);
    sessionDropdown.setSelectedItem(session);
    if (sessionDropdown.getItemCount() > 1) {
      sessionDropdownPanel.setEnabled(true);
      sessionDropdownPanel.setVisible(true);
    }
    selectSession(session);
  }

  private synchronized void closeSession(Session session) {
    sessionDropdown.removeItem(session);
    sessions.remove(session.getSessionKey());
    switch (sessionDropdown.getItemCount()) {
      case 0: // last session closed, close tab
        tabPanelCollection.removeTab(this);
        return;
      case 1: // one session left, remove dropdown
        sessionDropdownPanel.setEnabled(false);
        sessionDropdownPanel.setVisible(false);
        break;
      default: // more than 1 left, do nothing
        break;
    }
    if (session == currentSession) {
      selectSession((Session) sessionDropdown.getItemAt(
          sessionDropdown.getItemCount() - 1));
    }
  }

  /**
   * Return the proper Session object for this session, creating it if needed.
   *
   * @param sessionKey unique key for this session
   * @return Session instance
   */
  private synchronized Session findOrCreateSession(String sessionKey) {
    Session session = sessions.get(sessionKey);
    if (session == null) {
      session = new Session(sessionKey);
      sessions.put(sessionKey, session);
      addSession(session);
    }
    return session;
  }

  private String getTabTitle(URL parsedUrl) {
    String tabTitle = parsedUrl.getPath();
    if (tabTitle.length() > 0) {
      int startIdx = tabTitle.lastIndexOf('/');
      int lastIdx = tabTitle.length();
      if (tabTitle.endsWith(".html")) {
        lastIdx -= 5;
      } else if (tabTitle.endsWith(".htm")) {
        lastIdx -= 4;
      }
      tabTitle = tabTitle.substring(startIdx + 1, lastIdx);
    } else {
      tabTitle = "/";
    }
    return tabTitle;
  }

  private void selectSession(Session session) {
    currentSession = session;
    if (session != null) {
      // can be null when last session removed
      session.buildModuleDropdownContents();
    }
  }
}
