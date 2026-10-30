package com.klu.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.klu.model.Product;
public interface ProductRepository extends MongoRepository<Product,String> {}
