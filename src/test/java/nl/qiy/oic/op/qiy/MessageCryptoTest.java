/*
 * This work is protected under copyright law in the Kingdom of
 * The Netherlands. The rules of the Berne Convention for the
 * Protection of Literary and Artistic Works apply.
 * Digital Me B.V. is the copyright owner.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.qiy.oic.op.qiy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.openid.op.spi.impl.config.OpSdkSpiImplConfiguration;


public class MessageCryptoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCryptoTest.class);
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    @SuppressWarnings("static-method")
    @Test(expected = IllegalStateException.class)
    public void checkFailedDecryptOfMessage() throws Exception {
        String encryptedBase64 = "08igFx5Q1e7uheo3nw0REg==";
        String ivBase64 = "3XS/GknAR7Jhu/uc7HlH8A==";
        // This is in face a base64 encoded key of 31 instead of 32 bytes since it has a prepended zero byte removed
        String aes256Base64 = "8CLeG/0tZcASq3FAncnmNhTqDIGTShv85bNceGqK8g==";

        byte[] iv = Base64.getDecoder().decode(ivBase64);
        byte[] aes256 = Base64.getDecoder().decode(aes256Base64);
        byte[] encodedMessage = Base64.getDecoder().decode(encryptedBase64);

        // in fact throws an InvalidKeyException but will be wrapped to a an IllegalStateException
        LOGGER.warn("Expecting the following java.security.InvalidKeyException!");
        MessageCrypto.decryptSymmetric(encodedMessage, aes256, iv);
    }

    @SuppressWarnings("static-method")
    @Test
    public void whenZeroByteAtStart_thenRemovedInDecrypt() throws Exception {
        String base64EncodedBytesWithLeadingZeroByte = "AJJ+Ru97lzWxogqtn71jNYWx8jJtWowyaifnEBIlrkg=";
        byte[] originalBytes = Base64.getDecoder().decode(base64EncodedBytesWithLeadingZeroByte);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        QiyNodeConfig nc = QiyNodeConfig.unitTestInstance(kp.getPrivate());
        OpSdkSpiImplConfiguration config = new OpSdkSpiImplConfiguration(null, null, null, nc, null, null, null, null,
                null, null, null, null, null);
        OpSdkSpiImplConfiguration.setInstance(config);

        byte[] encryptedSecret = MessageCrypto.encryptAsymmetricNoPadding(kp.getPublic(), originalBytes);
        byte[] decryptedSecret = MessageCrypto.decryptAsymmetricNoPadding(encryptedSecret);

        assertTrue(originalBytes.length == 32);
        assertTrue(decryptedSecret.length == 31);
        assertFalse(Arrays.equals(originalBytes, decryptedSecret));

        // now prepend with a zero bytes
        decryptedSecret = MessageCrypto.prependZeroBytesTillSize(decryptedSecret, 32);
        assertTrue(decryptedSecret.length == 32);
        assertTrue(Arrays.equals(originalBytes, decryptedSecret));

        // test the whole decrypt in one action.
        decryptedSecret = MessageCrypto.decryptAsymmetric(encryptedSecret, 32);
        assertTrue(decryptedSecret.length == 32);
        assertTrue(Arrays.equals(originalBytes, decryptedSecret));
    }

    @SuppressWarnings("static-method")
    @Test
    public void checkBackwardsCompatible() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        SecureRandom sr = new SecureRandom();
        byte[] aesKey = new byte[32];
        byte[] iv = new byte[16];

        QiyNodeConfig nc = QiyNodeConfig.unitTestInstance(kp.getPrivate());
        OpSdkSpiImplConfiguration config = new OpSdkSpiImplConfiguration(null, null, null, nc, null, null, null, null,
                null,
                null, null, null, null);
        OpSdkSpiImplConfiguration.setInstance(config);

        // Perform some crypt and decrypt situation on all kinds of random 'secrets', which should include zero byte
        // prepended secrets.
        // Note that we generate 2 random secrets per iteration, and encrypt/decrypt both with both algorithms thus 4
        // encrypt and 4 decrypt actions per run. (Technically 6 decrypt actions per run).
        for (int i = 0; i < 250; i++) {
            sr.nextBytes(aesKey);
            sr.nextBytes(iv);

            // no padding
            byte[] cryptedNoPaddingAESKey = MessageCrypto.encryptAsymmetricNoPadding(kp.getPublic(), aesKey);
            byte[] cryptedNoPaddingIV = MessageCrypto.encryptAsymmetricNoPadding(kp.getPublic(), iv);
            // with padding
            byte[] cryptedWithPaddingAESKey = MessageCrypto.encryptAsymmetricWithPadding(kp.getPublic(), aesKey);
            byte[] cryptedWithPaddingIV = MessageCrypto.encryptAsymmetricWithPadding(kp.getPublic(), iv);

            // no padding
            assertTrue(Arrays.equals(aesKey, MessageCrypto.decryptAsymmetric(cryptedNoPaddingAESKey, 32)));
            assertTrue(Arrays.equals(iv, MessageCrypto.decryptAsymmetric(cryptedNoPaddingIV, 16)));
            // with padding
            assertTrue(Arrays.equals(aesKey, MessageCrypto.decryptAsymmetric(cryptedWithPaddingAESKey, 32)));
            assertTrue(Arrays.equals(iv, MessageCrypto.decryptAsymmetric(cryptedWithPaddingIV, 16)));
        }
    }


    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
