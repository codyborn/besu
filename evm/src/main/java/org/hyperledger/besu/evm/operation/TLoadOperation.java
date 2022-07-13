/*
 * Copyright ConsenSys AG.
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
package org.hyperledger.besu.evm.operation;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.internal.FixedStack.OverflowException;
import org.hyperledger.besu.evm.internal.FixedStack.UnderflowException;

import java.util.Optional;
import java.util.OptionalLong;

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class TLoadOperation extends AbstractOperation {

  public TLoadOperation(final GasCalculator gasCalculator) {
    super(0xb3, "TLOAD", 1, 1, 1, gasCalculator);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final long cost = gasCalculator().getTloadOperationGasCost();
    try {
      final Account account = frame.getWorldUpdater().get(frame.getRecipientAddress());
      final Bytes32 key = UInt256.fromBytes(frame.popStackItem());
      if (frame.getRemainingGas() < cost) {
        return new OperationResult(
          OptionalLong.of(cost), Optional.of(ExceptionalHaltReason.INSUFFICIENT_GAS));
      } else {
        frame.pushStackItem(account.getTransientStorageValue(UInt256.fromBytes(key)));

        return new OperationResult(OptionalLong.of(cost), Optional.empty());
      }
    } catch (final UnderflowException ufe) {
      return new OperationResult(
        OptionalLong.of(cost), Optional.of(ExceptionalHaltReason.INSUFFICIENT_STACK_ITEMS));
    } catch (final OverflowException ofe) {
      return new OperationResult(OptionalLong.of(cost), Optional.of(ExceptionalHaltReason.TOO_MANY_STACK_ITEMS));
    }
  }
}