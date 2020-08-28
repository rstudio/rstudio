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
  };

  VirtualScroller.prototype = {
    setup: function(element, visuallyHiddenClass) {
      if (!element)
        return;

      //  *** _CONSTANTS ***
      this._DEBUG = false;
      this._SCROLL_DEBOUNCE_MS = 500;
      this._BUCKET_MAX_SIZE = 50;
      this._MAX_VISIBLE_BUCKETS = 10;

      // we use this style to keep the elements in the DOM for screen readers
      this._HIDDEN_STYLE = visuallyHiddenClass;

      //  *** global vars ***
      this.LAST_SCROLL_TIME = 0;
      this.INITIALIZED = false;

      //  *** instance vars ***
      this.scrollerEle = null;
      this.consoleEle = element;
      this.buckets = [];
      this.visibleBuckets = [];

      var self = this;

      self._createAndAddNewBucket = self._createAndAddNewBucket.bind(self);
      self._debug = self._debug.bind(self);
      self._hideBucket = self._hideBucket.bind(self);
      self._isAtBottomBucket = self._isAtBottomBucket.bind(self);
      self._isAtTopBucket = self._isAtTopBucket.bind(self);
      self._isBucketHidden = self._isBucketHidden.bind(self);
      self._jumpToBottom = self._jumpToBottom.bind(self);
      self._moveWindow = self._moveWindow.bind(self);
      self._onParentScroll = self._onParentScroll.bind(self);
      self._showBucket = self._showBucket.bind(self);
      self.append = self.append.bind(self);
      self.clear = self.clear.bind(self);
      self.getCurBucket = self.getCurBucket.bind(self);
      self.setScrollParent = self.setScrollParent.bind(self);

      // set up the initial bucket
      this.append(document.createElement("span"));

      // iterate through all historical elements and put them into buckets
      while(element.children.length > 0) {
        var child = element.children[0];
        element.removeChild(child);
        this.append(child);
      }

      // put the bucket into the console
      for (var i = 0; i < this.buckets.length; i++)
        this.consoleEle.appendChild(this.buckets[i]);

      // traverse up the parents until we find the ace_scroller and attach to it
      var ancestor = element.parentElement;
      while (!self.scrollerEle && !!ancestor) {
        if (ancestor.className.indexOf("ace_scroller") !== -1) {
          self.setScrollParent(ancestor);
          ancestor = null;
        } else {
          ancestor = ancestor.parentElement;
        }
      }

      // jump to latest button
      if (this.scrollerEle.parentElement.getElementsByClassName("jump-to-latest-console").length < 1) {
        this.jumpToLatestButton = document.createElement("div");
        this.jumpToLatestButton.classList.add("jump-to-latest-console");
        this.jumpToLatestButton.innerText = "Latest";
        this.jumpToLatestButton.style.display = "none";
        this.jumpToLatestButton.onclick = function () {
          self._jumpToBottom();
          self._setJumpToLatestVisible(false);
          self.consoleEle.focus();
        };
        this.scrollerEle.parentElement.append(this.jumpToLatestButton);
      }

      this.INITIALIZED = true;
    },

    _debug: function(msg) {
      if (this._DEBUG)
        console.log(msg);
    },

    _moveWindow: function(up) {
      if (up && this._isAtTopBucket())
        return;
      if (!up && this._isAtBottomBucket())
        return;

      var indexToShow;
      var indexToHide;
      var i;
      if (up) {
        indexToShow = this.visibleBuckets[0] - 1;
        indexToHide = this.visibleBuckets[this.visibleBuckets.length - 1];

        for (i = 0; i < this._MAX_VISIBLE_BUCKETS; i++) {
          this.visibleBuckets[i]--;
        }
      } else {
        indexToShow = this.visibleBuckets[this.visibleBuckets.length -1] + 1;
        indexToHide = this.visibleBuckets[0];

        for (i = 0; i < this._MAX_VISIBLE_BUCKETS; i++) {
          this.visibleBuckets[i]++;
        }
      }

      this._showBucket(indexToShow);
      this._hideBucket(indexToHide);

      // move scrollbar to keep the content in the same location
      if (this.scrollerEle.scrollTop === 0) {
        var offsetHeight = this.buckets[indexToShow].offsetHeight;

        if (!!offsetHeight) {
          // if we're scrolling down, scroll up instead of down
          if (!up)
            offsetHeight *= -1;

          this.scrollerEle.scrollTop += offsetHeight;
        }
      }
    },

    _hideBucket: function(index) {
      if (!!this.buckets[index] && !this._isBucketHidden(index))
        this.buckets[index].classList.add(this._HIDDEN_STYLE);
    },

    _showBucket: function(index) {
      if (!!this.buckets[index] && this._isBucketHidden(index))
        this.buckets[index].classList.remove(this._HIDDEN_STYLE);
    },

    _isBucketHidden: function(index) {
      return !!this.buckets[index] && this.buckets[index].classList.contains(this._HIDDEN_STYLE);
    },

    _isAtTopBucket: function() {
      return this.visibleBuckets[0] === 0;
    },

    _isAtBottomBucket: function() {
      return this.buckets.length - 1 === this.visibleBuckets[this.visibleBuckets.length - 1];
    },

    _scrolledToTop: function() {
      return this.scrollerEle.scrollTop < 1;
    },

    _scrolledToBottom: function() {
      return Math.abs(this.scrollerEle.scrollHeight - this.scrollerEle.offsetHeight - this.scrollerEle.scrollTop) < 1;
    },

    // this BOUND callback function
    _jumpToBottom: function() {
      var i;

      if (this.buckets.length <= this._MAX_VISIBLE_BUCKETS)
        return;

      // hide the current shown window
      for (i = 0; i < this.visibleBuckets.length; i++) {
        this._hideBucket(this.visibleBuckets[i]);
      }

      // show the bottom _MAX_VISIBLE_BUCKETS
      this.visibleBuckets = [];
      for (i = this.buckets.length - this._MAX_VISIBLE_BUCKETS; i < this.buckets.length; i++) {
        this.visibleBuckets.push(i);
        this._showBucket(i);
      }

      if (this.scrollerEle) {
        this.scrollerEle.scrollTop = this.scrollerEle.scrollHeight - this.scrollerEle.offsetHeight;
      }
    },

    _setJumpToLatestVisible: function(visible) {
      this.jumpToLatestButton.style.display = visible ? "block" : "none";
    },

    _onParentScroll: function(event) {
      if (this.buckets.length <= this._MAX_VISIBLE_BUCKETS)
        return;

      var now = new Date().getTime();

      this._setJumpToLatestVisible(!this._isAtBottomBucket());

      if (now - this.LAST_SCROLL_TIME > this._SCROLL_DEBOUNCE_MS) {
        // if we scrolled to the top, move the window up
        if (this._scrolledToTop() && !this._isAtTopBucket()) {
          this._moveWindow(true);
        }
        // if we scrolled to the bottom, move the window down
        else if (this._scrolledToBottom() && !this._isAtBottomBucket()) {
          this._moveWindow(false);
        }
        else {
          return;
        }

        this.LAST_SCROLL_TIME = now;

        // set a timeout to call this function again if the user is holding the scrollbar
        // at 0 so we know to keep showing more items after the debounce, or just
        // load in a bit more data so the user knows there's more content above
        var self = this;
        setTimeout(function () {
          if (self._scrolledToBottom() || self._scrolledToTop())
            self._onParentScroll();
        }, this._SCROLL_DEBOUNCE_MS + 10);
      }
    },

    setScrollParent: function(ele) {
      if (!ele)
        return;

      ele.addEventListener("scroll", this._onParentScroll);
      ele.onscroll = this._onParentScroll;
      this.scrollerEle = ele;
    },

    append: function (element) {
      if (this.getCurBucket().childElementCount >= this._BUCKET_MAX_SIZE) {
        this._createAndAddNewBucket();
      }
      this.getCurBucket().appendChild(element);
      this._jumpToBottom();
    },

    clear: function() {
      // remove all buckets from the DOM
      for (var i = 0; i < this.buckets.length; i++) {
        this.buckets[i].remove();
      }

      this._setJumpToLatestVisible(false);
      this.visibleBuckets = [];
      this.buckets = [];

      this._createAndAddNewBucket();
    },

    _createAndAddNewBucket: function() {
      var newBucket = document.createElement("span");

      // before we're initialized buckets, live in the ether
      if (this.INITIALIZED)
        this.consoleEle.appendChild(newBucket);

      this.buckets.push(newBucket);

      // buckets always start visible, add to visible list
      this.visibleBuckets.push(this.buckets.length - 1);

      return newBucket;
    },

    getCurBucket: function() {
      // there should always be a bucket
      if (this.buckets.length < 1)
        return this._createAndAddNewBucket();

      return this.buckets[this.buckets.length - 1];
    }
  }
})();

if (typeof module !== 'undefined') {
  module.exports = VirtualScroller;
}

