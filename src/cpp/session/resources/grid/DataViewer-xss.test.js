/*
 * DataViewer-xss.test.js
 *
 * Focused regression coverage for viewer links built from nested data/list
 * cells. These links must not place user-controlled row names inside a
 * javascript: href; the row value should only flow through the click handler.
 */

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

class TestElement {
  constructor(tagName) {
    this.tagName = tagName.toUpperCase();
    this.children = [];
    this.attributes = new Map();
    this.listeners = new Map();
    this.style = {};
    this.parentNode = null;
    this._className = '';
    this._innerHTML = '';
    this._textContent = '';
    this.href = '';
    this.src = '';
    this.title = '';
    this.id = '';
    this.classList = {
      add: (...names) => {
        const set = new Set(this._className.split(/\s+/).filter(Boolean));
        names.forEach((name) => set.add(name));
        this._className = Array.from(set).join(' ');
      },
      remove: (...names) => {
        const remove = new Set(names);
        this._className = this._className
          .split(/\s+/)
          .filter((name) => name && !remove.has(name))
          .join(' ');
      },
      contains: (name) => this._className.split(/\s+/).includes(name),
    };
  }

  set className(value) {
    this._className = String(value);
  }

  get className() {
    return this._className;
  }

  set textContent(value) {
    this._textContent = String(value);
    this._innerHTML = escapeHtml(this._textContent);
    this.children = [];
  }

  get textContent() {
    if (this.children.length > 0) {
      return this.children.map((child) => child.textContent).join('');
    }
    return this._textContent || stripTags(this._innerHTML);
  }

  set innerHTML(value) {
    this._innerHTML = String(value);
    this.children = [];
  }

  get innerHTML() {
    return this._innerHTML + this.children.map((child) => child.outerHTML).join('');
  }

  get outerHTML() {
    const attrs = [];
    if (this.className) attrs.push(`class="${escapeHtml(this.className)}"`);
    if (this.href) attrs.push(`href="${escapeHtml(this.href)}"`);
    if (this.src) attrs.push(`src="${escapeHtml(this.src)}"`);
    for (const [name, value] of this.attributes) {
      if (name === 'class' || name === 'href' || name === 'src') continue;
      attrs.push(`${name}="${escapeHtml(value)}"`);
    }
    return `<${this.tagName.toLowerCase()}${attrs.length ? ' ' + attrs.join(' ') : ''}>${this.innerHTML}</${this.tagName.toLowerCase()}>`;
  }

  appendChild(child) {
    child.parentNode = this;
    this.children.push(child);
    return child;
  }

  setAttribute(name, value) {
    const stringValue = String(value);
    this.attributes.set(name, stringValue);
    if (name === 'class') this.className = stringValue;
    if (name === 'href') this.href = stringValue;
    if (name === 'src') this.src = stringValue;
  }

  getAttribute(name) {
    if (name === 'class') return this.className;
    if (name === 'href') return this.href;
    if (name === 'src') return this.src;
    return this.attributes.get(name) ?? null;
  }

  addEventListener(type, handler) {
    this.listeners.set(type, handler);
  }

  click() {
    const handler = this.listeners.get('click');
    assert.equal(typeof handler, 'function', 'expected click handler to be installed');
    let defaultPrevented = false;
    handler({ preventDefault: () => { defaultPrevented = true; } });
    return defaultPrevented;
  }

  querySelector(selector) {
    if (selector.startsWith('.')) {
      const className = selector.slice(1);
      return findElement(this, (el) => el.classList.contains(className));
    }
    return findElement(this, (el) => el.tagName.toLowerCase() === selector.toLowerCase());
  }
}

function findElement(root, predicate) {
  for (const child of root.children) {
    if (predicate(child)) return child;
    const nested = findElement(child, predicate);
    if (nested) return nested;
  }
  return null;
}

function stripTags(value) {
  return String(value).replace(/<[^>]*>/g, '');
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (ch) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }[ch]));
}

function loadDataViewer() {
  const dataViewerPath = path.join(__dirname, 'DataViewer.js');
  let source = fs.readFileSync(dataViewerPath, 'utf8');

  if (process.env.RSTUDIO_DATAVIEWER_TEST_MUTATE_VULNERABLE === '1') {
    source = source.replace(
      'linkEl.href = "#";',
      'linkEl.href = "javascript:" + cbName + "(" + rowData[0] + "," + cbCol + ")";',
    );
  }

  source = source.replace(
    '\n// ==========================================================================\n// Initialization\n// ==========================================================================\n',
    '\nwindow.__dataViewerXssTest = { createCell: createCell, escapeHtml: escapeHtml };\n\n// ==========================================================================\n// Initialization\n// ==========================================================================\n',
  );

  const document = {
    body: new TestElement('body'),
    createElement: (tagName) => new TestElement(tagName),
    getElementById: () => null,
    addEventListener: () => {},
  };
  const window = {
    location: { search: '' },
    addEventListener: () => {},
  };

  const context = vm.createContext({
    AbortController,
    Map,
    Math,
    Promise,
    Set,
    String,
    alert: () => {},
    console,
    decodeURIComponent,
    document,
    encodeURIComponent,
    fetch: () => Promise.reject(new Error('fetch not available in unit test')),
    localStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} },
    setTimeout,
    window,
  });

  vm.runInContext(source, context, { filename: dataViewerPath });
  return { document, window };
}

test('nested data-cell viewer links keep malicious row names out of javascript hrefs', () => {
  const { window } = loadDataViewer();
  const { createCell } = window.__dataViewerXssTest;
  const maliciousRowName = `evil');alert('xss');//<img src=x onerror=alert(1)>`;
  const rowData = [JSON.stringify(maliciousRowName), 'list(value = 1)'];
  const calls = [];

  window.dataViewerCallback = (row, col) => calls.push({ row, col });

  const cell = createCell('list(value = 1)', 1, rowData, 'dataCell');
  const link = cell.querySelector('.viewerLink');

  assert.ok(link, 'viewer link should be rendered for nested data cells');
  assert.equal(link.getAttribute('href'), '#');
  assert.doesNotMatch(cell.innerHTML, /javascript:/i);
  assert.doesNotMatch(cell.innerHTML, /alert\(/i);
  assert.doesNotMatch(cell.innerHTML, /onerror/i);
  assert.equal(link.click(), true, 'click handler should prevent default navigation');
  assert.deepEqual(calls, [{ row: maliciousRowName, col: 1 }]);
});
