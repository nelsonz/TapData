var SERVER = 'ws://localhost:9000';
var URL = 'http://192.168.1.192';

var ws = new WebSocket(SERVER);

ws.onmessage = function(e) {
  $.getJSON(URL + '/store/'+e.data, function(res){
    if(res.type == 'tabs') {
      res.tabs.forEach(function(el){
        chrome.tabs.create({'url': el}, function(tab) {});
      });
    }
  });
}