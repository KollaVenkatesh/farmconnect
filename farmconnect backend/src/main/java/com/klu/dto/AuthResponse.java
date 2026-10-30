package com.klu.dto;
import com.klu.model.Role;
public class AuthResponse {
 private String userId,name,email,message,token; private Role role;
 public AuthResponse() {}
 public AuthResponse(String userId,String name,String email,Role role,String message,String token){this.userId=userId;this.name=name;this.email=email;this.role=role;this.message=message;this.token=token;}
 public java.util.Map<String,Object> getUser(){return java.util.Map.of("userId",userId,"name",name,"email",email,"role",role);}
 public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public Role getRole(){return role;} public void setRole(Role v){role=v;}
 public String getMessage(){return message;} public void setMessage(String v){message=v;}
 public String getToken(){return token;} public void setToken(String v){token=v;}
}
