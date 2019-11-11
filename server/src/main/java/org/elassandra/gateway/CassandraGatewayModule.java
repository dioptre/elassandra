/*
 * Copyright (c) 2017 Strapdata (http://www.strapdata.com)
 * Contains some code from Elasticsearch (http://www.elastic.co)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elassandra.gateway;


import org.elasticsearch.common.inject.AbstractModule;

public class CassandraGatewayModule extends AbstractModule {

    @Override
    protected void configure() {
        //bind(MetaStateService.class).asEagerSingleton();
        //bind(DanglingIndicesState.class).asEagerSingleton();
        bind(CassandraGatewayService.class).asEagerSingleton();
        //bind(TransportNodesListGatewayMetaState.class).asEagerSingleton();
        //bind(GatewayMetaState.class).asEagerSingleton();
        //bind(TransportNodesListGatewayStartedShards.class).asEagerSingleton();
        //bind(LocalAllocateDangledIndices.class).asEagerSingleton();
    }
}
