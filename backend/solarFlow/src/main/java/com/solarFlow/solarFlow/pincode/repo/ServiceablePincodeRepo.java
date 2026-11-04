package com.solarFlow.solarFlow.pincode.repo;

import com.solarFlow.solarFlow.pincode.domain.ServiceablePincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceablePincodeRepo extends JpaRepository<ServiceablePincode, UUID> {

    boolean existsByPincode(String pincode);

    boolean existsByPincodeAndActiveTrue(String pincode);

    List<ServiceablePincode> findAllByOrderByPincodeAsc();
}