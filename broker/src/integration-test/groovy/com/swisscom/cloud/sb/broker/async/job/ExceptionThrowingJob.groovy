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

package com.swisscom.cloud.sb.broker.async.job

import com.swisscom.cloud.sb.broker.error.ServiceBrokerException
import com.swisscom.cloud.sb.broker.provisioning.async.AsyncOperationResult
import com.swisscom.cloud.sb.broker.provisioning.lastoperation.LastOperationJobContext

import java.util.concurrent.atomic.AtomicInteger

public class ExceptionThrowingJob extends AbstractLastOperationJob {
    public static final AtomicInteger ExecutionCount = new AtomicInteger()

    @Override
    public AsyncOperationResult handleJob(LastOperationJobContext context) {
        ExecutionCount.incrementAndGet()
        throw new ServiceBrokerException('Something terrible happened')
    }
}