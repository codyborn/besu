/*
 * Copyright Besu Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.vm.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hyperledger.besu.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.hyperledger.besu.evm.frame.ExceptionalHaltReason.INSUFFICIENT_GAS;
import static org.mockito.Mockito.mock;

import org.hyperledger.besu.config.experimental.ExperimentalEIPs;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderTestFixture;
import org.hyperledger.besu.ethereum.core.MessageFrameTestFixture;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.EIP1153GasCalculator;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.operation.Operation.OperationResult;
import org.hyperledger.besu.evm.operation.TLoadOperation;
import org.hyperledger.besu.evm.operation.TStoreOperation;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import org.apache.tuweni.units.bigints.UInt256;
import org.junit.Before;
import org.junit.Test;

public class TStoreOperationTest {

  private static final GasCalculator gasCalculator = new EIP1153GasCalculator();

  @Before
  public void setUp() {
    ExperimentalEIPs.eip1153Enabled = true;
  }

  private MessageFrame createMessageFrame(
      final Address address, final long initialGas, final long remainingGas) {
    final Blockchain blockchain = mock(Blockchain.class);

    final WorldStateArchive worldStateArchive = createInMemoryWorldStateArchive();
    final WorldUpdater worldStateUpdater = worldStateArchive.getMutable().updater();
    final BlockHeader blockHeader = new BlockHeaderTestFixture().buildHeader();
    final MessageFrame frame =
        new MessageFrameTestFixture()
            .address(address)
            .worldUpdater(worldStateUpdater)
            .blockHeader(blockHeader)
            .blockchain(blockchain)
            .initialGas(initialGas)
            .build();
    worldStateUpdater.getOrCreate(address).getMutable().setBalance(Wei.of(1));
    worldStateUpdater.commit();
    frame.setGasRemaining(remainingGas);

    return frame;
  }

  @Test
  public void tstoreInsufficientGas() {
    long initialGas = 10_000L;
    long remainingGas = 99L; // TSTORE cost should be 100
    final TStoreOperation operation = new TStoreOperation(gasCalculator);
    final MessageFrame frame =
        createMessageFrame(Address.fromHexString("0x18675309"), initialGas, remainingGas);
    frame.pushStackItem(UInt256.ZERO);
    frame.pushStackItem(UInt256.fromHexString("0x01"));

    final OperationResult result = operation.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(INSUFFICIENT_GAS);
  }

  @Test
  public void tStoreSimpleTest() {
    long initialGas = 10_000L;
    long remainingGas = 10_000L;
    final TStoreOperation operation = new TStoreOperation(gasCalculator);
    final MessageFrame frame =
        createMessageFrame(Address.fromHexString("0x18675309"), initialGas, remainingGas);
    frame.pushStackItem(UInt256.ZERO);
    frame.pushStackItem(UInt256.fromHexString("0x01"));

    final OperationResult result = operation.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(null);
  }

  @Test
  public void tLoadEmpty() {
    long initialGas = 10_000L;
    long remainingGas = 10_000L;
    final MessageFrame frame =
        createMessageFrame(Address.fromHexString("0x18675309"), initialGas, remainingGas);

    final TLoadOperation tload = new TLoadOperation(gasCalculator);
    frame.pushStackItem(UInt256.fromHexString("0x01"));
    final OperationResult tloadResult = tload.execute(frame, null);
    assertThat(tloadResult.getHaltReason()).isEqualTo(null);
    UInt256 tloadValue = UInt256.fromBytes(frame.popStackItem());
    assertThat(tloadValue).isEqualTo(UInt256.ZERO);
  }

  @Test
  public void tStoreTLoad() {
    long initialGas = 10_000L;
    long remainingGas = 10_000L;
    final TStoreOperation tstore = new TStoreOperation(gasCalculator);
    final MessageFrame frame =
        createMessageFrame(Address.fromHexString("0x18675309"), initialGas, remainingGas);
    frame.pushStackItem(UInt256.ONE);
    frame.pushStackItem(UInt256.fromHexString("0x01"));

    final OperationResult result = tstore.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(null);

    TLoadOperation tload = new TLoadOperation(gasCalculator);
    frame.pushStackItem(UInt256.fromHexString("0x01"));
    OperationResult tloadResult = tload.execute(frame, null);
    assertThat(tloadResult.getHaltReason()).isEqualTo(null);
    UInt256 tloadValue = UInt256.fromBytes(frame.popStackItem());
    assertThat(tloadValue).isEqualTo(UInt256.ONE);

    // Loading from a different location returns default value
    frame.pushStackItem(UInt256.fromHexString("0x02"));
    tloadResult = tload.execute(frame, null);
    assertThat(tloadResult.getHaltReason()).isEqualTo(null);
    tloadValue = UInt256.fromBytes(frame.popStackItem());
    assertThat(tloadValue).isEqualTo(UInt256.ZERO);
  }

  @Test
  public void tStoreUpdate() {
    long initialGas = 10_000L;
    long remainingGas = 10_000L;
    final TStoreOperation tstore = new TStoreOperation(gasCalculator);
    final MessageFrame frame =
        createMessageFrame(Address.fromHexString("0x18675309"), initialGas, remainingGas);
    frame.pushStackItem(UInt256.ONE);
    frame.pushStackItem(UInt256.fromHexString("0x01"));

    OperationResult result = tstore.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(null);

    // Store 2 at position 1
    frame.pushStackItem(UInt256.fromHexString("0x02"));
    frame.pushStackItem(UInt256.fromHexString("0x01"));

    result = tstore.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(null);

    final TLoadOperation tload = new TLoadOperation(gasCalculator);
    frame.pushStackItem(UInt256.fromHexString("0x01"));
    final OperationResult tloadResult = tload.execute(frame, null);
    assertThat(tloadResult.getHaltReason()).isEqualTo(null);
    UInt256 tloadValue = UInt256.fromBytes(frame.popStackItem());
    assertThat(tloadValue).isEqualTo(UInt256.fromHexString("0x02"));
  }

  // Zeroing out a transient storage slot does not result in gas refund
  @Test
  public void noGasRefundFromTransientState() {
    long initialGas = 10_000L;
    long remainingGas = 10_000L;
    final TStoreOperation tstore = new TStoreOperation(gasCalculator);
    final MessageFrame frame =
        createMessageFrame(Address.fromHexString("0x18675309"), initialGas, remainingGas);
    frame.pushStackItem(UInt256.ONE);
    frame.pushStackItem(UInt256.fromHexString("0x01"));

    OperationResult result = tstore.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(null);

    // Reset value to 0
    frame.pushStackItem(UInt256.fromHexString("0x00"));
    frame.pushStackItem(UInt256.fromHexString("0x01"));

    result = tstore.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(null);

    assertThat(result.getGasCost()).isEqualTo(100L);
  }
}