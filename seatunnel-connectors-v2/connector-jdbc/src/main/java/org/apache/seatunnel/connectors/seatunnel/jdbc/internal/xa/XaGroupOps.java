/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.seatunnel.jdbc.internal.xa;

import org.apache.seatunnel.api.common.SeaTunnelContext;
import org.apache.seatunnel.api.sink.SinkWriter;
import org.apache.seatunnel.connectors.seatunnel.jdbc.state.XidInfo;

import javax.transaction.xa.Xid;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface XaGroupOps
        extends Serializable {

    // Commit a batch of transactions
    public GroupXaOperationResult<XidInfo> commit(
            List<XidInfo> xids, boolean allowOutOfOrderCommits, int maxCommitAttempts);

    void rollback(List<XidInfo> xids);

    GroupXaOperationResult<XidInfo> failAndRollback(Collection<XidInfo> xids);

    void recoverAndRollback(SeaTunnelContext context, SinkWriter.Context sinkContext, XidGenerator xidGenerator, Xid excludeXid);

}
