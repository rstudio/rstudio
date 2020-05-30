/*jshint browser:true, strict:false, curly:false, indent:3*/

/*
 * hist.js
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

var hist = function (ele, breaks, counts, start, end, update) {

   // find the largest count for normalization
   var max = counts[0];
   for(var i = 1; i < counts.length; i++)
   {
      if (counts[i] > max)
         max = counts[i];
   }

   // arrays of histogram bars and background regions
   var bars = [];
   var backs = [];

   // create elements for each bin
   var n = counts.length;
   for(var j = 0; j < n; j++)
   {
      // the histogram bar
      var bar = document.createElement("div");
      bar.className = "histBar";
      var style = bar.style;
      var height = (counts[j] / max) * 100;
      style.height = height + "%";
      style.width = "100%";
      style.position = "absolute";
      style.left = "0";
      style.top = (100 - height) + "%";

      // the background of the histogram bar
      var back = document.createElement("div");
      back.className = "histBack";
      back.style.height = "100%";
      back.style.position = "absolute";
      back.style.width = (100 / n) + "%";
      back.style.left = ((j / n) * 100) + "%";
      back.setAttribute("data-bin", j);
      back.appendChild(bar);
      backs.push(back);

      ele.appendChild(back);
   }

   // update selection based on start/end points
   var updateSelection = function(fireEvent) {
      // apply the correct class to each node based on whether it is contained within the selection
      for (var i = 0; i < backs.length; i++) {
         backs[i].className = (i >= start && i <= end) ?
            "histBack selected" : "histBack";
      }
      // if requested, let the caller know that the selection has changed
      if (fireEvent) {
         update(breaks[start], breaks[end + 1]);
      }
   };

   // given an element, find the bin to which it corresponds, or null if the the bin could not be
   // determined
   var findBin = function(ele) {
      var bin = ele.getAttribute("data-bin");
      if (bin === null) {
         bin = ele.parentElement.getAttribute("data-bin");
      }
      if (bin === null) {
         return null;
      }
      return parseInt(bin);
   };

   // given a mouse event, update the selected bins
   var brush = function(evt) {
      // determine whether a bin was brushed
      var bin = findBin(evt.target);
      if (bin === null) {
         return;
      }

      // no work to do if we're already at an endpoint
      if (bin === end || bin === start) {
         return;
      }

      // move end marker to brushed bin
      var last = end;
      end = bin;

      // if we went backwards, swap start and end so that they're still in order
      if (end < start) {
         start = end;
         end = last;
      }

      // redraw and fire update
      updateSelection(true);
   };

   // perform initial redraw, but don't fire an update event
   updateSelection(false);

   // set up event listeners
   ele.addEventListener("mousedown", function(evt) {
      var bin = findBin(evt.target);
      if (bin === null) {
         return;
      }
      start = bin;
      end = bin;
      updateSelection(true);
      ele.addEventListener("mousemove", brush);
   });

   ele.addEventListener("mouseup", function(evt) {
      ele.removeEventListener("mousemove", brush);
   });

   ele.addEventListener("mouseout", function(evt) {
      if (evt.target === ele)
         ele.removeEventListener("mousemove", brush);
   });
};
