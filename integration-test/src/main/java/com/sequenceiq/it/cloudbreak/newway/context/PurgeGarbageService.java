package com.sequenceiq.it.cloudbreak.newway.context;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.StackEntity;

@Service
public class PurgeGarbageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurgeGarbageService.class);

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private List<Purgable> purgables;

    public <T> void purge() {
        LOGGER.info("purge has started");
        TestContext testContext = applicationContext.getBean(TestContext.class);
        testContext.as();
        purge(testContext);
        testContext.shutdown();
        LOGGER.info("purge has finished");
    }

    private <T> void purge(TestContext testContext) {
        StackEntity stackEntity = applicationContext.getBean(StackEntity.class, testContext);
        testContext.when(stackEntity, (testContext1, entity, cloudbreakClient) -> {
            purgables.forEach(purgable -> {
                Collection<Object> all = purgable.getAll(cloudbreakClient);
                all = all.stream()
                        .filter(purgable::deletable)
                        .collect(Collectors.toList());
                LOGGER.info("Purge all {}, count: {}", purgable.getClass().getSimpleName(), all.size());
                all.forEach(e -> purgable.delete(e, cloudbreakClient));
            });
            return entity;
        });
    }
}
