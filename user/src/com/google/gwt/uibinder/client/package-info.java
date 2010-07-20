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

/**
 * Classes used to generate user interfaces using declarative ui.xml files.
 * 
 * <p>
 * This package contains the classes and interfaces that allow you to define
 * user interfaces from ui.xml template files, managed by generated
 * implementations of the {@link com.google.gwt.uibinder.client.UiBinder UiBinder}
 * interface. UiBinder templates allow you to lay out your widgets and design
 * new ones via HTML, CSS and Image resources (the last two via generated
 * {@link com.google.gwt.resources.client.ClientBundle ClientBundles}) with a
 * minimum of coding. They also have extensive support for internationalization,
 * by generating {@link com.google.gwt.i18n.client.Messages Messages}.
 * </p>
 * 
 * <p>
 * Follow the links below for general documentation. Specialized markup for
 * individual widget types is described in their javadoc. In particular, see
 * {@link com.google.gwt.user.client.ui.UIObject UIObject} for markup that
 * applies to all widgets.
 * </p>
 * 
 * @see <a href="http://code.google.com/webtoolkit/doc/latest/DevGuideUiBinder.html">Declarative Layout with UiBinder</a>
 * @see <a href="http://code.google.com/webtoolkit/doc/latest/DevGuideUiBinderI18n.html">Internationalization - UiBinder</A>
 */
@com.google.gwt.util.PreventSpuriousRebuilds
package com.google.gwt.uibinder.client;
