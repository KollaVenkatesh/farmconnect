package com.klu.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document("products")
public class Product {
    @Id private String productId;
    private String productName, category, description;
    private Double price;
    private Integer stock;
    private String imageUrl;
    private Double rating;
    private String status, unit, location, farmer, farmerId;
    private transient boolean ownedByCurrentUser;
    public Product() {}
    public Product(String productId,String productName,String category,String description,Double price,Integer stock,String imageUrl,Double rating,String status){this.productId=productId;this.productName=productName;this.category=category;this.description=description;this.price=price;this.stock=stock;this.imageUrl=imageUrl;this.rating=rating;this.status=status;}
    public String getProductId(){return productId;} public void setProductId(String v){productId=v;}
    public String getProductName(){return productName;} public void setProductName(String v){productName=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public Double getPrice(){return price;} public void setPrice(Double v){price=v;}
    public Integer getStock(){return stock;} public void setStock(Integer v){stock=v;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
    public Double getRating(){return rating;} public void setRating(Double v){rating=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getId(){return productId;} public String getName(){return productName;}
    public String getUnit(){return unit==null?"kg":unit;} public void setUnit(String v){unit=v;}
    public String getLocation(){return location==null?"Local farm":location;} public void setLocation(String v){location=v;}
    public String getFarmer(){return farmer==null?"FarmConnect farmer":farmer;} public void setFarmer(String v){farmer=v;}
    @JsonIgnore public String getFarmerId(){return farmerId;} public void setFarmerId(String v){farmerId=v;}
    public boolean isOwnedByCurrentUser(){return ownedByCurrentUser;} public void setOwnedByCurrentUser(boolean v){ownedByCurrentUser=v;}
}
