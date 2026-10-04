package vn.nitrogen.identity.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nitrogen.identity.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
            select distinct u
            from User u
            left join fetch u.roles
            where u.id = :id
            """)
    Optional<User> findWithRolesById(@Param("id") UUID id);

    @Query("""
            select distinct u
            from User u
            left join fetch u.roles
            where u.id in :ids
            """)
    List<User> findAllWithRolesByIdIn(@Param("ids") Collection<UUID> ids);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}