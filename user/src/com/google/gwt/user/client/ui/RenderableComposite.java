/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.user.client.ui;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * A version of {@link Composite} that supports wrapping {@link Renderable}
 * widgets. This functionality will eventually be merged into {@link Composite}
 * itself, but is still under active development.
 * The only reason why this isn't a subclass of {@link Composite} is to avoid
 * messing up it's API, since {@link Composite} is very often subclassed.
 *
 * TODO(rdcastro): Delete this as soon as all references have been updated to
 * use Composite directly.
 */
public abstract class RenderableComposite extends Composite {
}
