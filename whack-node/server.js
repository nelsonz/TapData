var serial = require('serialport'),
	WebSocketServer = require('ws').Server;

var port = process.argv[2],
	prev = "";
	saving = false,
	ws;
	wss = new WebSocketServer({port: 9000});
	
wss.on('connection', function(socket) {
	ws = socket;
});

wss.on('message', function(message) {
	if(message=='save') {
		saving = true;
	}
});

serialPort = new serial.SerialPort(port, {
	parser: serial.parsers.readline('\n')
});

serialPort.on("data", function(data) {
	if(ws && (saving || prev != data)) {
		var response = {};
		if(saving) {
			response.write = true;
		} else {
			response.read = true;
		}
		response.id = data;
		ws.send(JSON.stringify(response));
		prev = data;
		saving = false;
	}
});
