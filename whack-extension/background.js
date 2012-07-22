var SERVER = 'ws://localhost:9000';


var ws = new WebSocket(SERVER);

ws.onmessage = function(e) {
  $.getJSON('/store/'+e.data, function(res){
    console.log(res);
  });
}