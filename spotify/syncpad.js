var LOCAL = 'ws://localhost:9000',
	CLOUD = 'http://192.168.1.192';
	
var sp = getSpotifyApi(1),
	player = sp.require('sp://import/scripts/api/models').player,
	exports.init = init;
	
function init() {
	var ws = new WebSocket(LOCAL);
	
	ws.onopen = function(){
		ws.send(JSON.stringify({type: 'announce', announce: 'song'}));
	};
	
	ws.onclose = function(){
		setTimeout(init, 5000);
	};

	ws.onmessage = function(msg) {
		var data = JSON.parse(msg.data);
		if (!data.write) {
			player.play("spotify:track:" + data.track);
			player.position = data.position;
		}
		if (data.write) {
			$.post(CLOUD + '/store/' + data.id, {type: 'song', track: player.track.uri, position: player.position});
		}
	}
	
	$("#write").on('click', function(e) {
		ws.send(JSON.stringify({type: 'write'}));
	});
}


