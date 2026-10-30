package com.klu.repository;
import java.util.List; import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.klu.model.CartItem;
public interface CartItemRepository extends MongoRepository<CartItem,String> {
 List<CartItem> findAllByUserEmail(String userEmail);
 Optional<CartItem> findByUserEmailAndProductId(String userEmail,String productId);
}
