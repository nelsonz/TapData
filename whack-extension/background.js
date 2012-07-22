var SERVER = 'ws://localhost:9000';
var URL = 'http://192.168.1.192';

chrome.browserAction.onClicked.addListener(function() {
  ws.send('save');
  chrome.browserAction.setBadgetText({text: "Waiting for NFC"});
  chrome.browserAction.setBadgeBackgroundColor({color: '#FF0000'});
});

var ws = new WebSocket(SERVER);

ws.onmessage = function(e) {
  var data = e.data;
  if(data.read) {
    $.getJSON(URL + '/store/'+data.id, function(res){
      if(res.type == 'tabs') {
        res.tabs.forEach(function(el){
          chrome.tabs.create({'url': el}, function(tab) {});
        });
      }
    });
  } else if (data.write) {
    var tabs = [];
    $.post(URL + '/store/'+data.id, {type: 'tabs', tabs: tabs});
    reset();
  }
}

function reset() {
  chrome.browserAction.setBadgetText({text: "Click to save tabs"});
  chrome.browserAction.setBadgeBackgroundColor({color: [0,0,0,0]});
}

reset();