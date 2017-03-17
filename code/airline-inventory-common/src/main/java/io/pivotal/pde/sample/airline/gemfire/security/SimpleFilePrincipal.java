package io.pivotal.pde.sample.airline.gemfire.security;

import java.security.Principal;

public class SimpleFilePrincipal implements Principal {

	private static String LOOK_ROLE = "look";
	private static String BOOK_ROLE = "book";
	private static String SYSTEM_ROLE = "system";
	
	private String username;
	private String role;
	private String password;
	
	public SimpleFilePrincipal(String username, String password, String role){
		this.username = username;
		this.password = password;
		this.role = role;
		
		if (!role.equals(LOOK_ROLE))
			if (!role.equals(BOOK_ROLE))
				if (!role.equals(SYSTEM_ROLE)) 
					throw new RuntimeException("invalid role: " + role);
	}
	
	@Override
	public String getName() {
		return username;
	}
	
	public boolean canLook(){
		return  (role.equals(LOOK_ROLE) || role.equals(BOOK_ROLE));
	}
	
	public boolean canBook(){
		return role.equals(BOOK_ROLE);
	}
	
	public boolean isSystem(){
		return role.equals(SYSTEM_ROLE);
	}
	
	public boolean authenticate(String password){
		return this.password.equals(password);
	}

}
