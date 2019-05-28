package br.com.luiz.consul_vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;


/**
 * docker run -d --name=dev-consul -p 8500:8500 --net host consul
 * docker run -d --name=fabio -p 9999:9999 -p 9998:9998 --net host fabiolb/fabio
 * 
 * lancar 5 containers de microserviços
 * docker run -d --name=vertx-service0 -e porta=2220 -p 2220:2220 --net host luizvilelamarques/consul-vertx
 * docker run -d --name=vertx-service1 -e porta=2221 -p 2221:2221 --net host luizvilelamarques/consul-vertx
 * docker run -d --name=vertx-service2 -e porta=2222 -p 2222:2222 --net host luizvilelamarques/consul-vertx
 * docker run -d --name=vertx-service3 -e porta=2223 -p 2223:2223 --net host luizvilelamarques/consul-vertx
 * docker run -d --name=vertx-service4 -e porta=2224 -p 2224:2224 --net host luizvilelamarques/consul-vertx
 * 
 * 
 * ui consul: http://192.168.99.100:8500
 * ui fabio:  http://192.168.99.100:9998 (chamadas ao load balance fabio: http://192.168.99.100:9999/)
 * 
 * @author Luiz
 *
 */
public class Server extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
	
	public static void main(String[] args) throws Exception {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new Server());
	}
	
	@Override
	public void start() throws Exception {
		  
		String porta = System.getenv("porta");
		LOGGER.info("porta"+ System.getenv("porta"));
		
		Router router = Router.router(vertx);
		router.route("/tasks/*").handler(ResponseContentTypeHandler.create());
		router.route(HttpMethod.POST, "/account").handler(BodyHandler.create());
		
		router.get("/tasks").produces("application/json").handler(rc -> {
			LOGGER.info("acionou get tasks", "");
			rc.response().end(Json.encodePrettily("OK get tasks acionado"));
		});
		router.get("/testetask").produces("application/json").handler(rc -> {
			LOGGER.info("acionou get testetask", "");
			rc.response().end(Json.encodePrettily("OK get acionado"));
		});
		router.post("/tasks").produces("application/json").handler(rc -> {
			LOGGER.info("acionou post {}", "");
			rc.response().end(Json.encodePrettily("OK post acionado"));
		});
		router.delete("/tasks/:id").handler(rc -> {
			LOGGER.info("acionou delete {}", "");
			rc.response().end(Json.encodePrettily("OK delete acionado"));
		});
		
		//sample(router, Integer.valueOf(porta));
		lancaMS(router, Integer.valueOf(porta));
	}
	
	private void lancaMS(Router router, Integer porta) {
		//recupera configuracao do git
		ConfigRetriever retriever = obterConfiguracaoFile();
		
		retriever.getConfig(conf -> {
			JsonObject discoveryConfig = conf.result().getJsonObject("discovery");
			//health check
			HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
			router.get("/health").handler(healthCheckHandler);
			 
			//lanca aplicacao
			vertx.createHttpServer().requestHandler(router::accept).listen(porta);
			//registra ms no service discovery
			registrarServiceDiscovery(conf.result().getString("service-name"), porta,
					discoveryConfig.getString("host"), discoveryConfig.getInteger("port"));
		});
	}
	
	private void registrarServiceDiscovery(String serviceName, Integer porta, String hostServiceDiscovery, Integer portaServiceDiscovery ) {
		//ip do microserviço
		String ipMS       = "localhost";
		
		WebClient client = WebClient.create(vertx);
		JsonObject json = new JsonObject()
				.put("ID", serviceName + porta)
				.put("Name", serviceName)
				.put("Address", ipMS)
				.put("Port", porta)
				.put("Tags", new JsonArray().add("urlprefix-/tasks").add("urlprefix-/testetask"))
				.put("Check", new JsonObject().put("HTTP", "http://" + ipMS + ":" + porta + "/health").put("Interval", "10s"));
		
		client.put(portaServiceDiscovery, hostServiceDiscovery, "/v1/agent/service/register")
		.sendJsonObject(json, res -> {
			LOGGER.info("Consul registration status: {}", res.result().statusCode());
		});
	}
	
	private ConfigRetriever obterConfiguracaoGit() {
		JsonObject jsonObject = new JsonObject()
		        .put("url", "https://github.com/luizvilelamarques/vertx-loadbalance.git")
		        .put("path", "local")
		        .put("user", "luizvilelamarques")
		        .put("password", "xxxx")
		        .put("filesets",
		                new JsonArray().add(new JsonObject().put("pattern", "*.json")));
		ConfigStoreOptions git = new ConfigStoreOptions()
		        .setType("git")
		        .setConfig(jsonObject);
		ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(git));
		return retriever;
	}
	
	private ConfigRetriever obterConfiguracaoConsulChaveValor(String chave, String hostConsul, Integer portaConsul) {
		ConfigStoreOptions httpStore = new ConfigStoreOptions()
			      .setType("http")
			      .setConfig(new JsonObject()
			        .put("host", hostConsul).put("port", portaConsul).put("path", "v1/kv/" + chave));
		//base 64 resultado.
		ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(httpStore));
		return retriever;
	}
	
	private ConfigRetriever obterConfiguracaoFile() {
		JsonObject jsonObject = new JsonObject().put("path", "application.json");
		ConfigStoreOptions file = new ConfigStoreOptions()
				.setType("file")
				.setConfig(jsonObject);
		ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(file));
		return retriever;
	}
}
