package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.entity.Role;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    User findUserByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findUserByEmail(String email);

    Optional<User> findByOauthProviderIdAndOauthProvider(String oauthProviderId, com.hitendra.turf_booking_backend.entity.OAuthProvider oauthProvider);

    /**
     * Find all users with a specific role (paginated)
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Find all users excluding admins and managers (paginated)
     */
    @Query("SELECT u FROM User u WHERE u.role = 'USER'")
    Page<User> findAllRegularUsers(Pageable pageable);
}
