/**
 * Page object for the Connections pane: the connection list, its toolbar,
 * and the connection explorer (the object tree shown while a connection is
 * being explored).
 *
 * Selector notes:
 *  - The pane/tab and the four toolbar commands have stable RStudio ids.
 *    Toolbar-button ids use the [id^=...] prefix form because
 *    ElementIds.assignElementId appends a uniqueness suffix on collision.
 *  - The list DataGrid, the explorer CellTree, and the search boxes have no
 *    ids and obfuscated GWT class names, so they are addressed by structure,
 *    accessible name, or title, always scoped to the pane.
 *  - The list and explorer live in a SlidingLayoutPanel that keeps BOTH
 *    halves in the DOM; before the first slide the hidden half is parked at
 *    left:-5000px and still reports visible. Assertions about "which view is
 *    showing" therefore key off view-specific toolbar content (the explorer
 *    toolbar's back button) rather than raw visibility of the halves.
 */

import { Locator, Page } from '@playwright/test';
import { PageObject } from './page_object_base_classes';

export const CONNECTIONS_PANEL = '#rstudio_workbench_panel_connections';
export const CONNECTIONS_TAB = '#rstudio_workbench_tab_connections';
export const NEW_CONNECTION_BTN = "[id^='rstudio_tb_newconnection']";
export const REMOVE_CONNECTION_BTN = "[id^='rstudio_tb_removeconnection']";
export const DISCONNECT_BTN = "[id^='rstudio_tb_disconnectconnection']";
export const REFRESH_CONNECTION_BTN = "[id^='rstudio_tb_refreshconnection']";

export class ConnectionsPane extends PageObject {
  public panel: Locator;
  public tab: Locator;
  public newConnectionBtn: Locator;
  public removeConnectionBtn: Locator;
  public disconnectBtn: Locator;
  public refreshConnectionBtn: Locator;
  /** Explorer-toolbar button returning to the connection list. */
  public backToConnectionsBtn: Locator;
  /** Search box filtering the connection list. */
  public filterConnections: Locator;
  /** Search box filtering the explorer's object tree. */
  public filterObjects: Locator;

  constructor(page: Page) {
    super(page);
    this.panel = page.locator(CONNECTIONS_PANEL);
    this.tab = page.locator(CONNECTIONS_TAB);
    this.newConnectionBtn = page.locator(NEW_CONNECTION_BTN);
    this.removeConnectionBtn = page.locator(REMOVE_CONNECTION_BTN);
    this.disconnectBtn = page.locator(DISCONNECT_BTN);
    this.refreshConnectionBtn = page.locator(REFRESH_CONNECTION_BTN);
    this.backToConnectionsBtn = this.panel.locator('[title="View all connections"]');
    this.filterConnections = page.getByLabel('Filter by connection');
    this.filterObjects = page.getByLabel('Filter by object');
  }

  /** A row of the connection list containing the given text. */
  connectionRow(text: string): Locator {
    return this.panel.locator('tr', { hasText: text });
  }

  /** The row's explore button (ImageButtonColumn renders a titled span). */
  exploreButton(rowText: string): Locator {
    return this.connectionRow(rowText).locator('span[title="Explore connection"]');
  }

  /**
   * The expand/collapse toggle beside an object-tree node. The CellTree's
   * class names are obfuscated; its toggle is the sibling image (a
   * clear.cache.gif sprite), matched relative to the node's text. Same
   * approach the retired Selene suite used for years.
   */
  expandToggle(nodeText: string): Locator {
    return this.panel.locator(
      `xpath=//*[text() = '${nodeText}']/../..//img[contains(@src, 'clear.cache.gif')]`,
    );
  }

  /** The "view table" zoom icon beside a table node in the object tree. */
  viewTableIcon(tableText: string): Locator {
    return this.panel
      .locator('tr', { hasText: tableText })
      .locator('img[title="View table (up to 1,000 records)"]');
  }
}
