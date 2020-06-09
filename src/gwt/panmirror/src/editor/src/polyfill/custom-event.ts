/**
 * https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent/CustomEvent
 *
 * From https://developer.mozilla.org/en-US/docs/MDN/About:
 * "Any copyright is dedicated to the Public Domain. http://creativecommons.org/publicdomain/zero/1.0/"
 */
export default function() {
  if (typeof window.CustomEvent === 'function') return false;

  function CustomEvent(event: any, params: any) {
    params = params || { bubbles: false, cancelable: false, detail: null };
    var evt = document.createEvent('CustomEvent');
    evt.initCustomEvent(event, params.bubbles, params.cancelable, params.detail);
    return evt;
  }

  window.CustomEvent = CustomEvent as any;
}
