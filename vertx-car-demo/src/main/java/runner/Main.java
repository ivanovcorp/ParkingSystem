package runner;

import db.PersistenceHandler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rest.RestAPI;
import web.WebService;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	private static final int PORT_WEB_SERVER = 9213;
	private static final int PORT_REST_SERVER = 9777;

	private static final String MONGO_HOST = "127.0.0.1";
	private static final int MONGO_PORT = 27017;

	public static void main(String[] args) {
		LOG.info("Starting Cars application.");

		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new WebService(PORT_WEB_SERVER), res -> {
			if (res.succeeded()) {
				
			}
		});
		vertx.deployVerticle(new RestAPI(PORT_REST_SERVER), res -> {
			
		});
		vertx.deployVerticle(new PersistenceHandler(MONGO_HOST, MONGO_PORT), res -> {
			
		});
	}
}
