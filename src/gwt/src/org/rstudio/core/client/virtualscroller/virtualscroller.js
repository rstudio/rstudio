/*
 * virtualscroller.js
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
var VirtualScroller;

(function () {
  "use strict";
  VirtualScroller = function () {

    // _CONSTANTS
    this._DEBUG = true;
    this._SCROLL_DEBOUNCE_MS = 9000;
    this._BUCKET_MAX_SIZE = 100;
    this._MAX_VISIBLE_BUCKETS = 5;

    // "global" vars
    this.LAST_SCROLL_TIME = 0;

    // "local" vars
    this.scrollerEle = null;
    this.consoleEle = null;
    this.buckets = [];

    var self = this;

    VirtualScroller.prototype = {
      _debug: function(msg) {
        if (self._DEBUG)
          console.log(msg);
      },

      _onParentScroll: function(event) {

        var now = new Date().getTime();

        if (now - self.LAST_SCROLL_TIME < self._SCROLL_DEBOUNCE_MS) {
          console.log("setting lock scroll: " + self.LOCKED_SCROLL);
          self.LOCKED_SCROLL = self.scrollerEle.scrollTop;
          return;
        }

        // show the previous bucket
        if (self.scrollerEle.scrollTop === 0) {

          var outOfRangeIndex = self.buckets.length - 1 - self._MAX_VISIBLE_BUCKETS;
          while (outOfRangeIndex >= 0 && self.scrollerEle.scrollTop === 0) {
            if (self.buckets[outOfRangeIndex].style.display !== "inline") {
              self.buckets[outOfRangeIndex].style.display = "inline";

              var offsetHeight = self.buckets[outOfRangeIndex].offsetHeight;
              if (!!offsetHeight && offsetHeight > 0) {
                self.scrollerEle.scrollTop += offsetHeight;
                self.LOCKED_SCROLL = self.scrollerEle.scrollTop;
              }

              self.LAST_SCROLL_TIME = now;


              break;
            } else {
              outOfRangeIndex--;
            }
          }
        }
      },

      setScrollParent: function(ele) {
        if (!ele)
          return;

        console.log("setting scroll parent");
        //ele.addEventListener("scroll", this._onParentScroll);
        ele.onscroll = this._onParentScroll;
        self.scrollerEle = ele;
      },

      setup: function(element) {
        self.consoleEle = element;

        // set up the initial bucket
        if (!!element) {
          self.parentElement = element;

          var initialBucket = document.createElement("span");
          initialBucket.appendChild(document.createTextNode("Bucket #" + self.buckets.length));
          self.consoleEle.appendChild(initialBucket);
          self.buckets.push(initialBucket);

          // traverse up the parents until we find the ace_scroller
          var ancestor = element.parentElement;
          while (!self.scrollerEle && !!ancestor) {
            if (ancestor.className.indexOf("ace_scroller") !== -1) {
              this.setScrollParent(ancestor);
              ancestor = null;
            } else {
              ancestor = ancestor.parentElement;
            }
          }
        }
      },

      append: function (element) {
        //this._debug("append");
        if (this._getCurBucket().childElementCount >= self._BUCKET_MAX_SIZE) {
          this._createAndAddNewBucket();
        }
        this._addElementToCurrentBucket(element);
      },

      _addElementToCurrentBucket: function(element) {
        //this._debug("add element to current bucket");
        var curBucket = this._getCurBucket();
        curBucket.appendChild(element);
      },

      _createAndAddNewBucket: function() {
        //this._debug("createAndAddNewBucket");
        var newBucket = document.createElement("span");
        newBucket.appendChild(document.createTextNode("Bucket #" + self.buckets.length));
        self.consoleEle.appendChild(newBucket);
        self.buckets.push(newBucket);

        // hide until only the _MAX_VISIBLE_BUCKETS are showing
        var outOfRangeIndex = self.buckets.length - 1 - self._MAX_VISIBLE_BUCKETS;
        while (outOfRangeIndex >= 0 && self.buckets[outOfRangeIndex].style.display !== "none") {
          this._debug("hiding index: " + outOfRangeIndex);
          self.buckets[outOfRangeIndex].style.display = "none";
          outOfRangeIndex--;
        }

        return newBucket;
      },

      _getCurBucket: function() {
        // there should always be a bucket
        if (self.buckets.length < 1)
          return this._createAndAddNewBucket();

        return self.buckets[self.buckets.length - 1];
      }
    }
  };
})();


// Support for use as a node.js module.
if (typeof module !== 'undefined') {
  module.exports = VirtualScroller;
}

