/*
 *  Copyright (c) 2020-2021 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.mail.impl.sasl;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class NTLMAuth extends AuthBaseClass {

    private final NTLMEngineImpl ntlmEngine;
    private boolean firstStep;
    private boolean finished;
    private final String domain;
    private final String workstation;

    NTLMAuth(String username, String password, String domain, String workstation) {
        super("NTLM", username, password);
        this.ntlmEngine = new NTLMEngineImpl();
        firstStep = true;
        finished = false;
        this.domain = domain;
        this.workstation = workstation;
    }

    @Override
    public String nextStep(String data) {
        if (finished) {
            return null;
        }
        try {
            if (firstStep) {
                firstStep = false;
                return this.ntlmEngine.generateType1Msg(this.domain, this.workstation);
            } else {
                finished = true;
                return this.ntlmEngine.generateType3Msg(username, password.toCharArray(), this.domain, this.workstation, data);
            }
        } catch (NTLMEngineException e) {
            throw new RuntimeException("Failed to generate NTLM response message", e);
        }
    }

    @Override
    public boolean handleCoding() {
        return true;
    }

    String getDomain() {
        return this.domain;
    }

    String getWorkstation() {
        return workstation;
    }
}