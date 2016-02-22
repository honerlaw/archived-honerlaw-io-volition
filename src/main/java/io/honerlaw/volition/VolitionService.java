package io.honerlaw.volition;

import org.mindrot.jbcrypt.BCrypt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.MVELTemplateEngine;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class VolitionService extends AbstractVerticle {
	
	private final MVELTemplateEngine templateEngine = MVELTemplateEngine.create().setMaxCacheSize(0);
	
	@Override
	public void start() {
		
		System.setProperty("vertx.disableFileCaching", "true");
		
		JDBCClient client = JDBCClient.createShared(getVertx(), new JsonObject()
				.put("url", "")
				.put("user", "")
				.put("password", ""));
		
		Router router = Router.router(getVertx());
		
		// create the session / cookie handler
		router.route().handler(CookieHandler.create());
		router.route().handler(SessionHandler.create(LocalSessionStore.create(getVertx())));
		
		// create the body handler
		router.route().handler(BodyHandler.create());
	
		// login / register page OR main account home page
		router.get("/").handler(ctx -> render(ctx, "index"));
		
		// handle a login request
		router.post("/login").handler(ctx -> {
			String email = ctx.request().getFormAttribute("email").trim();
			String password = ctx.request().getFormAttribute("password");
			client.getConnection(res -> {
				if(res.succeeded()) {
					SQLConnection con = res.result();
					con.queryWithParams("SELECT * FROM users WHERE UPPER(email) = UPPER(?)", new JsonArray().add(email), searchRes -> {
						con.close();
						if(searchRes.succeeded()) {
							ResultSet set = searchRes.result();
							if(set.getNumRows() > 0) {
								JsonObject user = set.getRows().get(0);
								if(BCrypt.checkpw(password, user.getString("hash"))) {
									ctx.session().put("user", user.getLong("id"));
									ctx.response().setStatusCode(302).putHeader("location", "/").end();
									return;
								} else {
									ctx.put("error", "invalid username or password.");
								}
							} else {
								ctx.put("error", "invalid username or password");
							}
						} else {
							searchRes.cause().printStackTrace();
						}
						render(ctx, "index");
					});
				} else {
					res.cause().printStackTrace();
					ctx.put("error", "invalid username or password.");
					render(ctx, "index");
				}
			});
		});
		
		// handle a register request
		router.post("/register").handler(ctx -> {
			String firstName = ctx.request().getFormAttribute("first_name").trim();
			String lastName = ctx.request().getFormAttribute("last_name").trim();
			String email = ctx.request().getFormAttribute("email").trim();
			String password = ctx.request().getFormAttribute("password");
			
			// mark that we are working with the register form (for errors)
			ctx.put("form", "register");
			
			// check if this is a valid request
			if(firstName.length() == 0 || lastName.length() == 0 || email.length() == 0 || password.length() < 6) {
				ctx.put("error", "all fields are required and a password must be at least 6 characters in length.");
				render(ctx, "index");
			} else {
				client.getConnection(res -> {
					if(res.succeeded()) {
						// create the array of data to insert
						JsonArray insert = new JsonArray()
							.add(firstName)
							.add(lastName)
							.add(email)
							.add(BCrypt.hashpw(password, BCrypt.gensalt(12)));
						
						// attempt to insert the data
						SQLConnection con = res.result();
						con.updateWithParams("INSERT INTO users (first_name, last_name, email, hash) VALUES (UPPER(?), UPPER(?), UPPER(?), ?)", insert, insertRes -> {
							con.close();
							if(insertRes.succeeded()) {
								UpdateResult result = insertRes.result();
								if(result.getUpdated() == 1) {
									ctx.session().put("user", result.getKeys().getLong(0));
									ctx.response().setStatusCode(302).putHeader("location", "/").end();
									return;
								} else {
									ctx.put("error", "a user with the given email already exists.");
								}
							} else {
								insertRes.cause().printStackTrace();
								ctx.put("error", "a user with the given email already exists.");
							}
							render(ctx, "index");
						});
					} else {
						res.cause().printStackTrace();
						render(ctx, "index");
					}
				});
			}
		});
		
		// set the static handler for serving static files (mostly assets)
		router.route().handler(StaticHandler.create().setCachingEnabled(false));
		
		// create and start the http server
		getVertx().createHttpServer().requestHandler(router::accept).listen(80);
	}
	
	private void render(RoutingContext ctx, String path) {
		templateEngine.render(ctx, "templates/" + path + ".templ", res -> {
			if(res.succeeded()) {
				ctx.response().end(res.result());
			} else {
				ctx.fail(res.cause());
			}
		});
	}
	
	public static void main(String[] args) {
		Vertx.vertx().deployVerticle(new VolitionService());
	}

}