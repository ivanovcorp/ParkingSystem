package rest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import db.PersistenceHandler;
import db.models.Car;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

public class RestAPI extends AbstractVerticle {

	private static final Logger LOG = LoggerFactory.getLogger(RestAPI.class);

	/* API paths */
	private static final String BASE = "/api/";
	private static final String CARS_PATH = BASE + "cars";
	private static final String CARS_AND_ID_PATH = CARS_PATH + "/:id";
	private static final String CARS_WILDCARD_PATH = CARS_PATH + "/*";

	private int port;

	public RestAPI(int port) {
		this.port = port;
	}

	@Override
	public void start() throws Exception {
		LOG.info("Starting REST Server.");
		Router router = Router.router(vertx);
		Set<HttpMethod> allowedMethods = new LinkedHashSet<>(
				Arrays.asList(HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS));
		router.route().handler(CorsHandler.create("*").allowedMethods(allowedMethods));

		router.get(CARS_PATH).handler(this::getAll);
		router.route(CARS_WILDCARD_PATH).handler(BodyHandler.create());
		router.post(CARS_PATH).handler(this::addOne);
		router.put(CARS_AND_ID_PATH).handler(this::updateOne);
		router.delete(CARS_AND_ID_PATH).handler(this::deleteOne);

		vertx.createHttpServer().requestHandler(router::accept).listen(9777, resultOfStart -> {
			if (resultOfStart.succeeded()) {
				LOG.info("REST Server started and listening on port " + this.port);
			} else {
				LOG.error("REST Server could NOT start on port: " + this.port + ". Reason: "
						+ resultOfStart.cause().getMessage());
			}
		});
	}

	private void updateOne(RoutingContext routingContext) {
		LOG.info("REST CALL -> updateOne()");
		final String id = routingContext.request().getParam("id");
		JsonObject json = routingContext.getBodyAsJson();
		if (id == null || json == null) {
			routingContext.response().setStatusCode(400).end();
		} else {
			final Integer idAsInteger = Integer.valueOf(id);
			json.put("id", idAsInteger);
			vertx.eventBus().send(PersistenceHandler.PERSISTENCE_SERVICE_ADDRESS_UPDATE_ONE, Json.encodePrettily(json),
					reply -> {
						if (reply.succeeded()) {
							LOG.info("Record with ID " + idAsInteger + " updated successfuly.");
							routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
									.end(reply.result().body().toString());
						} else {
							LOG.error("Failed to update record with ID: " + idAsInteger + ". Reason: "
									+ reply.cause().getMessage());
							reply.cause().printStackTrace();
						}
					});
		}
	}

	private void deleteOne(RoutingContext routingContext) {
		LOG.info("REST CALL -> deleteOne()");
		String id = routingContext.request().getParam("id");
		if (id == null) {
			routingContext.response().setStatusCode(400).end();
		} else {
			Integer idAsInteger = Integer.valueOf(id);
			vertx.eventBus().send(PersistenceHandler.PERSISTENCE_SERVICE_ADDRESS_DELETE_ONE, idAsInteger, res -> {
				if (res.succeeded()) {
					LOG.info("Record with ID " + idAsInteger + " deleted successfully.");
					routingContext.response().setStatusCode(204).end();
				} else {
					LOG.info("Failed to delete recorct with ID " + idAsInteger + ". Reason: "
							+ res.cause().getMessage());
					res.cause().printStackTrace();
					routingContext.response().setStatusCode(400).end();
				}
			});
		}
	}

	private void getAll(RoutingContext routingContext) {
		LOG.info("REST CALL -> getAll()");
		vertx.eventBus().send(PersistenceHandler.PERSISTENCE_SERVICE_ADDRESS_GET_ALL, "", reply -> {
			routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
					.end(reply.result().body().toString());
		});
	}

	private void addOne(RoutingContext routingContext) {
		LOG.info("REST CALL -> addOne()");
		JsonObject jsonPostObject = new JsonObject(routingContext.getBodyAsString());
		final Car car = new Car(jsonPostObject.getString("registrationNumber"), jsonPostObject.getString("ownerName"));
		vertx.eventBus().send(PersistenceHandler.PERSISTENCE_SERVICE_ADDRESS_ADD_ONE, Json.encodePrettily(car),
				reply -> {
					if (reply.succeeded()) {
						LOG.info("Record with ID " + car.getId() + " added successfuly.");
						routingContext.response().setStatusCode(201)
								.putHeader("content-type", "application/json; charset=utf-8")
								.end(reply.result().body().toString());
					} else {
						LOG.info("Failed to add Record with ID " + car.getId() + ". Reason: "
								+ reply.cause().getMessage());
						reply.cause().printStackTrace();
					}
				});
	}
}
