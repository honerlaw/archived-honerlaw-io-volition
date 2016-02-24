package io.honerlaw.volition.handler.user.friend;

import io.honerlaw.volition.handler.Route;
import io.honerlaw.volition.handler.RouteRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

@Route(route = "/friend")
public class FriendHandler {
	
	
	@Route(route = "/search", auth = true)
	public void search(RouteRequest req) {
		
		String query = '%' + req.context().request().getParam("q") + '%';
		if(query.length() == 2) {
			req.complete(new JsonObject().put("results", new JsonArray()));
			return;
		}
		
		
		long id = (long) req.context().session().get("user");

		req.jdbc().getConnection(res -> {
			if(res.succeeded()) {
				
				SQLConnection con = res.result();
				
				// friend requests
				// where we are the requestor and they are the requestee
				// where we are the requestee and they are the requestor
				
				// basically we need to know if it is a friend request
				// or a friend
				// so we need to know the status
				
				// friend
				// where they are the user and we are the friend
				
				// where their id is not equal to our id
				String q = "SELECT users.id, first_name, last_name, email, friends_requests.id as frid, friends_requests.requestor_id as requestor, friends_requests.requestee_id as requestee, friends.id as fid FROM users LEFT JOIN friends_requests ON (users.id = friends_requests.requestor_id AND friends_requests.requestee_id = ?) OR (users.id = friends_requests.requestee_id AND friends_requests.requestor_id = ?) LEFT JOIN friends ON users.id = friends.users_id and friends.friends_id = ? WHERE UPPER(users.email) LIKE UPPER(?) AND users.id != ?";
				con.queryWithParams(q, new JsonArray().add(id).add(id).add(id).add(query).add(id), searchRes -> {
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
		
		// we are searching for users to be our friend (or are already our friend)
		// basically list all users, whether they are friends or not, and whether there
		// is a pending friend request for the users
	}

}
