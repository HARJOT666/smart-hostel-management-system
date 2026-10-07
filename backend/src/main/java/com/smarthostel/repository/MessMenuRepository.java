package com.smarthostel.repository;

import com.smarthostel.model.MessMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessMenuRepository extends JpaRepository<MessMenu, Long> {

    List<MessMenu> findByHostelId(Long hostelId);
}
