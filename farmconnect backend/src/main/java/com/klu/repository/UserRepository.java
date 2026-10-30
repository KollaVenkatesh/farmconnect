package com.klu.repository;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.klu.model.User;
public interface UserRepository extends MongoRepository<User,String> {
 Optional<User> findByEmail(String email); Optional<User> findByUsername(String username);
 boolean existsByUsername(String username); boolean existsByEmail(String email);
}
