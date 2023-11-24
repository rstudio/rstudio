/*
 * typed-event-emitter.ts
 *
 * Copyright (C) 2023 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// Thanks to Wade Baglin for this handy class:
// https://blog.makerx.com.au/a-type-safe-event-emitter-in-node-js/

import EventEmitter from 'events';

/* eslint-disable @typescript-eslint/no-explicit-any */
export class TypedEventEmitter<TEvents extends Record<string, any>> {
  private emitter = new EventEmitter();

  emit<TEventName extends keyof TEvents & string>(
    eventName: TEventName, ...eventArg: TEvents[TEventName]
  ): boolean {
    return this.emitter.emit(eventName, ...(eventArg as []));
  }

  on<TEventName extends keyof TEvents & string>(
    eventName: TEventName, handler: (...eventArg: TEvents[TEventName]) => void
  ) {
    this.emitter.on(eventName, handler as any);
    return this;
  }

  off<TEventName extends keyof TEvents & string>(
    eventName: TEventName, handler: (...eventArg: TEvents[TEventName]) => void
  ) {
    this.emitter.removeListener(eventName, handler as any);
    return this;
  }

  once<TEventName extends keyof TEvents & string>(
    eventName: TEventName, handler: (...eventArg: TEvents[TEventName]) => void
  ) {
    this.emitter.once(eventName, handler as any);
    return this;
  }

  listenerCount<TEventName extends keyof TEvents & string>(eventName: TEventName): number {
    return this.emitter.listenerCount(eventName);
  }

  removeAllListeners<TEventName extends keyof TEvents & string>(eventName: TEventName) {
    this.emitter.removeAllListeners(eventName);
    return this;
  }
}