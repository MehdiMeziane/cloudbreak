package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.lang.reflect.Method;

import org.slf4j.MDC;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterTemplateCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.LaunchClusterFromTemplateAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class ClusterTemplateTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Method method, Object[] data) {
        MDC.put("suite", method.getDeclaringClass().getSimpleName() + '.' + method.getName());
        TestContext testContext = (TestContext) data[0];

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = "testContext")
    public void testClusterTemplate(TestContext testContext) {
        testContext.given(StackEntity.class)
                .given("se", StackEntity.class)
                .given(ClusterTemplateEntity.class).withStackTemplate("se")
                .when(new ClusterTemplateCreateAction())
                .when(new LaunchClusterFromTemplateAction("se"))
                .await(STACK_AVAILABLE, key("se"))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
