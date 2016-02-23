package io.honerlaw.volition.handler;

import io.vertx.core.Future;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.RoutingContext;

public class RouteRequest {
	
	private final RoutingContext context;
	private final Future<Object> future;
	private final JDBCClient jdbc;
	
	public RouteRequest(RoutingContext context, Future<Object> future, JDBCClient jdbc) {
		this.context = context;
		this.future = future;
		this.jdbc = jdbc;
	}
	
	public RoutingContext context() {
		return context;
	}
	
	public JDBCClient jdbc() {
		return jdbc;
	}
	
	public void complete() {
		future.complete();
	}
	
	public void complete(Object obj) {
		future.complete(obj);
	}
	
	public void fail(Throwable throwable) {
		future.fail(throwable);
	}
	
	public void fail(String message) {
		future.fail(message);
	}

}
