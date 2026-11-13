package team7.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import team7.model.Child;

public interface ChildRepository extends JpaRepository<Child, Long> {
}
