package com.klu.dto;

import java.util.List;

public class RecipeSuggestionResponse {
    private String dish;
    private String prepTime;
    private String cookTime;
    private String servings;
    private String description;
    private List<MatchedIngredient> matchedProducts;
    private List<String> pantryItems;
    private List<String> instructions;
    private double estimatedTotal;

    public RecipeSuggestionResponse() {}

    public RecipeSuggestionResponse(String dish, String prepTime, String cookTime, String servings,
                                    String description, List<MatchedIngredient> matchedProducts,
                                    List<String> pantryItems, List<String> instructions, double estimatedTotal) {
        this.dish = dish;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.servings = servings;
        this.description = description;
        this.matchedProducts = matchedProducts;
        this.pantryItems = pantryItems;
        this.instructions = instructions;
        this.estimatedTotal = estimatedTotal;
    }

    public String getDish() { return dish; }
    public void setDish(String dish) { this.dish = dish; }

    public String getPrepTime() { return prepTime; }
    public void setPrepTime(String prepTime) { this.prepTime = prepTime; }

    public String getCookTime() { return cookTime; }
    public void setCookTime(String cookTime) { this.cookTime = cookTime; }

    public String getServings() { return servings; }
    public void setServings(String servings) { this.servings = servings; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<MatchedIngredient> getMatchedProducts() { return matchedProducts; }
    public void setMatchedProducts(List<MatchedIngredient> matchedProducts) { this.matchedProducts = matchedProducts; }

    public List<String> getPantryItems() { return pantryItems; }
    public void setPantryItems(List<String> pantryItems) { this.pantryItems = pantryItems; }

    public List<String> getInstructions() { return instructions; }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }

    public double getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(double estimatedTotal) { this.estimatedTotal = estimatedTotal; }

    public static class MatchedIngredient {
        private String productId;
        private String productName;
        private String category;
        private Double price;
        private String unit;
        private String farmer;
        private String location;
        private String imageUrl;
        private String role;
        private int suggestedQuantity;

        public MatchedIngredient() {}

        public MatchedIngredient(String productId, String productName, String category, Double price,
                                 String unit, String farmer, String location, String imageUrl,
                                 String role, int suggestedQuantity) {
            this.productId = productId;
            this.productName = productName;
            this.category = category;
            this.price = price;
            this.unit = unit;
            this.farmer = farmer;
            this.location = location;
            this.imageUrl = imageUrl;
            this.role = role;
            this.suggestedQuantity = suggestedQuantity;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public String getFarmer() { return farmer; }
        public void setFarmer(String farmer) { this.farmer = farmer; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public int getSuggestedQuantity() { return suggestedQuantity; }
        public void setSuggestedQuantity(int suggestedQuantity) { this.suggestedQuantity = suggestedQuantity; }
    }
}
