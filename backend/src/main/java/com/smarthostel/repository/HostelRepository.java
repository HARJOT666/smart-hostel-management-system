package com.smarthostel.repository;

import com.smarthostel.model.Hostel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostelRepository extends JpaRepository<Hostel, Long> {

    List<Hostel> findAllByOrderByIdAsc();

    boolean existsByHostelNameIgnoreCase(String hostelName);
}
