var SERVER = 'ws://localhost:9000';
var URL = 'http://192.168.1.192';

chrome.browserAction.onClicked.addListener(function() {
  ws.send(JSON.stringify({type: 'write'}));
  chrome.browserAction.setIcon({path: "icon_green.png"});
  chrome.browserAction.setTitle({title: "Tap your card to save"});
});

var ws;

start();

function start() {
  ws = new WebSocket(SERVER);
  ws.onopen = function(){
    ws.send(JSON.stringify({type: 'announce', announce: 'tabs'}));
  };
  ws.onclose = function(){
    setTimeout(start, 5000);
  };
  ws.onmessage = function(e) {
    var data = JSON.parse(e.data);
    if(!data.write) {        
      if(data.data.type == 'tabs') {
        data.data.tabs.forEach(function(el){
          chrome.tabs.create({'url': el}, function(tab) {});
        });
      }
      reset();
    } else if (data.write) {
      var tabs = [];
      chrome.tabs.query({}, function callback(ts){
        for(var i =0, ii = ts.length; i < ii; i++){
          tabs.push(ts[i].url);
        }
        $.post(URL + '/store/'+data.id, {type: 'tabs', tabs: tabs});
        reset();
      });
    }
  }
}

function reset() {
  chrome.browserAction.setTitle({title: "Click and tap to save tabs"});
  chrome.browserAction.setIcon({path: "icon_blue.png"});
}
