package io.honerlaw.volition.handler.index;

import io.honerlaw.volition.handler.Route;
import io.honerlaw.volition.handler.RouteRequest;

@Route(route = "/")
public class IndexHandler {
	
	@Route(route = "/")
	public void index(RouteRequest req) {
		if(req.context().session().get("user") == null) {
			req.complete("auth/auth");
		} else {			
			req.complete("home");
		}
	}
	
}
