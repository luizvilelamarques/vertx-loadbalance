"# USO" 


git clone https://github.com/luizvilelamarques/vertx-loadbalance.git

cd consul-fabio-example

docker-compose up -d

docker-compose scale vertx=5

docker-compose down


=======
"# vertx-loadbalance" 

praticando conceitos e fazendo app rodar com 5 containers, service discovery(consul) e loadbalance(fabio).

1- vertx-service discovery - consul com fabio LB </br> 
2- vertx config (git, file, httt)</br> 
3- vertx check health</br> 
4- docker</br> 


  

buildar imagem: 'mvn clean install dockerfile:build'
