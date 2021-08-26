/**
 * 
 * url-verifier.ts
 * 
 * Copyright (C) 2021 by RStudio, PBC
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

import { BrowserWindow, shell } from 'electron';
import { URL } from 'url';

export class UrlVerifier {
  
  private safeHosts: string[];

  constructor(
    private window: BrowserWindow,
    private baseUrl?: string,
    private viewerUrl?: string,
    private tutorialUrl?: string,
    private shinyDialogUrl?: string,
  ) {
    this.safeHosts = [
      '.youtube.com',
      '.vimeo.com',
      '.c9.ms',
      '.google.com'
    ];

    //TODO
    // for (const SessionServer& server : sessionServerSettings().servers())
    // {
    //     http::URL url(server.url());
    //     safeHosts_.push_back(url.hostname());
    // }
  }

  public acceptNavigationRequest(origUrl: string, allowExternalNavigate: boolean): boolean {
    
    let url: URL;
    try {
      url = new URL(origUrl);
    } catch (err) {
      // malformed URL will cause exception
      return false;
    }

    if (origUrl === 'about:blank') {
      return true;
    }
    if (origUrl === 'chrome://gpu/'){
      return true;
    }
    if (url.protocol === 'data:') {
      return true;
    }
  
    if (!(['http:', 'https:', 'mailto:'].includes(url.protocol))) {
      return false;
    }
  
    const isLocal = this.isUrlLocal(url);
    if (isLocal && url.port === '5858') {
      return true;
    }// TODO get chromiumDevtoolsPort programatically?

    if (isLocal &&
      ['/recompile',
        '/rstudio'].some((element) => {return url.pathname.startsWith(element);})) {
      return true;
    }
  
    // if this appears to be an attempt to load an external page within
    // an iframe, check it's from a safe host
    if (!isLocal) {
      const framesWithUrl = this.window.webContents.mainFrame.frames.filter((frame) => {
        try {
          return url.toString() === frame.url;
        } catch {
          return false;
        }
      });

      if (framesWithUrl.length > 0) {
        return this.isExternalUrlSafe(url);
      }
    }
  
    if (this.baseUrl) {
      const baseUrl = new URL(this.baseUrl);
      if ((this.baseUrl.length === 0 && isLocal) ||
          (url.protocol === baseUrl.protocol &&
          url.host == baseUrl.host &&
          url.port == baseUrl.port))
      {
        return true;
      }
    }
    // allow viewer urls to be handled internally by Qt. note that the client is responsible for 
    // ensuring that non-local viewer urls are appropriately sandboxed.
    else if (this.viewerUrl?.length &&
            origUrl.startsWith(this.viewerUrl))
    {
      return true;
    }
    // allow tutorial urls to be handled internally by Qt. note that the client is responsible for 
    // ensuring that non-local tutorial urls are appropriately sandboxed.
    else if (this.tutorialUrl?.length &&
            origUrl.startsWith(this.tutorialUrl))
    {
      return true;
    }
    // allow shiny dialog urls to be handled internally by Qt
    else if (isLocal && this.shinyDialogUrl?.length &&
            origUrl.startsWith(this.shinyDialogUrl))
    {
      return true;
    }
    else
    {
      let navigated = false;
      
      if (allowExternalNavigate)
      {
        // if allowing external navigation, follow this (even if a link click)
        return true;
      }
      else if (this.isExternalUrlSafe(url))
      {
        return true;
      }
      else
      {
        // when not allowing external navigation, open an external browser
        // to view the URL
        void shell.openExternal(origUrl);
        navigated = true;
      }

      if (!navigated){
        // this->view()->window()->deleteLater();
        this.window.close();
      }
    }
    return false;
  }

  public getBaseUrl(): string {
    if (this.baseUrl) {
      return this.baseUrl;
    } else {
      return '';
    }
  }

  public setBaseUrl(value: string): void {
    this.baseUrl = value;
  }

  public getViewerUrl(): string {
    if (this.viewerUrl) {
      return this.viewerUrl;
    } else {
      return '';
    }
  }

  public setViewerUrl(value: string): void {
    this.viewerUrl = value;
  }

  public getTutorialUrl(): string {
    if (this.tutorialUrl) {
      return this.tutorialUrl;
    } else {
      return '';
    }
  }

  public setTutorialUrl(value: string): void {
    this.tutorialUrl = value;
  }

  public getShinyDialogUrl(): string {
    if (this.shinyDialogUrl) {
      return this.shinyDialogUrl;
    } else {
      return '';
    }
  }

  public setShinyDialogUrl(value: string): void {
    this.shinyDialogUrl = value;
  }

  private isUrlLocal(url: URL): boolean {
    return ['localhost', '127.0.0.1', '::1'].includes(url.hostname);
  }

  private isExternalUrlSafe(url: URL): boolean {
    return this.safeHosts.some((element: string): boolean => {
      return element.endsWith(url.hostname);
    });
  }
}