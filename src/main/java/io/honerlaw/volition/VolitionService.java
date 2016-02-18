package io.honerlaw.volition;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.MVELTemplateEngine;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class VolitionService extends AbstractVerticle {
	
	private final MVELTemplateEngine templateEngine = MVELTemplateEngine.create().setMaxCacheSize(0);
	
	@Override
	public void start() {
		
		System.setProperty("vertx.disableFileCaching", "true");
		
		Router router = Router.router(getVertx());
		
		// create the session handler
		router.route().handler(SessionHandler.create(LocalSessionStore.create(getVertx())));
		
		// create the body handler
		router.route().handler(BodyHandler.create());
	
		// login / register page OR main account home page
		router.get("/").handler(ctx -> {
			render(ctx, "index");
		});
		
		// handle a login request
		router.post("/login").handler(ctx -> {
	
		});
		
		// handle a register request
		router.post("/register").handler(ctx -> {
			
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