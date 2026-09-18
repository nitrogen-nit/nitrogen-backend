package vn.nitrogen.identity.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.nitrogen.identity.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findAllByIdIn(Collection<UUID> ids);
}
