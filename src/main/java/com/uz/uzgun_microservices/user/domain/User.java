package com.uz.uzgun_microservices.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long userId;

  private Long accountId;

  @Column(name = "checkout_bypass_permitted", nullable = false)
  private boolean checkoutBypassPermitted;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Column(nullable = false)
  private boolean active = true;

  @Column(length = 13)
  private String phone;

  @Column(length = 254)
  private String email;

  @Column(length = 100)
  private String firstName;

  @Column(length = 100)
  private String lastName;

  @Column(length = 100)
  private String patronymic;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Sex sex;

  private LocalDate birthDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private PreferredDelivery preferredDelivery;

  @Column(nullable = false)
  private boolean createdFromTezkorApp;

  @Version private long version;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;

  @UpdateTimestamp private Instant updatedAt;
}
