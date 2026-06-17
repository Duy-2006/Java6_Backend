package com.poly.java5.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.poly.java5.Entity.Banner;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BannerService {
	@PersistenceContext
    private EntityManager em;

    public List<Banner> getActiveBanners() {
        return em.createQuery(
            "SELECT b FROM Banner b WHERE b.active = true " +
            "AND (b.start_date IS NULL OR b.start_date <= CURRENT_TIMESTAMP) " +
            "AND (b.end_date IS NULL OR b.end_date >= CURRENT_TIMESTAMP) " +
            "ORDER BY b.position",
            Banner.class
        ).getResultList();
    }

}
