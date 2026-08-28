package com.servifood.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "customer_addresses")
public class CustomerAddress extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @Size(max = 50) @Column(length = 50) private String label;
    @NotBlank @Size(max = 250) @Column(nullable = false, length = 250) private String address;
    @NotBlank @Size(max = 120) @Column(nullable = false, length = 120) private String neighborhood;
    @Size(max = 500) @Column(length = 500) private String reference;
    @Column(name = "is_primary", nullable = false) private boolean primaryAddress;
    protected CustomerAddress() {}
    public CustomerAddress(String label, String address, String neighborhood, String reference, boolean primaryAddress) {
        this.label = label; this.address = address; this.neighborhood = neighborhood; this.reference = reference; this.primaryAddress = primaryAddress;
    }
    void assignTo(Customer customer) { this.customer = customer; }
    public Long getId() { return id; } public Customer getCustomer() { return customer; } public String getAddress() { return address; }
    public String getNeighborhood() { return neighborhood; } public boolean isPrimaryAddress() { return primaryAddress; }
    public String getLabel() { return label; } public String getReference() { return reference; }
}
