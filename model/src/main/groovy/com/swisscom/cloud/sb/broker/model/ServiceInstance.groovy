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

package com.swisscom.cloud.sb.broker.model

import javax.persistence.*

@Entity
class ServiceInstance extends BaseModel{
    @Column(unique = true)
    String guid
    Date dateCreated = new Date()
    @Column(columnDefinition='tinyint(1) default 1')
    boolean completed
    @Column(columnDefinition='tinyint(1) default 0')
    boolean deleted
    @Column
    Date dateDeleted
    @Column
    String parameters
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="service_instance_id")
    Set<ServiceBinding> bindings = []
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "service_instance_service_detail",
            joinColumns = @JoinColumn(name = "service_instance_details_id"),
            inverseJoinColumns = @JoinColumn(name = "service_detail_id"))
    List<ServiceDetail> details = []
    @Column(name="plan_id", updatable = false, insertable = false)
    Integer planId
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="plan_id")
    Plan plan
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_service_instance_id")
    ServiceInstance parentServiceInstance
    @OneToMany(mappedBy = "parentServiceInstance", fetch = FetchType.LAZY)
    Set<ServiceInstance> childs = []
    @OneToOne(fetch = FetchType.EAGER)
    ServiceContext serviceContext
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_user_id")
    ApplicationUser applicationUser

    /**
     * Get the value of the desired ServiceDetail identified by its key
     * FIXME we should use a Map for ServiceDetail instead of a List
     *
     * @param key the identifier of the ServiceDetail
     * @return the value associated to the key
     */
    public String getDetail(String key){
        details.find{d -> d.key == key}.value
    }

    @Override
    String toString() {
        return "ServiceInstance{" +
                "id=" + id +
                ", guid='" + guid + '\'' +
                ", dateCreated=" + dateCreated +
                ", completed=" + completed +
                ", deleted=" + deleted +
                ", dateDeleted=" + dateDeleted +
                ", parameters=" + parameters +
                "}"
    }
}
