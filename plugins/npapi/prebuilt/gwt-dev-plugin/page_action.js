function getParam(key) {
  var idx = window.location.search.indexOf(key + "=");
  var value = '';
  if (idx >= 0) {
    idx += key.length + 1;
    value = window.location.search.substring(idx).split('&')[0];
  }
  return value;
}

function init() {
  var permission = getParam('permission');
  var host = getParam('host');
  var code = getParam('codeserver');
  var message='';

  if (permission == 'include') {
    message = 'The web and code server (' + host + '/' + code + ') is allowed to use the plugin';
  } else if (permission == 'exclude') {
    message = 'The web and code server (' + host + '/' + code + ') has been been blacklisted.';
  } else if (permission == 'unknown') {
    message = 'The web and code server (' + host + '/' + code + ') is unknown to the plugin.';
  }

  document.getElementById('message').innerText = message;
  document.getElementById("updateConfiguration").onclick = updateConfiguration
}

function updateConfiguration() {
  var url = 'DevModeOptions.html?host=' + getParam('host') + '&codeserver=' + getParam('codeserver');
  url = chrome.extension.getURL(url);
  chrome.tabs.create({'url' : url});
}

window.onload = init;
