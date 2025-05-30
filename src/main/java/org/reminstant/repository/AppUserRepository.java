package org.reminstant.repository;

import org.reminstant.model.AppUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends CrudRepository<AppUser, Long> {

  Optional<AppUser> getAppUserByUsername(String username);

  boolean existsAppUserByUsername(String username);
}