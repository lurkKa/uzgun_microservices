package com.uz.uzgun_microservices.user.repositroies;

import com.uz.uzgun_microservices.user.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

}
