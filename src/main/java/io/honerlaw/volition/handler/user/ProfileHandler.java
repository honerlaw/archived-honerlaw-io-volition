package io.honerlaw.volition.handler.user;

import io.honerlaw.volition.handler.Route;
import io.honerlaw.volition.handler.RouteRequest;

@Route(route = "/profile")
public class ProfileHandler {
	
	@Route(route = "", auth = true)
	public void profile(RouteRequest req) {
		req.complete();
	}

}
