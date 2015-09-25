// Helper functions copied from closure base.js

var goog = {};
var jsinterop = {};

goog.global = this;
goog.implicitNamespaces_ = {};
goog.object = {};

goog.object.createSet = function() {
  var result = {};
  for (var i = 0; i < arguments.length; i++) {
    result[arguments[i]] = true;
  }
  return result;
};

goog.isProvided_ = function (name) {
  return !goog.implicitNamespaces_[name] && !!goog.getObjectByName(name);
};

goog.getObjectByName = function (name, opt_obj) {
  var parts = name.split('.');
  var cur = opt_obj || goog.global;
  for (var part; part = parts.shift();) {
    if (cur[part] != null) {
      cur = cur[part];
    } else {
      return null;
    }
  }
  return cur;
};

// no-op
goog.require = function () {
};

goog.abstractMethod = function() {
}

goog.provide = function (name) {
  // Ensure that the same namespace isn't provided twice. This is intended
  // to teach new developers that 'goog.provide' is effectively a variable
  // declaration. And when JSCompiler transforms goog.provide into a real
  // variable declaration, the compiled JS should work the same as the raw
  // JS--even when the raw JS uses goog.provide incorrectly.
  if (goog.isProvided_(name)) {
    throw Error('Namespace "' + name + '" already declared.');
  }
  delete goog.implicitNamespaces_[name];

  var namespace = name;
  while ((namespace = namespace.substring(0, namespace.lastIndexOf('.')))) {
    if (goog.getObjectByName(namespace)) {
      break;
    }
    goog.implicitNamespaces_[namespace] = true;
  }

  goog.exportPath_(name);
};

goog.exportPath_ = function (name, opt_object, opt_objectToExportTo) {
  var parts = name.split('.');
  var cur = opt_objectToExportTo || goog.global;

  // Internet Explorer exhibits strange behavior when throwing errors from
  // methods externed in this manner.  See the testExportSymbolExceptions in
  // base_test.html for an example.
  if (!(parts[0] in cur) && cur.execScript) {
    cur.execScript('var ' + parts[0]);
  }

  // Certain browsers cannot parse code in the form for((a in b); c;);
  // This pattern is produced by the JSCompiler when it collapses the
  // statement above into the conditional loop below. To prevent this from
  // happening, use a for-loop and reserve the init logic as below.

  // Parentheses added to eliminate strict JS warning in Firefox.
  for (var part; parts.length && (part = parts.shift());) {
    if (!parts.length && opt_object !== undefined) {
      // last part and we have an object; use it
      cur[part] = opt_object;
    } else if (cur[part]) {
      cur = cur[part];
    } else {
      cur = cur[part] = {};
    }
  }
};

/**
 * Inherit the prototype methods from one constructor into another.
 *
 * Usage:
 * <pre>
 * function ParentClass(a, b) { }
 * ParentClass.prototype.foo = function(a) { };
 *
 * function ChildClass(a, b, c) {
 *   ChildClass.base(this, 'constructor', a, b);
 * }
 * goog.inherits(ChildClass, ParentClass);
 *
 * var child = new ChildClass('a', 'b', 'see');
 * child.foo(); // This works.
 * </pre>
 *
 * @param {Function} childCtor Child class.
 * @param {Function} parentCtor Parent class.
 */
goog.inherits = function(childCtor, parentCtor) {
  // Workaround MyJsInterfaceWithPrototype test since the parentCtor doesn't exist
  // until after ScriptInjector, but this test back-patches the ctor
  if (!parentCtor) {
    return;
  }
  /** @constructor */
  function tempCtor() {};
  tempCtor.prototype = parentCtor.prototype;
  childCtor.superClass_ = parentCtor.prototype;
  childCtor.prototype = new tempCtor();
  /** @override */
  childCtor.prototype.constructor = childCtor;

  /**
   * Calls superclass constructor/method.
   *
   * This function is only available if you use goog.inherits to
   * express inheritance relationships between classes.
   *
   * NOTE: This is a replacement for goog.base and for superClass_
   * property defined in childCtor.
   *
   * @param {!Object} me Should always be "this".
   * @param {string} methodName The method name to call. Calling
   *     superclass constructor can be done with the special string
   *     'constructor'.
   * @param {...*} var_args The arguments to pass to superclass
   *     method/constructor.
   * @return {*} The return value of the superclass method/constructor.
   */
  childCtor.base = function(me, methodName, var_args) {
    // Copying using loop to avoid deop due to passing arguments object to
    // function. This is faster in many JS engines as of late 2014.
    var args = new Array(arguments.length - 2);
    for (var i = 2; i < arguments.length; i++) {
      args[i - 2] = arguments[i];
    }
    return parentCtor.prototype[methodName].apply(me, args);
  };
};

jsinterop.closure = {};
jsinterop.closure.uniqueIds_ = {};
jsinterop.closure.uniqueIdCounter_ = 0;

jsinterop.closure.getUniqueId = function (identifier) {
  if (!(identifier in jsinterop.closure.uniqueIds_)) {
    var newIdent = identifier + "_" + jsinterop.closure.uniqueIdCounter_++;
    jsinterop.closure.uniqueIds_[identifier] = newIdent;
  }
  return jsinterop.closure.uniqueIds_[identifier];
};

$wnd.MyJsInterface = function() {};
$wnd.MyJsInterface.staticX = 33;
$wnd.MyJsInterface.answerToLife = function() { return 42;};
$wnd.MyJsInterface.prototype.sum = function sum(bias) { return this.x + bias; };
