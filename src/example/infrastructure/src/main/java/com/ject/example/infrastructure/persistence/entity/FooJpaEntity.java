package com.ject.example.infrastructure.persistence.entity;

import com.ject.example.domain.domain.FooStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "example_foo")
public class FooJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FooStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "foo_id")
    @OrderBy("id ASC")
    private List<BarJpaEntity> bars = new ArrayList<>();

    protected FooJpaEntity() {
    }

    public FooJpaEntity(Long id, String name, FooStatus status, List<BarJpaEntity> bars) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.bars = new ArrayList<>(bars);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public FooStatus getStatus() {
        return status;
    }

    public List<BarJpaEntity> getBars() {
        return bars;
    }
}
