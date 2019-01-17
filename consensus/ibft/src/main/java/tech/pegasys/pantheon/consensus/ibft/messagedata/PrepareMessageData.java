/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.consensus.ibft.messagedata;

import tech.pegasys.pantheon.consensus.ibft.payload.PreparePayload;
import tech.pegasys.pantheon.consensus.ibft.payload.SignedData;
import tech.pegasys.pantheon.ethereum.p2p.api.MessageData;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public class PrepareMessageData extends AbstractIbftMessageData {

  private static final int MESSAGE_CODE = IbftV2.PREPARE;

  private PrepareMessageData(final BytesValue data) {
    super(data);
  }

  public static PrepareMessageData fromMessageData(final MessageData messageData) {
    return fromMessageData(
        messageData, MESSAGE_CODE, PrepareMessageData.class, PrepareMessageData::new);
  }

  public SignedData<PreparePayload> decode() {
    return SignedData.readSignedPreparePayloadFrom(RLP.input(data));
  }

  public static PrepareMessageData create(final SignedData<PreparePayload> signedPayload) {

    return new PrepareMessageData(signedPayload.encode());
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }
}
