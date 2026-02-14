package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "gstin")
    private String gstin;

    // OAuth Provider Information
    @Column(name = "oauth_provider")
    @Enumerated(EnumType.STRING)
    private OAuthProvider oauthProvider;  // GOOGLE, APPLE, null for OTP users

    @Column(name = "oauth_provider_id")
    private String oauthProviderId;  // Unique ID from OAuth provider

    @Column(name = "email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    // Account status for deletion compliance
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column
    @Builder.Default
    private Instant createdAt = Instant.now();

    // Relationship to user profile (one-to-one)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile userProfile;

    // Relationship to admin profile (one-to-one)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private AdminProfile adminProfile;


    @PrePersist
    protected void onCreate() {
        if (role == null) {
            role = Role.USER;  // Set default role
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }
    }
}
