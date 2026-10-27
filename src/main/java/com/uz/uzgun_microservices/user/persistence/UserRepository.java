package com.uz.uzgun_microservices.user.persistence;

import com.uz.uzgun_microservices.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends CrudRepository<User, Long> {
  @Query("select u.checkoutBypassPermitted from User u where u.userId = :userId")
  Optional<Boolean> findCheckoutBypassPermissionById(@Param("userId") long userId);
}
