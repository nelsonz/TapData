var mongo = require('mongoskin');
var ObjectID = require('mongoskin').ObjectID;
var db = mongo.db('localhost:27017/nfc?auto_reconnect');
var Data = db.collection('data');

var fs = require('fs');

var BinaryServer = require('binaryjs').BinaryServer;

var express = require('express');
var app = express.createServer();

app.use(express.static(__dirname + '/public'));
app.use(express.bodyParser());
app.use(express.cookieParser());

app.use(function(req, res, next) {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE');
  res.header('Access-Control-Allow-Headers', 'Content-Type');

  next();
});

app.get('/store/:id', function(req, res){
  Data.findOne({id: req.params.id}, function(err, doc){
    if(err || !doc) {
      res.send({error: 'nodata'});
      return;
    }
    res.send(doc);
  });
});

app.post('/store/:id', function(req, res){
  req.body.id = req.params.id;
  Data.update({id: req.params.id}, req.body, {upsert: true}, function(err){
    res.send('success');
  });
});
app.post('/mirror/:id', function(req, res){
  req.body.id = req.params.id;
  res.send(req.body);
});

app.listen(80);


var b = new BinaryServer({port: 9005});
b.on('connection', function(client){
  client.on('stream', function(stream, meta){
    var file = fs.createWriteStream(__dirname + '/public/'+meta.name);
    stream.pipe(file);
  });
});
