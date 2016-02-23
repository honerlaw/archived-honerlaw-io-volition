package io.honerlaw.volition.handler.user;

import org.mindrot.jbcrypt.BCrypt;

import io.honerlaw.volition.handler.Route;
import io.honerlaw.volition.handler.RouteRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

@Route(route = "/")
public class AuthHandler {
	
	@Route(route = "/logout")
	public void logout(RouteRequest req) {
		req.context().session().destroy();
		req.complete(req.context().response().setStatusCode(302).putHeader("location", "/"));
	}
	
	@Route(route = "/login", method = HttpMethod.POST)
	public void login(RouteRequest req) {		
		
		String email = req.context().request().getFormAttribute("email").trim();
		String password = req.context().request().getFormAttribute("password");
		req.jdbc().getConnection(res -> {
			if(res.succeeded()) {
				SQLConnection con = res.result();
				con.queryWithParams("SELECT * FROM users WHERE UPPER(email) = UPPER(?)", new JsonArray().add(email), searchRes -> {
					con.close();
					if(searchRes.succeeded()) {
						ResultSet set = searchRes.result();
						if(set.getNumRows() > 0) {
							JsonObject user = set.getRows().get(0);
							if(BCrypt.checkpw(password, user.getString("hash"))) {
								req.context().session().put("user", user.getLong("id"));
								req.complete(req.context().response().setStatusCode(302).putHeader("location", "/"));
								return;
							} else {
								req.context().put("error", "invalid username or password.");
							}
						} else {
							req.context().put("error", "invalid username or password");
						}
					} else {
						searchRes.cause().printStackTrace();
					}
					req.complete("auth/auth");
				});
			} else {
				res.cause().printStackTrace();
				req.context().put("error", "invalid username or password.");
				req.complete("auth/auth");
			}
		});
		
	}
	
	@Route(route = "/register", method = HttpMethod.POST)
	public void register(RouteRequest req) {

		String firstName = req.context().request().getFormAttribute("first_name").trim();
		String lastName = req.context().request().getFormAttribute("last_name").trim();
		String email = req.context().request().getFormAttribute("email").trim();
		String password = req.context().request().getFormAttribute("password");
		
		// mark that we are working with the register form (for errors)
		req.context().put("form", "register");
		
		// check if this is a valid request
		if(firstName.length() == 0 || lastName.length() == 0 || email.length() == 0 || password.length() < 6) {
			req.context().put("error", "all fields are required and a password must be at least 6 characters in length.");
			req.complete("auth/auth");
		} else {
			req.jdbc().getConnection(res -> {
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
								req.context().session().put("user", result.getKeys().getLong(0));
								req.complete(req.context().response().setStatusCode(302).putHeader("location", "/"));
								return;
							} else {
								req.context().put("error", "a user with the given email already exists.");
							}
						} else {
							insertRes.cause().printStackTrace();
							req.context().put("error", "a user with the given email already exists.");
						}
						req.complete("auth/auth");
					});
				} else {
					res.cause().printStackTrace();
					req.complete("auth/auth");
				}
			});
		}
	}

}
