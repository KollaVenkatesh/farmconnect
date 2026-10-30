package com.klu.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("cart_items")
@CompoundIndex(name = "user_product_unique", def = "{'userEmail': 1, 'productId': 1}", unique = true)
public class CartItem {

    @Id
    private String id;
    private String userEmail;
    private String productId;
    private Integer quantity = 1;

    public CartItem() {
    }

    public CartItem(String userEmail, String productId, Integer quantity) {
        this.userEmail = userEmail;
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
