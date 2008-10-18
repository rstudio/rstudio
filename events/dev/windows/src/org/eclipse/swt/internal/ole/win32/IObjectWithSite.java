/*
 * Copyright 2007 Google Inc.
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
package org.eclipse.swt.internal.ole.win32;

/**
 * Provide the interface IObjectWithSite, a lighter-weight siting mechanism
 * than IOleObject.
 * 
 * This interface is used by IE Browser Helper Objects, such as Google Gears.
 * See http://msdn2.microsoft.com/en-us/library/aa768220.aspx
 */
public class IObjectWithSite extends IUnknown {
  public IObjectWithSite(int address) {
    super(address);
  }
  
  /**
   * Return the last site set with SetSite.
   * 
   * @param riid IID of the interface to be returned
   * @param ppvObject return value of the IObjectWithSite interface
   * @return COM.S_OK on success, COM.E_FAIL if no site has been set, or
   *     COM.E_NOINTERFACE if the requested interface is not supported
   */
  public int GetSite(GUID riid, int ppvObject[]) {
    return COM.VtblCall(4, address, riid, ppvObject);
  }
  
  /**
   * Sets the IUnknown interface of the site managing this object.
   * 
   * @param site an IUnknown interface to the browser object
   * @return COM.S_OK always
   */
  public int SetSite(IUnknown site) {
    return COM.VtblCall(3, address, site.getAddress());
  }
}
