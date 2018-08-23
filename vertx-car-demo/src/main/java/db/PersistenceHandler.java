package db;

import java.util.Arrays;

import db.models.Car;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

public class PersistenceHandler extends AbstractVerticle {

	private static final Logger LOG = LoggerFactory.getLogger(PersistenceHandler.class);

	public static final String PERSISTENCE_SERVICE_ADDRESS_GET_ALL = "persistence-service-getall";
	public static final String PERSISTENCE_SERVICE_ADDRESS_ADD_ONE = "persistence-service-addone";
	public static final String PERSISTENCE_SERVICE_ADDRESS_DELETE_ONE = "persistence-service-deleteone";
	public static final String PERSISTENCE_SERVICE_ADDRESS_UPDATE_ONE = "persistence-service-updateone";

	private static final String[] predefinedCars = { "CA 7404 TC,Dima Atanasova", "CB 5715 KT,Ivan Ivanov",
			"CA 1884 AK,Mihail Uzunov", "BT 1914 KC,Luybomir Staykov" };

	private static final String CARS_COLLECTON = "cars";

	private MongoClient mongoClient;
	private JsonObject mongoConfig;

	public PersistenceHandler(String host, int port) {
		this.mongoConfig = new JsonObject(this.buildJsonConnectionString(host, port));
	}

	@Override
	public void start() throws Exception {
		this.mongoClient = MongoClient.createShared(vertx, this.mongoConfig);
		
		this.dropCollection(CARS_COLLECTON);
		this.createCollection(CARS_COLLECTON);
		
		this.generateInitalTestData();
	
		this.registerGetAllEventConsumer();	
		this.registerAddOneEventConsumer();
		this.registerDeleteOneEventConsumer();
		this.registerUpdateOneEventConsumer();
	}

	private void registerUpdateOneEventConsumer() {
		vertx.eventBus().consumer(PERSISTENCE_SERVICE_ADDRESS_UPDATE_ONE, event -> {
			JsonObject body = new JsonObject(event.body().toString());
			JsonObject query = new JsonObject().put("id", body.getInteger("id"));
			this.mongoClient.find(CARS_COLLECTON, query, res -> {
				if (res.succeeded()) {
					JsonObject record = res.result().get(0);
					String regNum = "registrationNumber";
					String ownerName = "ownerName";
					record.put("registrationNumber", body.getString(regNum)).put(ownerName, body.getString(ownerName));
					JsonObject queryUpdt = new JsonObject().put("_id", record.getString("_id"));
					JsonObject update = new JsonObject().put("$set",
							new JsonObject().put("registrationNumber", body.getString(regNum)).put(ownerName,
									body.getString(ownerName)));
					this.mongoClient.updateCollection(CARS_COLLECTON, queryUpdt, update, resultOfUpdate -> {
						if (resultOfUpdate.succeeded()) {
							this.mongoClient.find(CARS_COLLECTON, query,
									replyResult -> event.reply(replyResult.result().get(0)));
						} else {
							LOG.error("Failed to update document. Reason: " + res.cause().getMessage());
							res.cause().printStackTrace();
						}
					});
				} else {
					LOG.error("Failed to find document. Reason: " + res.cause().getMessage());
					res.cause().printStackTrace();
				}
			});
		});
	}

	private void registerDeleteOneEventConsumer() {
		vertx.eventBus().consumer(PERSISTENCE_SERVICE_ADDRESS_DELETE_ONE, event -> {
			this.mongoClient.removeDocument(CARS_COLLECTON, new JsonObject().put("id", event.body()), res -> {
				if (res.succeeded()) {
					event.reply("");
				} else {
					LOG.error("Failed to delete document. Reason: " + res.cause().getMessage());
					res.cause().printStackTrace();
				}
			});
		});
	}

	private void registerAddOneEventConsumer() {
		vertx.eventBus().consumer(PERSISTENCE_SERVICE_ADDRESS_ADD_ONE, event -> {
			this.mongoClient.save(CARS_COLLECTON, new JsonObject(event.body().toString()), res -> {
				if (res.succeeded()) {
					event.reply(event.body());
				} else {
					LOG.error("Failed to save document. Reason: " + res.cause().getMessage());
					res.cause().printStackTrace();
				}
			});
		});
	}

	private void registerGetAllEventConsumer() {
		vertx.eventBus().consumer(PERSISTENCE_SERVICE_ADDRESS_GET_ALL, event -> {
			this.mongoClient.find(CARS_COLLECTON, new JsonObject(), res -> {
				if (res.succeeded()) {
					event.reply(Json.encodePrettily(res.result()));
				} else {
					LOG.error("Failed to get all documents. Reason: " + res.cause().getMessage());
					res.cause().printStackTrace();
				}
			});
		});
	}

	private void generateInitalTestData() {
		LOG.info("Creating initial data.");
		Arrays.asList(predefinedCars).stream().forEach(predefCar -> {
			String[] splittedData = predefCar.split(",");
			Car car = new Car(splittedData[0], splittedData[1]);
			this.mongoClient.save(CARS_COLLECTON, JsonObject.mapFrom(car), res -> {
				if (!res.succeeded()) {
					LOG.error("Failed to save document. Reason: " + res.cause().getMessage());
					res.cause().printStackTrace();
				}
			});
		});
		LOG.info("Initial data created.");
	}

	private String buildJsonConnectionString(String host, int port) {

		return "{\"host\":\"" + host + "\", \"port\":" + port + "}";
	}
	
	private void dropCollection(String collection) {
		this.mongoClient.dropCollection(collection, res -> {
			if (res.succeeded()) {
				LOG.info("Collection " + collection + " dropped.");
			} else {
				LOG.error("Failed to drop collection: " + collection + ". Reason: " + res.cause().getMessage());
				res.cause().printStackTrace();
			}
		});
	}

	private void createCollection(String collection) {
		this.mongoClient.createCollection(collection, res -> {
			if (res.succeeded()) {
				LOG.info("Collection " + collection + " created successfuly.");
			} else {
				LOG.warn("Failed to create collection " + collection + ". Reason: " + res.cause().getMessage());
			}
		});
	}
}