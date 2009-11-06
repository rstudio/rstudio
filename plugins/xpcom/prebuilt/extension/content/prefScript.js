var GwtDeveloperPlugin = {

// Add a new entry when the Add Entry button is clicked.
addEntry: function() {
  var prefs = this.getAccessList();
  var hostname = document.getElementById("hostname").value;
  if (!hostname || hostname.length == 0) {
    alert("No host name provided");
    return;
  }
  if (hostname.indexOf(",") >=0 || hostname.indexOf("!") >= 0) {
    alert("Host name must not contain ',' or '!'");
    return;
  }
  var exclude = document.getElementById("exclude");
  var incText;
  var prefix = "";
  if (exclude.selected) {
    incText = "Exclude";
    prefix = "!";
  } else {
    incText = "Include";
  }
  var listboxEntry = this.makeLBE(incText, hostname);
  var prefsEntry = prefix + hostname;
  var listbox = document.getElementById("accessListListbox");
  listbox.appendChild(listboxEntry);
  prefs.push(prefsEntry.toString());
  this.saveAccessList(prefs);
},

// Remove the selected entry when the Remove Entry button is clicked.
removeEntry: function() {
  var listbox = document.getElementById("accessListListbox");
  var idx = listbox.selectedIndex;
  if (idx >= 0) {
    listbox.removeItemAt(idx);
    var prefs = this.getAccessList();
    prefs.splice(idx, 1);
    this.saveAccessList(prefs);
  }
},

// Populate the listbox when the dialog window is loaded
onload: function() {
  var listbox = document.getElementById("accessListListbox");
  var prefs = this.getAccessList();
  for (var i = 0 ; i < prefs.length; ++i) {
    var pref = prefs[i];
    var hostname = pref;
    var incexc = "Include";
    if (pref.length > 0 && pref.charAt(0) == "!") {
      hostname = hostname.substr(1);
      incexc = "Exclude";
    }
    var listboxEntry = this.makeLBE(incexc, hostname);
    listbox.appendChild(listboxEntry);
  }
},

// Internal - create a entry for the list box
makeLBE: function(inc, hostname) {
  var listboxEntry = document.createElement("listitem");
  var lbeInc = document.createElement("listcell");
  lbeInc.setAttribute("label", inc);
  listboxEntry.appendChild(lbeInc);
  var lbeHost = document.createElement("listcell");
  lbeHost.setAttribute("label", hostname);
  listboxEntry.appendChild(lbeHost);
  return listboxEntry;
},

// Internal - load the access list from the gwt-dev-plugin.accessList
// preference
getAccessList: function() {
  var prefServ = Components.classes["@mozilla.org/preferences-service;1"]
                  .getService(Components.interfaces.nsIPrefService);
  var prefs = prefServ.getBranch("gwt-dev-plugin.");
  var pref = prefs.getCharPref("accessList");
  if (!pref) {
    return [];
  }
  return pref.split(",");
},

// Internal - save the access list to the gwt-dev-plugin.accessList
// preference
saveAccessList: function(list) {
  var prefServ = Components.classes["@mozilla.org/preferences-service;1"]
                  .getService(Components.interfaces.nsIPrefService);
  var prefs = prefServ.getBranch("gwt-dev-plugin.");
  prefs.setCharPref("accessList", list.join(","));
  prefServ.savePrefFile(null);
}

};
