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

package com.swisscom.cloud.sb.broker.services.openwhisk

import com.swisscom.cloud.sb.broker.util.servicedetail.AbstractServiceDetailKey
import com.swisscom.cloud.sb.broker.util.servicedetail.ServiceDetailType
import groovy.transform.CompileStatic

@CompileStatic
enum OpenWhiskServiceDetailKey implements AbstractServiceDetailKey{

    OPENWHISK_EXECUTION_URL("openwhisk_execution_url", ServiceDetailType.HOST),
    OPENWHISK_ADMIN_URL("openwhisk_admin_url", ServiceDetailType.HOST),
    OPENWHISK_UUID("openwhisk_uuid", ServiceDetailType.USERNAME),
    OPENWHISK_KEY("openwhisk_key", ServiceDetailType.PASSWORD),
    OPENWHISK_NAMESPACE("openwhisk_namespace", ServiceDetailType.OTHER),
    OPENWHISK_SUBJECT("openwhisk_subject", ServiceDetailType.OTHER)

    OpenWhiskServiceDetailKey(String key, ServiceDetailType serviceDetailType) {
        com_swisscom_cloud_sb_broker_util_servicedetail_AbstractServiceDetailKey__key = key
        com_swisscom_cloud_sb_broker_util_servicedetail_AbstractServiceDetailKey__serviceDetailType = serviceDetailType
    }
}
