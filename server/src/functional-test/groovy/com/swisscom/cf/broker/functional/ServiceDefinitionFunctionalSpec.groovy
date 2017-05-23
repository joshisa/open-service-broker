package com.swisscom.cf.broker.functional

import com.swisscom.cf.broker.util.Resource

class ServiceDefinitionFunctionalSpec extends BaseFunctionalSpec {
    private String serviceName = 'functionalTestServiceforServiceDefiniton'

    def setup() {
        serviceLifeCycler.createServiceIfDoesNotExist(serviceName, serviceName)
    }

    def "service definition gets created/updated"() {
        given:
        String serviceId = serviceLifeCycler.cfService.guid
        when:
        def response = serviceBrokerClient.createOrUpdateServiceDefinition(Resource.readTestFileContent("/service-data/service1.json"))
        then:
        response.statusCodeValue == 200
    }
}