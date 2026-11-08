package com.inzinjeri.rs_projekat.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.mapping.ToOne;

@Entity
@Data
@Table(name="zadatak", uniqueConstraints = @UniqueConstraint(name = "uk_zadatak_github_folder",
        columnNames = {"github_repo", "github_folder"}))
public class Zadatak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String naziv;

    @OneToOne
    @JoinColumn(name = "grupa_id", unique = true)
    private Grupa grupa;

    @Column(nullable = false, unique = true, length = 700)
    private String repoPutanja;

    @Column(length = 140)
    private String githubRepo;
    @Column(length = 500)
    private String githubFolder;
    @Column(length = 64)
    private String githubCommit;
}
