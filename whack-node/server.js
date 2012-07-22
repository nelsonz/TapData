

var WebSocketServer = require('ws').Server
var wss = new WebSocketServer({port: 9000});
wss.on('connection', function(ws) {
  ws.on('message', function(message) {
    
  });
});