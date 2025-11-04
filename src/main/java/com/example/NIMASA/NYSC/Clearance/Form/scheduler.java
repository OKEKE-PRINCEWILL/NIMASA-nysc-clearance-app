package com.example.NIMASA.NYSC.Clearance.Form;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class scheduler {

    @PersistenceContext
    private EntityManager entityManager;

    // This method runs every 5 minutes
    @Scheduled(cron = "0 */10 * * * *")
    public void keepAlive() {
        // Simple lightweight native query, adjust if you want something more meaningful
        entityManager.createNativeQuery("SELECT 1").getSingleResult();
        // You can add logs if desired:
         System.out.println("[KeepAliveScheduler] Keep-alive executed.");
    }
}