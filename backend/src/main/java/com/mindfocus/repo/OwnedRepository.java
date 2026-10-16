package com.mindfocus.repo;

import com.mindfocus.domain.OwnedEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface OwnedRepository<T extends OwnedEntity> extends JpaRepository<T, Long> {
    List<T> findByUserId(Long userId);

    List<T> findByUserId(Long userId, Sort sort);

    Optional<T> findByIdAndUserId(Long id, Long userId);
}
