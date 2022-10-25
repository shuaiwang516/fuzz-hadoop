/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ipc;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


public class TestMultipleProtocolServer extends TestRpcBase {

  private static RPC.Server server;

  @Before
  public void setUp() throws Exception {
    super.setupConfFuzz();

    server = setupTestServer(conf, 2);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  // Now test a PB service - a server  hosts both PB and Writable Rpcs.
  @Fuzz
  public void testPBServiceFuzz(@From(ConfigurationGenerator.class) Configuration generatedConfig) throws Exception {
    // Set RPC engine to protobuf RPC engine
    Configuration conf2 = new Configuration(generatedConfig);
    RPC.setProtocolEngine(conf2, TestRpcService.class,
            ProtobufRpcEngine2.class);
    TestRpcService client = RPC.getProxy(TestRpcService.class, 0, addr, conf2);
    TestProtoBufRpc.testProtoBufRpc(client);
  }

  // Now test a PB service - a server  hosts both PB and Writable Rpcs.
  @Test
  public void testPBService() throws Exception {
    // Set RPC engine to protobuf RPC engine
    Configuration conf2 = new Configuration();
    RPC.setProtocolEngine(conf2, TestRpcService.class,
        ProtobufRpcEngine2.class);
    TestRpcService client = RPC.getProxy(TestRpcService.class, 0, addr, conf2);
    TestProtoBufRpc.testProtoBufRpc(client);
  }
}
