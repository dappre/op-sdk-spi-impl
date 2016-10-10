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

package nl.qiy.openid.op.spi.impl.demo;

import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.ECKey.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * Hat tip to https://github.com/JamesSullivan1/JWK_Extractor/blob/master/src/jwk_extractor/JWK_Handler.java
 * 
 * Tries to read a JWKSet from a Java Keystore (.jks)
 * <p>
 * see: <a href="https://tools.ietf.org/html/rfc7518#appendix-A">RFC 7518</a> for translation of algorithms (repeated
 * here).
 * 
 * <pre>
 * A.1.  Digital Signature/MAC Algorithm Identifier Cross-Reference
 * 
 *    This section contains a table cross-referencing the JWS digital
 *    signature and MAC "alg" (algorithm) values defined in this
 *    specification with the equivalent identifiers used by other standards
 *    and software packages.
 * 
 *    +--------------------------------------------------------+
 *    | JWS   | JCA                  | OID                     |
 *    +--------------------------------------------------------+
 *    | HS256 | HmacSHA256           | 1.2.840.113549.2.9      |
 *    | HS384 | HmacSHA384           | 1.2.840.113549.2.10     |
 *    | HS512 | HmacSHA512           | 1.2.840.113549.2.11     |
 *    | RS256 | SHA256withRSA        | 1.2.840.113549.1.1.11   |
 *    | RS384 | SHA384withRSA        | 1.2.840.113549.1.1.12   |
 *    | RS512 | SHA512withRSA        | 1.2.840.113549.1.1.13   |
 *    | ES256 | SHA256withECDSA      | 1.2.840.10045.4.3.2     |
 *    | ES384 | SHA384withECDSA      | 1.2.840.10045.4.3.3     |
 *    | ES512 | SHA512withECDSA      | 1.2.840.10045.4.3.4     |
 *    | PS256 | SHA256withRSAandMGF1 | 1.2.840.113549.1.1.10   |
 *    | PS384 | SHA384withRSAandMGF1 | 1.2.840.113549.1.1.10   |
 *    | PS512 | SHA512withRSAandMGF1 | 1.2.840.113549.1.1.10   |
 *    +--------------------------------------------------------+
 * 
 * A.2.  Key Management Algorithm Identifier Cross-Reference
 * 
 *    This section contains a table cross-referencing the JWE "alg"
 *    (algorithm) values defined in this specification with the equivalent
 *    identifiers used by other standards and software packages.
 * 
 *    +--------------------------------------------------------------------------------+
 *    | JWE          | JCA                                   | OID                     |
 *    +--------------------------------------------------------------------------------+
 *    | RSA1_5       | RSA/ECB/PKCS1Padding                  | 1.2.840.113549.1.1.1    |
 *    | RSA-OAEP     | RSA/ECB/OAEPWithSHA-1AndMGF1Padding   | 1.2.840.113549.1.1.7    |
 *    | RSA-OAEP-256 | RSA/ECB/OAEPWithSHA-256AndMGF1Padding | 1.2.840.113549.1.1.7    |
 *    |              | and MGF1ParameterSpec.SHA256          |                         |
 *    | ECDH-ES      | ECDH                                  | 1.3.132.1.12            |
 *    | A128KW       | AESWrap                               | 2.16.840.1.101.3.4.1.5  |
 *    | A192KW       | AESWrap                               | 2.16.840.1.101.3.4.1.25 |
 *    | A256KW       | AESWrap                               | 2.16.840.1.101.3.4.1.45 |
 *    +--------------------------------------------------------------------------------+
 * 
 * A.3.  Content Encryption Algorithm Identifier Cross-Reference
 * 
 *    This section contains a table cross-referencing the JWE "enc"
 *    (encryption algorithm) values defined in this specification with the
 *    equivalent identifiers used by other standards and software packages.
 * 
 *    For the composite algorithms "A128CBC-HS256", "A192CBC-HS384", and
 *    "A256CBC-HS512", the corresponding AES-CBC algorithm identifiers are
 *    listed.
 * 
 *    +----------------------------------------------------------------+
 *    | JWE           | JCA                  | OID                     |
 *    +----------------------------------------------------------------+
 *    | A128CBC-HS256 | AES/CBC/PKCS5Padding | 2.16.840.1.101.3.4.1.2  |
 *    | A192CBC-HS384 | AES/CBC/PKCS5Padding | 2.16.840.1.101.3.4.1.22 |
 *    | A256CBC-HS512 | AES/CBC/PKCS5Padding | 2.16.840.1.101.3.4.1.42 |
 *    | A128GCM       | AES/GCM/NoPadding    | 2.16.840.1.101.3.4.1.6  |
 *    | A192GCM       | AES/GCM/NoPadding    | 2.16.840.1.101.3.4.1.26 |
 *    | A256GCM       | AES/GCM/NoPadding    | 2.16.840.1.101.3.4.1.46 |
 *    +----------------------------------------------------------------+
 * 
 * </pre>
 *
 * @author Friso Vrolijken
 * @since 30 mei 2016
 */
public class JWKReader {
    private static final Map<String, String> OID_MAP = new HashMap<>();

    private final String alias;
    private final Entry entry;

