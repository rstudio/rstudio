/*
 * virtualscroller.js
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
      this._BUCKET_MAX_HEIGHT = 50;
      this._MAX_VISIBLE_BUCKETS = 10;
      this._MAX_NEWLINES = 1000;

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
      self._isBucketFull = self._isBucketFull.bind(self);
      self._moveWindow = self._moveWindow.bind(self);
      self._onParentScroll = self._onParentScroll.bind(self);
      self._showBucket = self._showBucket.bind(self);
      self.append = self.append.bind(self);
      self.clear = self.clear.bind(self);
      self.scrollToBottom = self.scrollToBottom.bind(self);
      self.scrolledToBottom = self.scrolledToBottom.bind(self);
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

      // validate that we've successfully found ace_scroller
      if (self.scrollerEle == null) {
        throw "internal error: virtual scroller could not find ace_scroller element";
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

    _isBucketFull: function(bucket) {
      var height = 0;
      var contents = bucket.innerText;

      // add height for each new line
      height += contents.split(/\n/).length - 1;

      return height >= this._BUCKET_MAX_HEIGHT;
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
      return this.scrollerEle.scrollTop < 4;
    },

    scrolledToBottom: function() {
      return !!this.scrollerEle &&
          Math.abs(this.scrollerEle.scrollHeight - this.scrollerEle.offsetHeight - this.scrollerEle.scrollTop) < 50;
    },

    scrollToBottom: function() {
      if (!!this.scrollerEle)  {
        this.scrollerEle.scrollTop = this.scrollerEle.scrollHeight;
      }
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
        else if (this.scrolledToBottom() && !this._isAtBottomBucket()) {
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
          if (self.scrolledToBottom() || self._scrolledToTop())
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
      if (element === null)
        return;

      var keepScrolled = !!this.scrollerEle && this.scrolledToBottom();

      if (this._isBucketFull(this.getCurBucket())) {
        this._createAndAddNewBucket();
      }
      this.prune(element);
      this.getCurBucket().appendChild(element);
      this._jumpToBottom();
      if (keepScrolled && !!this.scrollerEle)  {
        this.scrollerEle.scrollTop = this.scrollerEle.scrollHeight;
      }
    },

    clear: function() {
      var i = 0;

      // remove all buckets from the DOM
      for (i = 0; i < this.buckets.length; i++) {
        this.buckets[i].remove();
      }

      // remove possible vestigial contents of the parent element that
      // may have snuck in before the VirtualScroller was initialized
      var eleChildren = this.consoleEle.children;
      while (this.consoleEle.children.length > 0) {
          this.consoleEle.removeChild(this.consoleEle.children[0]);
      }

      this._setJumpToLatestVisible(false);
      this.visibleBuckets = [];
      this.buckets = [];

      this._createAndAddNewBucket();
    },

    // element line number is hard capped at 1000 on the server, the VirtualConsole range
    // overwriting gets partially obviated by the virtualscroller so ensure that this
    // limit is also respected here
    prune: function(element) {
      var text = element.innerText;
      var newlineMatch = text.match(/\n/g);
      if (newlineMatch === null) return;

      // because IE11 doesn't support String.matchAll there isn't a much better option than this
      var newlinesToPrune = newlineMatch.length - this._MAX_NEWLINES
      var indexToSlice = 0;

      while (newlinesToPrune > 0) {
        indexToSlice = text.indexOf("\n", indexToSlice + 1);
        newlinesToPrune -= 1;
      }

      if (indexToSlice > 0) {
        element.innerText = "<console output truncated>" +
          element.innerText.substring(indexToSlice);
      }
    },

    ensureStartingOnNewLine: function() {
      if (this.getCurBucket().children < 1)
        return;

      // get the last element from the last bucket
      var lastText = this.getCurBucket().lastElementChild.innerHTML;
      if (!lastText.endsWith("\n"))
        this.getCurBucket().lastElementChild.innerHTML = lastText + "\n";
    },

    _createAndAddNewBucket: function() {
      var newBucket = document.createElement("span");

      // before we're initialized, buckets live in the ether
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

