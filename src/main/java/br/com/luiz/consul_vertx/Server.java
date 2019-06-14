package br.com.luiz.consul_vertx;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

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
 * docker-compose up -d
 * docker run -d --name=fabio -p 9999:9999 -p 9998:9998 --net host fabiolb/fabio
 * 
 * lancar 5 containers de microserviços
 * docker-compose scale vertx=5
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
		Router router = Router.router(vertx);
		router.route("/tasks/*").handler(ResponseContentTypeHandler.create());
		router.route(HttpMethod.POST, "/account").handler(BodyHandler.create());
		
		router.get("/tasks").produces("application/json").handler(rc -> {
			LOGGER.info("Tasks atendido por: " + ipServer() , "");
			rc.response().end(Json.encodePrettily("Tasks atendido por: " + ipServer()));
		});
		router.get("/testetask").produces("application/json").handler(rc -> {
			LOGGER.info("testetask atendido por: " + ipServer() , "");
			rc.response().end(Json.encodePrettily("TesteTask atendido por: " + ipServer()));
		});
		lancaMS(router, 2222);
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
		String ipMS = ipServer();
		
		WebClient client = WebClient.create(vertx);
		JsonObject json = new JsonObject()
				.put("ID", ipMS + "-" + new Random().nextInt(1000) + "-" + porta)
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
	
	private String ipServer() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return "localhost";
	}
}
