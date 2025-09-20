package com.example.NIMASA.NYSC.Clearance.Form.repository;

import com.example.NIMASA.NYSC.Clearance.Form.model.CorpsMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorpsMemberRepository extends JpaRepository<CorpsMember, UUID> {

    Optional<CorpsMember> findByNameAndActive(String name, boolean active);

    Optional<CorpsMember> findByName(String name);

    List<CorpsMember> findByDepartment(String department);

    List<CorpsMember> findByActive(boolean active);

    boolean existsByName(String name);

    boolean existsByNameAndActive(String name, boolean active);
}