package org.nicetu.spb.userservice.repository;

import org.nicetu.spb.userservice.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepositoryPaging extends PagingAndSortingRepository<User, Long> {
    Page<User> findAll(Pageable pageable);
}
