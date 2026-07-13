package com.jejulocaltime.api.repository;

import com.jejulocaltime.api.domain.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    boolean existsByProductIdAndStatusIn(Long productId, Collection<PaymentOrder.Status> statuses);
}
