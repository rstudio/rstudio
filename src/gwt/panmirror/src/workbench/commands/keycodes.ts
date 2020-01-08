const kMac = typeof navigator !== 'undefined' ? /Mac/.test(navigator.platform) : false;

export interface KeyCode {
  meta: boolean;
  shift: boolean;
  ctrl: boolean;
  alt: boolean;
  key: string;
}

export function toBlueprintHotkeyCombo(key: string) {
  return key.toLowerCase().replace(/-/g, ' + ');
}

export function toKeyCode(key: string) {
  if (!kMac) {
    key = key.replace('Mod-', 'Ctrl-');
  }
  const keys = key.split('-');
  let keystr = keys[keys.length - 1];
  if (/^[\w]$/.test(keystr)) {
    keystr = keystr.toUpperCase();
  }
  return {
    meta: keys.indexOf('Mod') !== -1,
    shift: keys.indexOf('Shift') !== -1,
    ctrl: keys.indexOf('Ctrl') !== -1,
    alt: keys.indexOf('Alt') !== -1,
    key: keystr,
  };
}

export function keyCodeString(keyCode: string | KeyCode, pretty = true) {
  if (typeof keyCode === 'string') {
    keyCode = toKeyCode(keyCode);
  }
  if (kMac && pretty) {
    return (
      `${keyCode.ctrl ? '⌃' : ''}` +
      `${keyCode.alt ? '⌥' : ''}` +
      `${keyCode.shift ? '⇧' : ''}` +
      `${keyCode.meta ? '⌘' : ''}` +
      keyName(keyCode.key, pretty)
    );
  } else {
    return (
      `${keyCode.ctrl ? 'Ctrl+' : ''}` +
      `${keyCode.alt ? 'Alt+' : ''}` +
      `${keyCode.shift ? 'Shift+' : ''}` +
      `${keyCode.meta ? 'Cmd+' : ''}` +
      keyName(keyCode.key, pretty)
    );
  }
}

function keyName(key: string, pretty: boolean) {
  if (pretty) {
    switch (key) {
      case 'Enter':
        return '⌅';
      case 'Up':
        return '↑';
      case 'Down':
        return '↓';
      case 'Left':
        return '←';
      case 'Right':
        return '→';
      case 'Tab':
        return '⇥';
      case 'PageUp':
        return 'PgUp';
      case 'PageDown':
        return 'PgDn';
      case 'Backspace':
        return '⌫';
      default:
        return key;
    }
  }

  return key;
}
