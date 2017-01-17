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

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.service.SecretService;
import nl.qiy.openid.op.spi.impl.config.OpSdkSpiImplConfiguration;

/**
 * Handling encryption/decryption for messages sent to a node. This should be factored out ASAP (i.e. when consent has
 * properly been implemented)
 *
 * @author Friso Vrolijken
 * @since 24 jun. 2016
 */
class MessageCrypto {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCrypto.class);

    private MessageCrypto() {
        throw new UnsupportedOperationException("Utility class should only be used statically");
    }

    // TODO [FV 20160611] part of the exploratory hack of enroll new user
    static byte[] encryptSymmetric(byte[] cleartext, byte[] secretBytes, byte[] iv) {
        try {
            SecretKey secret = new SecretKeySpec(secretBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));
            return cipher.doFinal(cleartext);
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Error while doing encryptSymmetric", e);
            throw new IllegalStateException(e);
        }
    }

    // TODO [FV 20160611] part of the exploratory hack of enroll new user
    static byte[] decryptSymmetric(byte[] encrypted, byte[] secretBytes, byte[] iv) {
        try {
            SecretKey secret = new SecretKeySpec(secretBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Error while doing decryptSymmetric", e);
            throw new IllegalStateException(e);
        }
    }

    // TODO [FV 20160611] part of the exploratory hack of enroll new user
    static byte[] encryptAsymmetricNoPadding(PublicKey publicKey, byte[] cleartext) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(cleartext);
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Error while doing encryptAsymmetric", e);
            throw new IllegalStateException(e);
        }
    }

    static byte[] encryptAsymmetricWithPadding(PublicKey publicKey, byte[] cleartext) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPwithSHA-1andMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(cleartext);
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Error while doing encryptAsymmetric", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * <p>
     * Decrypts the provided base64 encoded asymmetric encrypted data {@code crypted} with the {@link PrivateKey} of the
     * {@link SecretService} .
     * </p>
     * <p>
     * First tries to decrypt using the {@code RSA/ECB/OAEPwithSHA-1andMGF1Padding} algorithm which includes padding.
     * Should this fail and throw a {@link BadPaddingException}, a decrypt with the {@code RSA/ECB/NoPadding} algorithm
     * is performed. In that case, the decrypted result will be prepended with zero bytes until the result reaches a
     * size of {@code expectedSize} bytes since they might have been removed in the steps after the decrypt.
     * </p>
     * <p>
     * In case of a successful decrypt with the {@code RSA/ECB/OAEPwithSHA-1andMGF1Padding} algorithm, the
     * {@code expectedSize} parameter is ignored.
     * </p>
     * 
     * @param b64crypted
     *            base64 encoded asymmetric encrypted data to decrypt.
     * @param expectedSize
     *            the size that the decrypted result should be. Will be used when decrypting using the
     *            {@code RSA/ECB/NoPadding} algorithm to prepend the possibly 'lost' zero bytes.
     * @return the decrypted bytes.
     */
    static byte[] decryptAsymmetric(final String b64crypted, final int expectedSize) {
        return decryptAsymmetric(Base64.getDecoder().decode(b64crypted), expectedSize);
    }

    /**
     * <p>
     * Decrypts the provided asymmetric encrypted data {@code crypted} with the {@link PrivateKey} of the
     * {@link SecretService} .
     * </p>
     * <p>
     * First tries to decrypt using the {@code RSA/ECB/OAEPwithSHA-1andMGF1Padding} algorithm which includes padding.
     * Should this fail and throw a {@link BadPaddingException}, a decrypt with the {@code RSA/ECB/NoPadding} algorithm
     * is performed. In that case, the decrypted result will be prepended with zero bytes until the result reaches a
     * size of {@code expectedSize} bytes since they might have been removed in the steps after the decrypt.
     * </p>
     * <p>
     * In case of a successful decrypt with the {@code RSA/ECB/OAEPwithSHA-1andMGF1Padding} algorithm, the
     * {@code expectedSize} parameter is ignored.
     * </p>
     * 
     * @param crypted
     *            asymmetric encrypted data to decrypt.
     * @param expectedSize
     *            the size that the decrypted result should be. Will be used when decrypting using the
     *            {@code RSA/ECB/NoPadding} algorithm to prepend the possibly 'lost' zero bytes.
     * @return the decrypted bytes.
     */
    static byte[] decryptAsymmetric(final byte[] crypted, final int expectedSize) {
        try {
            // Since we are using padding, we no longer have to remove the zero bytes since that is handled by the
            // padding. This also makes sure that no leading zero bytes are accidently removed.
            return decryptAsymmetricWithPadding(crypted);
        } catch (BadPaddingException e) {
            LOGGER.trace("Trying to recover from bad padding", e);
            // BadPaddingException thus probably encrypted without padding.
            byte[] decryptedBytes = decryptAsymmetricNoPadding(crypted);
            // Note that zero bytes at the start of the byte array that should be part of the result might have been
            // accidently removed. Add them again if so.
            return prependZeroBytesTillSize(decryptedBytes, expectedSize);
        }
    }

    static byte[] decryptAsymmetricNoPadding(byte[] crypted) {
        try {
            OpSdkSpiImplConfiguration config = OpSdkSpiImplConfiguration.getInstance();
            PrivateKey privateKey = config.nodeConfig.privateKey;
            Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedSecret = cipher.doFinal(crypted);

            // skip null bytes at the start
            int index = 0;
            while (index < decryptedSecret.length && decryptedSecret[index] == 0) {
                index++;
            }
            int length = decryptedSecret.length - index;
            byte[] output = new byte[length];
            System.arraycopy(decryptedSecret, index, output, 0, length);
            return output;
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Error while doing decryptAsymmetric", e);
            throw new IllegalStateException(e);
        }
    }

    static byte[] decryptAsymmetricWithPadding(byte[] crypted) throws BadPaddingException {
        try {
            OpSdkSpiImplConfiguration config = OpSdkSpiImplConfiguration.getInstance();
            PrivateKey privateKey = config.nodeConfig.privateKey;
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPwithSHA-1andMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(crypted);
        } catch (BadPaddingException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Error while doing decryptAsymmetric", e);
            throw new IllegalStateException(e);
        }
    }

    static byte[] prependZeroBytesTillSize(byte[] input, int size) {
        if (input.length >= size) {
            return input;
        }
        // else
        int bytesToAdd = size - input.length;
        byte[] output = new byte[input.length + bytesToAdd];
        System.arraycopy(input, 0, output, bytesToAdd, input.length);
        return output;
    }

}
