package io.honerlaw.volition.handler.index;

import io.honerlaw.volition.handler.Route;
import io.honerlaw.volition.handler.RouteRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

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
	
	@Route(route = "/search", method = HttpMethod.GET, auth = true)
	public void search(RouteRequest req) {
		
		String query = '%' + req.context().request().getParam("q") + '%';
		if(query.length() == 2) {
			req.complete(new JsonObject().put("results", new JsonArray()));
			return;
		}

		req.jdbc().getConnection(res -> {
			if(res.succeeded()) {
				
				SQLConnection con = res.result();
				con.queryWithParams("SELECT id, first_name, last_name, email FROM users WHERE UPPER(email) LIKE UPPER(?) AND id != ?", new JsonArray().add(query).add((long) req.context().session().get("user")), searchRes -> {
					con.close();
					if(searchRes.succeeded()) {
						req.complete(new JsonObject().put("results", searchRes.result().getRows()));
					} else {
						searchRes.cause().printStackTrace();
						req.complete(new JsonObject().put("results", new JsonArray()));
					}
				});
				
			} else {
				res.cause().printStackTrace();
				req.complete(new JsonObject().put("results", new JsonArray()));
			}
		});
	}

}
