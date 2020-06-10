/*
 * event-types.ts
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

import { makeEventType } from './events';
import { Transaction } from 'prosemirror-state';

export const UpdateEvent = makeEventType('Update');
export const OutlineChangeEvent = makeEventType('OutlineChange');
export const StateChangeEvent = makeEventType('StateChange');
export const ResizeEvent = makeEventType('Resize');
export const LayoutEvent = makeEventType('Layout');
export const ScrollEvent = makeEventType('Scroll');
export const FocusEvent = makeEventType('Focus');
export const DispatchEvent = makeEventType<Transaction>('Dispatch');
