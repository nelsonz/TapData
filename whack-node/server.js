var CLOUD = 'http://192.168.1.192';

var Serial = require('serialport');
var	WebSocketServer = require('ws').Server;
var Rest = require('restler');

var paused = false;
var write = false;
var timeout;
var sockets = {};

var Port = new Serial.SerialPort(process.argv[2], {
	parser: Serial.parsers.readline('\n')
});  

Port.on("data", function(data) {
  if(data !== paused) {
    var response = {write: write, id: data};
    if(!write) {
      Rest.get(CLOUD + '/store/'+data).on('complete', function(res){
		  
        if(sockets[res.type]) {
          response.data = res;
          sockets[res.type].send(JSON.stringify(response));
          console.log('Sending read to', res.type);
        }
      });
      paused = data;
    } else {
      sockets[write].send(JSON.stringify(response));
      write = false;
      paused = data;
    }
    timeout = setTimeout(function(){
      paused = false;
    }, 10000);
  }
});

var Server = new WebSocketServer({port: 9000});

Server.on('connection', function(socket) {
  var type;
  paused = false;
  clearTimeout(timeout);
	socket.on('message', function(message) {
    try {
    message = JSON.parse(message);
    if(message.type == 'announce'){
      type = message.announce;
      sockets[type] = socket;
      paused = false;
      clearTimeout(timeout);
      console.log('Started', type, 'socket');
    } else if (message.type == 'write'){
      paused = false;
      clearTimeout(timeout);
      write = type;
      console.log('Write requested by', type);
    }
    } catch (e) {console.log(e);}
  });
  socket.on('close', function(){
    delete sockets[type];
  });
});




