boilerpipe-server
=================

## Server
Simple server/bash script for parsing article contents from a web page using the excellent boilerpipe project

```
./compile_server.sh
./run_server.sh
Server is listening on port 6666
```

```
# in other window
curl -d "http://mashable.com/2012/10/23/lifeswap/" "localhost:6666"
```


## Bash script
```
./compile.sh
```

```
curl "http://mobil.di.se/c.jsp;jsessionid=9A9574DB7A0D394DB97CB9247E5EDC7D.sonny4?cid=25400741&articleId=296509" | ./run.sh
```
