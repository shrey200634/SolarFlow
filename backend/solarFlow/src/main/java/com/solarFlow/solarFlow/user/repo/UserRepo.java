package com.solarFlow.solarFlow.user.repo;


import com.solarFlow.solarFlow.user.domain.Role;
import com.solarFlow.solarFlow.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, UUID> {

    Optional<User>  findByEmail(String email );
    boolean existsByEmail(String email);
    List<User> findByRoleInAndActiveTrueOrderByFullNameAsc(Collection<Role> roles);
}
