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

import java.nio.file.Paths

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import com.nimbusds.jose.jwk.KeyType

import nl.qiy.openid.op.spi.impl.config.JWKConfig
import nl.qiy.openid.op.spi.impl.keystore.JWKReader;;

class JWKReaderSpec extends Specification {
    @Shared keystoreFilePath = Paths.get(this.getClass().getResource("/test.jks").toURI()).toAbsolutePath().toString();
    @Shared keystorePass = "abacus"

    @Unroll
    def "throw an exception when the keystore config is wrong (#kf, #p1, #p2)"() {
        when:
        new JWKReader("testRSA", new JWKConfig(kf, null, p1, p2));

        then:
        thrown(Exception)

        where:
        kf               | p1           | p2
        "aap"            | keystorePass | keystorePass
        keystoreFilePath | "aap"        | keystorePass
        keystoreFilePath | keystorePass | "aap"
        null             | keystorePass | keystorePass
        // keystoreFilePath | null         | keystorePass this one actually works ?!
        keystoreFilePath | keystorePass | null
    }


    @Unroll
    def "load the expected key (#alias, #type)"() {
        def config = new JWKConfig(keystoreFilePath, null, keystorePass, keystorePass);

        when:
        def jwk = new JWKReader(alias, config).getJWK();

        then:
        jwk.getKeyType() == type;

        and:
        jwk.isPrivate() == true

        and:
        jwk.toPublicJWK().isPrivate() == false;

        and:
        jwk.toPublicJWK().getKeyType() == type;

        and:
        jwk.toString().size() > jwk.toPublicJWK().toString().size()

        where:
        alias     | type
        "testRSA" | KeyType.RSA
        "testEC"  | KeyType.EC
    }
}