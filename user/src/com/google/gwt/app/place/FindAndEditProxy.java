/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.app.place;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Extends {@link AbstractProxyEditActivity} to work from a {@link EntityProxyId}
 * 
 * @param <P> the type of proxy to find and edit
 */
public abstract class FindAndEditProxy<P extends EntityProxy> extends
    AbstractProxyEditActivity<P> {

  private final RequestFactory factory;
  private final EntityProxyId<P> proxyId;
  private P proxy;

  public FindAndEditProxy(EntityProxyId<P> proxyId, RequestFactory factory,
      ProxyEditView<P, ?> view, PlaceController placeController) {
    super(view, placeController);
    this.proxyId = proxyId;
    this.factory = factory;
  }

  @Override
  public void start(final AcceptsOneWidget display, final EventBus eventBus) {
    factory.find(proxyId).fire(new Receiver<P>() {
      @Override
      public void onSuccess(P response) {
        proxy = response;
        FindAndEditProxy.super.start(display, eventBus);
      }
    });
  }

  @Override
  protected P getProxy() {
    return proxy;
  }
}
