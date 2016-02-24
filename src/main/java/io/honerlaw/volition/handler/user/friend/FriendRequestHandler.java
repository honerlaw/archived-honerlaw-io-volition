package io.honerlaw.volition.handler.user.friend;

import io.honerlaw.volition.handler.Route;
import io.honerlaw.volition.handler.RouteRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

@Route(route = "/friend/request")
public class FriendRequestHandler {
	
	// requestee = the person who we are sending the request to
	// requestor = the person who is sending the request
	/*
	CREATE TABLE friends_requests (
		id BIGSERIAL PRIMARY KEY,
		requestor_id BIGINT NOT NULL REFERENCES users (id),
		requestee_id BIGINT NOT NULL REFERENCES users (id),
		created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	
	CREATE TABLE friends (
		id BIGSERIAL PRIMARY KEY,
		users_id BIGINT NOT NULL REFERENCES users (id),
		friends_id BIGINT NOT NULL REFERENCES users (id),
		created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
		constraint friends_unique_constraint UNIQUE (users_id, friends_id)
	);
	*/
	
	// sent by the requestor, retreived by the requestee
	@Route(method = HttpMethod.POST, auth = true)
	public void request(RouteRequest req) {
		
		// get the id of the user we are requesting
		long requestee = Long.parseLong(req.context().request().getFormAttribute("id"));
		long requestor = (long) req.context().session().get("user");
		
		// make sure they are not trying to add themselves
		if(requestee == requestor) {
			req.complete(req.context().response().setStatusCode(400).setStatusMessage("You can not request to be friends with yourself."));
			return;
		}
		
		// get the database connection
		req.jdbc().getConnection(res -> {
			if(res.succeeded()) {

				// insert the friend request into the database
				SQLConnection con = res.result();
				con.updateWithParams("INSERT INTO friends_requests (requestor_id, requestee_id) values (?, ?)", new JsonArray().add(requestor).add(requestee), insertRes -> {
					con.close();
					if(insertRes.succeeded()) {
						req.complete(new JsonObject());
					} else {
						req.fail(insertRes.cause());
					}
				});
				
			} else {
				req.fail(res.cause());
			}
		});
	}
	
	// only the requestor can cancel a friend request
	@Route(route = "/cancel", method = HttpMethod.POST, auth = true)
	public void cancel(RouteRequest req) {
		
		// get the ids needed to complete the request
		long request = Long.parseLong(req.context().request().getFormAttribute("id"));
		long requestor = (long) req.context().session().get("user");
		
		// get the database connection
		req.jdbc().getConnection(res -> {
			if(res.succeeded()) {
				
				// delete the friend request
				SQLConnection con = res.result();
				con.updateWithParams("DELETE FROM friends_requests WHERE requestor_id = ? AND id = ?", new JsonArray().add(requestor).add(request), deleteRes -> {
					con.close();
					if(deleteRes.succeeded()) {
						req.complete(new JsonObject());
					} else {
						req.fail(deleteRes.cause());
					}
				});
				
			} else {
				req.fail(res.cause());
			}
		});
	}
	
	// only the requestee can accept the friend request
	@Route(route = "/accept", method = HttpMethod.POST, auth = true)
	public void accept(RouteRequest req) {
		
		// get the required ids
		long requestor = Long.parseLong(req.context().request().getFormAttribute("id"));
		long requestee = (long) req.context().session().get("user");
		
		// get the database connection
		req.jdbc().getConnection(res -> {
			if(res.succeeded()) {
				
				// delete the friend request
				SQLConnection con = res.result();
				con.updateWithParams("DELETE FROM friends_requests WHERE requestee_id = ? AND requestor_id = ?", new JsonArray().add(requestee).add(requestor), deleteRes -> {
					if(deleteRes.succeeded()) {

						// make sure something was deleted
						if(deleteRes.result().getUpdated() > 0) {
						
							// insert the friendship status
							con.updateWithParams("INSERT INTO friends (users_id, friends_id) VALUES (?, ?), (?, ?)", new JsonArray().add(requestor).add(requestee).add(requestee).add(requestor), insertRes -> {
								con.close();
								if(insertRes.succeeded()) {
									req.complete(new JsonObject());
								} else {
									req.fail(insertRes.cause());
								}
							});
						
						} else {
							req.fail("Friend request was not found.");
							con.close();
						}
						
					} else {
						req.fail(deleteRes.cause());
						con.close();
					}
				});
				
			} else {
				req.fail(res.cause());
			}
		});
	}
	
	// only the requestee can deny the friend request
	@Route(route = "/deny", method = HttpMethod.DELETE, auth = true)
	public void deny(RouteRequest req) {
		
		// get the ids needed to complete the request
		long request = Long.parseLong(req.context().request().getFormAttribute("id"));
		long requestee = (long) req.context().session().get("user");
		
		// get the database connection
		req.jdbc().getConnection(res -> {
			if(res.succeeded()) {
				
				// delete the friend request
				SQLConnection con = res.result();
				con.updateWithParams("DELETE FROM friends_requests WHERE requestee_id = ? AND id = ?", new JsonArray().add(requestee).add(request), deleteRes -> {
					con.close();
					if(deleteRes.succeeded()) {
						req.complete(new JsonObject());
					} else {
						req.fail(deleteRes.cause());
					}
				});
				
			} else {
				req.fail(res.cause());
			}
		});
		
	}

}