    static {
        OID_MAP.put("1.2.840.113549.2.9", "HS256");
        OID_MAP.put("1.2.840.113549.2.10", "HS384");
        OID_MAP.put("1.2.840.113549.2.11", "HS512");
        OID_MAP.put("1.2.840.113549.1.1.11", "RS256");
        OID_MAP.put("1.2.840.113549.1.1.12", "RS384");
        OID_MAP.put("1.2.840.113549.1.1.13", "RS512");
        OID_MAP.put("1.2.840.10045.4.3.2", "ES256");
        OID_MAP.put("1.2.840.10045.4.3.3", "ES384");
        OID_MAP.put("1.2.840.10045.4.3.4", "ES512");
        OID_MAP.put("1.2.840.113549.1.1.10", "PS256"); // NOSONAR
        OID_MAP.put("1.2.840.113549.1.1.10", "PS384"); // NOSONAR
        OID_MAP.put("1.2.840.113549.1.1.10", "PS512"); // NOSONAR
        OID_MAP.put("1.2.840.113549.1.1.1", "RSA1_5");
        OID_MAP.put("1.2.840.113549.1.1.7", "RSA-OAEP");
        OID_MAP.put("1.2.840.113549.1.1.7", "RSA-OAEP-256");
        OID_MAP.put("1.3.132.1.12", "ECDH-ES");
        OID_MAP.put("2.16.840.1.101.3.4.1.5", "A128KW");
        OID_MAP.put("2.16.840.1.101.3.4.1.25", "A192KW");
        OID_MAP.put("2.16.840.1.101.3.4.1.45", "A256KW");
        OID_MAP.put("2.16.840.1.101.3.4.1.2", "A128CBC-HS256");
        OID_MAP.put("2.16.840.1.101.3.4.1.22", "A192CBC-HS384");
        OID_MAP.put("2.16.840.1.101.3.4.1.42", "A256CBC-HS512");
        OID_MAP.put("2.16.840.1.101.3.4.1.6", "A128GCM");
        OID_MAP.put("2.16.840.1.101.3.4.1.26", "A192GCM");
        OID_MAP.put("2.16.840.1.101.3.4.1.46", "A256GCM");

    }

    public JWKReader(String alias, JWKConfig config) {
        super();
        this.alias = alias;
        this.entry = SecretStoreImpl.loadKey(alias, config.keystoreFilename, config.keystoreType,
                config.keystorePassPhrase, config.keyPassPhrase);
    }

    public JWK getJWK() {
        if (entry instanceof TrustedCertificateEntry) {
            throw new UnsupportedOperationException("trusted certificate is not yet implemented");
        } else if (entry instanceof SecretKeyEntry) {
            throw new UnsupportedOperationException("secret key is not yet implemented");
        } else if (entry instanceof PrivateKeyEntry) {
            return getPrivateKeyJWK();
        } else {
            throw new IllegalStateException("Unknow entry type: " + entry + " for key " + alias);
        }
    }

    private JWK getPrivateKeyJWK() {
        PrivateKeyEntry pke = (PrivateKeyEntry) entry;
        X509Certificate certificate = (X509Certificate) pke.getCertificate();
        PrivateKey privateKey = pke.getPrivateKey();
        KeyUse ku = getKeyUsage(certificate);
        if (privateKey instanceof RSAPrivateKey) {
            // @formatter:off
            RSAKey.Builder builder = new RSAKey
                    .Builder((RSAPublicKey) certificate.getPublicKey())
                    .privateKey((RSAPrivateKey) privateKey)
                    .keyUse(ku)
                    .keyID(alias);
            // @formatter:on
            // TODO check if this makes any sense (the alg with which the cert was signed vs the alg that can be used
            // with the PK?
            if (ku == null || ku == KeyUse.SIGNATURE) {
                String alg = OID_MAP.get(certificate.getSigAlgOID());
                builder.algorithm(JWSAlgorithm.parse(alg));
            }
            return builder.build();
        } else if (privateKey instanceof ECPrivateKey) {
            ECPrivateKey ecPK = (ECPrivateKey) privateKey;
            Curve curve = Curve.forECParameterSpec(ecPK.getParams());
            // @formatter:off
            ECKey.Builder builder = new ECKey
                    .Builder(curve, (ECPublicKey) certificate.getPublicKey())
                    .privateKey(ecPK)
                    .keyUse(ku)
                    .keyID(alias);
            // @formatter:on
            // TODO if it didn't make sense on the previous if, it doesn't here
            if (ku == null || ku == KeyUse.SIGNATURE) {
                String alg = OID_MAP.get(certificate.getSigAlgOID());
                builder.algorithm(JWSAlgorithm.parse(alg));
            }
            return builder.build();
        } else {
            throw new UnsupportedOperationException("Currently RSAKey and ECKey are supported");
        }
    }

    /**
     * Returns null if both sign and enc are on or the certificate doesn't specify the key usage,
     * {@link KeyUse#SIGNATURE} if the 0th bit of the certificate's key use is on but the 7th not,
     * {@link KeyUse#ENCRYPTION} if the 7th bit of the certificate's key usage is on but the 0th isn't.
     * 
     * @param certificate
     *            describes the key use
     * @return see description
     */
    private static KeyUse getKeyUsage(X509Certificate certificate) {
        KeyUse ku = null;
        // leave null if undefined or both sig and enc
        boolean[] keyUsage = certificate.getKeyUsage();
        if (keyUsage != null && keyUsage.length > 7) {
            if (keyUsage[0] && !keyUsage[7]) {
                ku = KeyUse.SIGNATURE;
            } else if (!keyUsage[0] && keyUsage[7]) {
                ku = KeyUse.ENCRYPTION;
            }
        }
        return ku;
    }
}
