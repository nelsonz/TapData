Getting data
============

```
GET http://192.168.1.192/store/(id)

=> JSON of whatever was stored
```

Storing data
============

```
POST http://192.168.1.192/store/(id)

Content-type = application/json

Body of request is JSON and stored directly into Mongo

```