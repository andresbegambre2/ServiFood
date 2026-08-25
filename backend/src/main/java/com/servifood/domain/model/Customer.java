package com.servifood.domain.model;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.servifood.domain.exception.DomainException;

@Entity
@Table(name = "customers")
public class Customer extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 120) @Column(nullable = false, length = 120) private String name;
    @NotBlank @Size(min = 7, max = 30) @Column(nullable = false, length = 30) private String phone;
    @Email @Size(max = 190) @Column(length = 190) private String email;
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true) private List<CustomerAddress> addresses = new ArrayList<>();
    protected Customer() {}
    public Customer(String name, String phone, String email) { this.name = name; this.phone = phone; this.email = email; }
    public void addAddress(CustomerAddress address) {
        if (address.isPrimaryAddress() && addresses.stream().anyMatch(CustomerAddress::isPrimaryAddress)) throw new DomainException("customer can only have one primary address");
        address.assignTo(this); addresses.add(address);
    }
    public Long getId() { return id; } public String getName() { return name; } public String getPhone() { return phone; }
    public String getEmail() { return email; } public List<CustomerAddress> getAddresses() { return List.copyOf(addresses); }
}
