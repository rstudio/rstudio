/*
 * events.ts
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

/**
 * Represents an event type; only a single instance of this should exist per
 * event type (akin to PluginKey) and it should be visible to everyone who wants
 * to subscribe to or emit events of that type. Do not create one of these
 * directly, instead use makeEventType().
 */
export interface EventType<TDetail> {
  readonly eventName: string;
  // This field is needed only to prevent TDetail from being ignored by the type
  // checker; if TDetail isn't used, tsc acts as if EventType isn't generic.
  readonly dummy?: TDetail;
}

/**
 * Type signature of event-handler functions; the TDetail must match with the
 * EventType<TDetail> being subscribed to.
 *
 * (Note that the detail is always optional. I couldn't figure out how to make
 * it mandatory for some event types, forbidden for others, and optional for
 * still others, so it's just optional for everyone.)
 */
export type EventHandler<TDetail> = (detail?: TDetail) => void;

/**
 * Generic interface for objects that support eventing.
 *
 * TODO: I don't see a reason why this interface should support both
 * subscription *and* emitting, the latter seems like something private.
 */
export interface EditorEvents {
  subscribe<TDetail>(event: EventType<TDetail>, handler: EventHandler<TDetail>): VoidFunction;
  emit<TDetail>(event: EventType<TDetail>, detail?: TDetail): void;
}

/**
 * Creates a new type of event. Use the TDetail type parameter to indicate the
 * type of data, if any, that event handlers can expect.
 */
export function makeEventType<TDetail = void>(eventName: string) {
  return { eventName: `panmirror${eventName}` } as EventType<TDetail>;
}

/**
 * An implementation of EditorEvents, using the DOM event system.
 */
export class DOMEditorEvents implements EditorEvents {
  private readonly el: HTMLElement;

  constructor(el: HTMLElement) {
    this.el = el;
  }

  public emit<TDetail>(eventType: EventType<TDetail>, detail?: TDetail) {
    // Note: CustomEvent requires polyfill for IE, see
    // https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent/CustomEvent
    const event = new CustomEvent(eventType.eventName, { detail });
    return this.el.dispatchEvent(event);
  }

  public subscribe<TDetail>(eventType: EventType<TDetail>, handler: EventHandler<TDetail>) {
    const listener = function(this: any, evt: Event) {
      const detail: TDetail | undefined = (evt as CustomEvent).detail;
      handler.call(this, detail);
    };
    this.el.addEventListener(eventType.eventName, listener);
    return () => {
      this.el.removeEventListener(eventType.eventName, listener);
    };
  }
}
