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

/**
 * Creates a DropWizard based implementation, which is mainly geared towards a demo environment. Notable 'features' are:
 * <ol>
 * <li>Secrets are stored in the dropwizard configuration file
 * <li>Login information is stored in the http session and an instance of Guava's {@link com.google.common.cache.Cache}.
 * This means that a restart of the application will delete all logins.
 * <li>It is assumed the application will not work on a cluster of services; no session replication has been provided.
 * If the application does need to run in a cluster, some form of sticky sessions can be put in front of this
 * application.
 * </ol>
 *
 * @author Friso Vrolijken
 * @since 10 mei 2016
 */
package nl.qiy.openid.op.spi.impl.demo;