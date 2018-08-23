package web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class WebService extends AbstractVerticle {

	private static final Logger LOG = LoggerFactory.getLogger(WebService.class);
	private static final String RESOURCE_PATH = "web-resources";
	private static final String BASE_ROUTE_PATH = "/";

	private int port;

	public WebService(int port) {
		this.port = port;
	}

	@Override
	public void start() throws Exception {
		LOG.info("Starting WebService.");
		Router router = Router.router(vertx);

		router.route(BASE_ROUTE_PATH).handler(StaticHandler.create(RESOURCE_PATH));

		vertx.createHttpServer().requestHandler(router::accept).listen(this.port, resultOfStart -> {
			if (resultOfStart.succeeded()) {
				LOG.info("WebServer started and listening on port " + this.port);
			} else {
				LOG.error("WebServer could NOT start on port: " + this.port + ". Reason: "
						+ resultOfStart.cause().getMessage());
			}
		});
	}
}
