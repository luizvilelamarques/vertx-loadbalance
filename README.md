"# vertx-loadbalance" 

praticando conceitos e fazendo app rodar com 5 containers, service discovery(consul) e loadbalance(fabio).

1- vertx-service discovery - consul com fabio LB </br> 
2- vertx config (git, file, httt)</br> 
3- vertx check health</br> 
4- docker</br> 



lancar consul</br> 
docker run -d --name=dev-consul -p 8500:8500 --net host consul

lancar fabio</br> 
docker run -d --name=fabio -p 9999:9999 -p 9998:9998 --net host fabiolb/fabio

lancar 5 containers de microserviços </br> 
  docker run -d --name=vertx-service0 -e porta=2220 -p 2220:2220 --net host luizvilelamarques/consul-vertx </br> 
  docker run -d --name=vertx-service1 -e porta=2221 -p 2221:2221 --net host luizvilelamarques/consul-vertx </br> 
  docker run -d --name=vertx-service2 -e porta=2222 -p 2222:2222 --net host luizvilelamarques/consul-vertx </br> 
  docker run -d --name=vertx-service3 -e porta=2223 -p 2223:2223 --net host luizvilelamarques/consul-vertx </br> 
  docker run -d --name=vertx-service4 -e porta=2224 -p 2224:2224 --net host luizvilelamarques/consul-vertx </br> 
  

buildar imagem: 'mvn clean install dockerfile:build'
