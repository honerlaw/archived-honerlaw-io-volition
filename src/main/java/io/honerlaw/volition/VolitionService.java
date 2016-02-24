package io.honerlaw.volition;

import java.lang.reflect.Method;
import java.util.Set;

import org.reflections.Reflections;

import io.honerlaw.volition.handler.Route;
import io.honerlaw.volition.handler.RouteRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.MVELTemplateEngine;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class VolitionService extends AbstractVerticle {
	
	// create the template engine
	private final MVELTemplateEngine templateEngine = MVELTemplateEngine.create().setMaxCacheSize(0);
	
	@Override
	public void start() {
		
		// disable file caching by the template engine (for development)
		System.setProperty("vertx.disableFileCaching", "true");
		
		// create the database connection pool
		JDBCClient client = JDBCClient.createShared(getVertx(), new JsonObject()
				.put("url", "")
				.put("user", "")
				.put("password", ""));
		
		// create the router
		Router router = Router.router(getVertx());
		
		// create the session / cookie handler
		router.route().handler(CookieHandler.create());
		router.route().handler(SessionHandler.create(LocalSessionStore.create(getVertx())));
		router.route().handler(BodyHandler.create());
		
		// find all of the classes that have routes defined for them
		Set<Class<?>> classes = new Reflections().getTypesAnnotatedWith(Route.class);
		for(Class<?> clazz : classes) {
			try {
				Object instance = clazz.newInstance();
				Route classRoute = clazz.getAnnotation(Route.class);
				
				// find all of the methods in the class that has routes defined
				for(Method method : clazz.getDeclaredMethods()) {
					Route methodRoute = method.getAnnotation(Route.class);
					if(methodRoute != null) {
						
						// create the route, replace any duplicated forward slashes with a single forward slash
						String route = (classRoute.route() + methodRoute.route()).trim().replaceAll("/+", "/");
						
						// register the route
						router.route(methodRoute.method(), route).handler(ctx -> {
							
							// if the route requires authentication, check that they are logged in
							if(methodRoute.auth()) {
								if(ctx.session().get("user") == null) {
									ctx.response().setStatusCode(401).setStatusMessage("Unauthorized").end("You must be logged in to view this page.");
									return;
								}
							}
							
							// request may block event thread so execute in a worker
							getVertx().executeBlocking(fut -> {
								
								// handle the request
								try {
									method.invoke(instance, new RouteRequest(ctx, fut, client));
								} catch (Exception e) {
									e.printStackTrace();
								}
								
							}, res -> {
								
								// handle the result
								if(res.succeeded()) {
									
									// if the execution succeed we will be sending out success status codes
									Object temp = res.result();
									if(temp instanceof String) {
										ctx.put("page", "templates/" + (String) temp + ".templ");
										render(ctx);
									} else if(temp == null) {
										ctx.put("page", "templates" + route + ".templ");
										render(ctx);
									} else if(temp instanceof HttpServerResponse) {
										((HttpServerResponse) temp).end();
									} else if(temp instanceof JsonObject || temp instanceof JsonArray) {
										ctx.response().setStatusCode(200).putHeader("Content-Type", "application/json").end(temp.toString());
									}
									
								} else {
									
									// if the execution failed we will be sending out error status codes
									if(res.cause() != null) {
										res.cause().printStackTrace();
									}									
									Object temp = res.result();
									if(temp instanceof Throwable) {
										((Throwable) temp).printStackTrace();
									} else {
										System.err.println(temp);
									}
									ctx.response().setStatusCode(500).setStatusMessage("Internal Server Error").end("Internal Server Error");
								}
							});
						});
					}
				}
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		// set the static handler for serving static files (mostly assets)
		router.route().handler(StaticHandler.create().setCachingEnabled(false));
		
		// create and start the http server
		getVertx().createHttpServer().requestHandler(router::accept).listen(80);
	}
	
	private void render(RoutingContext ctx) {
		templateEngine.render(ctx, "templates/index.templ", templateRes -> {
			if(templateRes.succeeded()) {
				ctx.response().end(templateRes.result());
			} else {
				ctx.fail(templateRes.cause());
			}
		});
	}
	
	public static void main(String[] args) {
		Vertx.vertx().deployVerticle(new VolitionService());
	}

}