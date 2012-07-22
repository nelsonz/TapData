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

wss.on('save', function(socket) {
	saving = true;
});

serialPort = new serial.SerialPort(port, {
	parser: serial.parsers.readline('\n')
});

serialPort.on("data", function(data) {
	if(ws && (saving || prev != data)) {
		ws.send(data);
		prev = data;
		saving = false;
	}
});
