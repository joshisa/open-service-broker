/*
 * Copyright (c) 2018 Swisscom (Switzerland) Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.swisscom.cloud.sb.broker.functional

import com.fasterxml.jackson.databind.ObjectMapper
import com.swisscom.cloud.sb.broker.error.ErrorCode
import com.swisscom.cloud.sb.broker.model.CFService
import com.swisscom.cloud.sb.broker.model.Plan
import com.swisscom.cloud.sb.broker.repository.PlanRepository
import com.swisscom.cloud.sb.broker.repository.ServiceDetailRepository
import com.swisscom.cloud.sb.broker.repository.ServiceInstanceRepository
import com.swisscom.cloud.sb.broker.servicedefinition.ServiceDefinitionInitializer
import com.swisscom.cloud.sb.broker.servicedefinition.dto.ServiceDto
import com.swisscom.cloud.sb.broker.util.servicecontext.ServiceContextHelper
import com.swisscom.cloud.sb.broker.util.test.DummyServiceProvider
import com.swisscom.cloud.sb.client.model.LastOperationState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.servicebroker.model.CloudFoundryContext
import org.springframework.http.HttpStatus
import spock.lang.Shared

import static com.swisscom.cloud.sb.broker.util.Resource.readTestFileContent

class ServiceUpdateFunctionalSpec extends BaseFunctionalSpec {
    @Autowired
    ServiceInstanceRepository serviceInstanceRepository
    @Autowired
    PlanRepository planRepository
    @Autowired
    ServiceDetailRepository serviceDetailRepository
    @Autowired
    ServiceDefinitionInitializer serviceDefinitionInitializer

    @Shared
    boolean serviceDefinitionsSetUp = false

    static String notUpdatableServiceGuid = "updateTest_notUpdateable"
    static String notUpdatablePlanGuid = "updateTest_notUpdateable_plan_a"
    static String secondNotUpdatablePlanGuid = "updateTest_notUpdateable_plan_b"
    static String defaultServiceGuid = "updateTest_Updateable"
    static String defaultPlanGuid = "updateTest_Updateable_plan_a"
    static String secondPlanGuid = "updateTest_Updateable_plan_b"
    static String defaultOrgGuid = "org_id"
    static String defaultSpaceGuid = "space_id"

    static String parameterKey = "mode"
    static String parameterOldValue = "blocking"
    static String parameterNewValue = "open"
    Map<String, Object> parameters

    private String requestServiceProvisioning(
            Map<String, Object> parameters = new HashMap<String, Object>(),
            String planGuid = defaultPlanGuid,
            boolean async = false,
            String serviceGuid = defaultServiceGuid) {

        def serviceInstanceGuid = UUID.randomUUID().toString()
        def context = CloudFoundryContext.builder().
                organizationGuid(defaultOrgGuid).
                spaceGuid(defaultSpaceGuid).
                build()

        serviceLifeCycler.requestServiceProvisioning(
                serviceInstanceGuid,
                serviceGuid,
                planGuid,
                async,
                context,
                parameters)

        serviceLifeCycler.setServiceInstanceId(serviceInstanceGuid)
        return serviceInstanceGuid
    }

    private updatePlans(){
        Plan defaultPlan = serviceLifeCycler.getPlanByGuid(defaultPlanGuid)
        Plan secondPlan = serviceLifeCycler.getPlanByGuid(secondPlanGuid)
        CFService defaultService = serviceLifeCycler.getServiceByGuid(defaultServiceGuid)
        defaultPlan.service = defaultService
        serviceLifeCycler.updateServiceOfPlanInRepository(defaultPlanGuid, defaultService)
        secondPlan.service = defaultService
        serviceLifeCycler.updateServiceOfPlanInRepository(secondPlanGuid, defaultService)

        Plan notUpdatablePlan = serviceLifeCycler.getPlanByGuid(notUpdatablePlanGuid)
        Plan secondNotUpdatablePlan = serviceLifeCycler.getPlanByGuid(secondNotUpdatablePlanGuid)
        CFService notUpdatableService = serviceLifeCycler.getServiceByGuid(notUpdatableServiceGuid)
        notUpdatablePlan.service = notUpdatableService
        secondNotUpdatablePlan.service = notUpdatableService
        serviceLifeCycler.updateServiceOfPlanInRepository(notUpdatablePlanGuid, notUpdatableService)
        serviceLifeCycler.updateServiceOfPlanInRepository(secondNotUpdatablePlanGuid, notUpdatableService)

        serviceLifeCycler.setPlan(defaultPlan)
        serviceLifeCycler.setPlan(secondPlan)
        serviceLifeCycler.setPlan(notUpdatablePlan)
        serviceLifeCycler.setPlan(secondNotUpdatablePlan)

        serviceLifeCycler.addCFServiceToSet(defaultService)
        serviceLifeCycler.addCFServiceToSet(notUpdatableService)
    }

    def setup() {
        if (!serviceDefinitionsSetUp) {
            ServiceDto updateableServiceDto = new ObjectMapper().readValue(
                    readTestFileContent("/service-data/serviceDefinition_updateTest_updateable.json"), ServiceDto)
            ServiceDto notUpdateableServiceDto = new ObjectMapper().readValue(
                    readTestFileContent("/service-data/serviceDefinition_updateTest_notUpdateable.json"), ServiceDto)
            serviceDefinitionInitializer.addOrUpdateServiceDefinitions(updateableServiceDto)
            serviceDefinitionInitializer.addOrUpdateServiceDefinitions(notUpdateableServiceDto)
            serviceDefinitionsSetUp = true
        }

        updatePlans()

        parameters = new HashMap<String, Object>()
        parameters.put(parameterKey, parameterOldValue)
    }

    def cleanupSpec() {
        serviceLifeCycler.cleanup()
    }

    def "plan can be updated"() {
        setup:
        def serviceInstanceGuid = requestServiceProvisioning()
        serviceLifeCycler.setServiceInstanceId(serviceInstanceGuid)

        when:
        serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, secondPlanGuid)

        then:
        def serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
        assert serviceInstance
        serviceInstance.plan.guid == secondPlanGuid

        cleanup:
        serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, secondPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }

    def "parameters can be updated"() {
        given:
        def serviceInstanceGuid = requestServiceProvisioning(parameters)
        serviceLifeCycler.setServiceInstanceId(serviceInstanceGuid)

        when:
        parameters.put(parameterKey, parameterNewValue)
        serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, null, parameters)

        then:
        def serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
        assert serviceInstance
        def modeDetail = serviceInstance.details.find { d -> d.key == parameterKey }
        assert modeDetail
        modeDetail.value == parameterNewValue

        cleanup:
        serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }

    def "update request also updates context"() {
        given:
        def serviceInstanceGuid = requestServiceProvisioning(parameters)
        serviceLifeCycler.setServiceInstanceId(serviceInstanceGuid)
        def updatedContext = CloudFoundryContext.builder().
                organizationGuid("myorg").
                spaceGuid("myspace").
                build()

        when:
        parameters.put(parameterKey, parameterNewValue)
        serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, null, parameters, false, updatedContext)

        then:
        def serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
        assert serviceInstance
        assert ServiceContextHelper.convertFrom(serviceInstance.serviceContext).equals(updatedContext)

        cleanup:
        serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }

    def "parameters can be updated with same planId"() {
        given:
        def serviceInstanceGuid = requestServiceProvisioning(parameters)
        serviceLifeCycler.setServiceInstanceId(serviceInstanceGuid)

        when:
        parameters.put(parameterKey, parameterNewValue)
        serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, parameters)

        then:
        def serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
        assert serviceInstance
        def modeDetail = serviceInstance.details.find { d -> d.key == parameterKey }
        assert modeDetail
        modeDetail.value == parameterNewValue

        cleanup:
        serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }

    def "parameters update changes parameters on serviceDefinition"() {
        setup:
        def serviceInstanceGuid = requestServiceProvisioning(parameters)
        serviceLifeCycler.setServiceInstanceId(serviceInstanceGuid)

        when:
        parameters.put(parameterKey, parameterNewValue)
        serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, null, parameters)

        then:
        def serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
        assert serviceInstance
        serviceInstance.parameters == '{"mode":"open"}'

        cleanup:
        serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }
    
    def "plan and parameters can be updated"() {
        setup:
        def parameters = new HashMap<String, Object>()
        def parameterKey = "mode"
        def oldValue = "blocking"
        def newValue = "open"
        parameters.put(parameterKey, oldValue)
        def serviceInstanceGuid = requestServiceProvisioning(parameters)
        def newPlanGuid = "updateTest_Updateable_plan_b"

        when:
        parameters.put(parameterKey, newValue)
        serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultSpaceGuid, newPlanGuid, parameters)

        then:
        def serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
        assert serviceInstance
        serviceInstance.plan.guid == newPlanGuid
        def modeDetail = serviceInstance.details.findAll { d -> d.key == parameterKey }
        modeDetail[0].value == newValue

        cleanup:
        serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultSpaceGuid, newPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }

    def "plan update blocks if plan_updatable is false"() {
        given:
        def newPlanGuid = "updateTest_notUpdateable_plan_b"
        def serviceInstanceGuid = requestServiceProvisioning(null, notUpdatablePlanGuid, false, notUpdatableServiceGuid)

        when:
        def response = serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, notUpdatableServiceGuid, newPlanGuid)

        then:
        response.statusCode == ErrorCode.PLAN_UPDATE_NOT_ALLOWED.httpStatus

        cleanup:
        serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, notUpdatableServiceGuid, notUpdatablePlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)

    }

    def "async parameter update is supported"() {
        setup:
            def serviceInstanceGuid = requestServiceProvisioning(parameters)

        when:
            parameters.put(parameterKey, parameterNewValue)
            serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, null, parameters, true)
            waitUntilMaxTimeOrTargetState(serviceInstanceGuid, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 6)

        then:
            def lastOperationResponse = serviceBrokerClient.getServiceInstanceLastOperation(serviceInstanceGuid).getBody()
            def operationState = lastOperationResponse.state
            operationState == LastOperationState.SUCCEEDED || operationState == LastOperationState.FAILED
            def serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
            assert serviceInstance
            def modeDetail = serviceInstance.details.find { d -> d.key == parameterKey }
            assert modeDetail
            modeDetail.value == parameterNewValue

        cleanup:
            serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }

    def "async update is failing if provision is still in progress"() {
        setup:
            def newPlanGuid = "updateTest_notUpdateable_plan_b"
            def serviceInstanceGuid = requestServiceProvisioning(parameters, defaultPlanGuid, true)

        when:
            parameters.put(parameterKey, parameterNewValue)
            def response = serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, newPlanGuid, parameters, true)

        then:
            response.statusCode == ErrorCode.OPERATION_IN_PROGRESS.httpStatus

        cleanup:
            serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }

    def "async update response does not contain field 'operation' if value would be empty or null"() {
        setup:
        def serviceInstanceGuid = requestServiceProvisioning()

        when:
        def response = serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, secondPlanGuid)

        then:
        response.statusCode == HttpStatus.OK
        response.body.operation == null

        cleanup:
        serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }

    def "second async parameter update is denied"() {
        setup:

            def secondNewValue = "unidirectional"
            def serviceInstanceGuid = requestServiceProvisioning(parameters)

        when:
            parameters.put(parameterKey, parameterNewValue)
            def response = serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultServiceGuid, null, parameters, true)
            parameters.put(parameterKey, secondNewValue)
            def response2 = serviceLifeCycler.requestUpdateServiceInstance(serviceInstanceGuid, defaultSpaceGuid, null, parameters, true)
            waitUntilMaxTimeOrTargetState(serviceInstanceGuid, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 6)

        then:
            response.statusCode == HttpStatus.ACCEPTED
            response2.statusCode == ErrorCode.OPERATION_IN_PROGRESS.httpStatus
            def lastOperationResponse = serviceBrokerClient.getServiceInstanceLastOperation(serviceInstanceGuid).getBody()
            def operationState = lastOperationResponse.state
            operationState == LastOperationState.SUCCEEDED || operationState == LastOperationState.FAILED
            def serviceInstance = serviceInstanceRepository.findByGuid(serviceInstanceGuid)
            assert serviceInstance
            def modeDetail = serviceInstance.details.find { d -> d.key == parameterKey }
            assert modeDetail
            modeDetail.value == parameterNewValue

        cleanup:
            serviceLifeCycler.deleteServiceInstanceAndAssert(serviceInstanceGuid, defaultServiceGuid, defaultPlanGuid, null, false, DummyServiceProvider.RETRY_INTERVAL_IN_SECONDS * 4)
    }
}
