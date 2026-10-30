package com.klu.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class User {
    @Id private String userId;
    private String name;
    @Indexed(unique = true) private String username;
    @Indexed(unique = true) private String email;
    private String password;
    private Role role;
    private String razorpayAccountId;
    private String payoutStatus;
    private String payoutAccountLastDigits;
    private String preferredLanguage = "en";
    public User() {}
    public User(String userId, String name, String email, String password, Role role) { this.userId=userId; this.name=name; this.email=email; this.password=password; this.role=role; }
    public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public Role getRole(){return role;} public void setRole(Role v){role=v;}
    public String getRazorpayAccountId(){return razorpayAccountId;} public void setRazorpayAccountId(String v){razorpayAccountId=v;}
    public String getPayoutStatus(){return payoutStatus;} public void setPayoutStatus(String v){payoutStatus=v;}
    public String getPayoutAccountLastDigits(){return payoutAccountLastDigits;} public void setPayoutAccountLastDigits(String v){payoutAccountLastDigits=v;}
    public String getPreferredLanguage(){return preferredLanguage == null ? "en" : preferredLanguage;}
    public void setPreferredLanguage(String v){preferredLanguage=v;}
}
