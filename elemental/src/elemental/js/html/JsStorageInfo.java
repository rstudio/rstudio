/*
 * Copyright 2012 Google Inc.
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
package elemental.js.html;
import elemental.html.StorageInfo;
import elemental.html.StorageInfoQuotaCallback;
import elemental.html.StorageInfoUsageCallback;
import elemental.html.StorageInfoErrorCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsStorageInfo extends JsElementalMixinBase  implements StorageInfo {
  protected JsStorageInfo() {}

  public final native void queryUsageAndQuota(int storageType) /*-{
    this.queryUsageAndQuota(storageType);
  }-*/;

  public final native void queryUsageAndQuota(int storageType, StorageInfoUsageCallback usageCallback) /*-{
    this.queryUsageAndQuota(storageType, $entry(usageCallback.@elemental.html.StorageInfoUsageCallback::onStorageInfoUsageCallback(DD)).bind(usageCallback));
  }-*/;

  public final native void queryUsageAndQuota(int storageType, StorageInfoUsageCallback usageCallback, StorageInfoErrorCallback errorCallback) /*-{
    this.queryUsageAndQuota(storageType, $entry(usageCallback.@elemental.html.StorageInfoUsageCallback::onStorageInfoUsageCallback(DD)).bind(usageCallback), $entry(errorCallback.@elemental.html.StorageInfoErrorCallback::onStorageInfoErrorCallback(Lelemental/dom/DOMException;)).bind(errorCallback));
  }-*/;

  public final native void requestQuota(int storageType, double newQuotaInBytes) /*-{
    this.requestQuota(storageType, newQuotaInBytes);
  }-*/;

  public final native void requestQuota(int storageType, double newQuotaInBytes, StorageInfoQuotaCallback quotaCallback) /*-{
    this.requestQuota(storageType, newQuotaInBytes, $entry(quotaCallback.@elemental.html.StorageInfoQuotaCallback::onStorageInfoQuotaCallback(D)).bind(quotaCallback));
  }-*/;

  public final native void requestQuota(int storageType, double newQuotaInBytes, StorageInfoQuotaCallback quotaCallback, StorageInfoErrorCallback errorCallback) /*-{
    this.requestQuota(storageType, newQuotaInBytes, $entry(quotaCallback.@elemental.html.StorageInfoQuotaCallback::onStorageInfoQuotaCallback(D)).bind(quotaCallback), $entry(errorCallback.@elemental.html.StorageInfoErrorCallback::onStorageInfoErrorCallback(Lelemental/dom/DOMException;)).bind(errorCallback));
  }-*/;
}
