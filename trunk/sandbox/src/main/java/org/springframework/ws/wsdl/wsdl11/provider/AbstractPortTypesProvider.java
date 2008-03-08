/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.wsdl.wsdl11.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link PortTypesProvider} implementations.
 *
 * @author Arjen Poutsma
 * @since 1.5.0
 */
public abstract class AbstractPortTypesProvider implements PortTypesProvider {

    private String portTypeName;

    /** Returns the port type name used for this definition. */
    public String getPortTypeName() {
        return portTypeName;
    }

    /** Sets the port type name used for this definition. Required. */
    public void setPortTypeName(String portTypeName) {
        this.portTypeName = portTypeName;
    }

    /**
     * Creates a single {@link PortType}, and calls {@link #populatePortType(Definition, PortType)} with it.
     *
     * @param definition the WSDL4J <code>Definition</code>
     * @throws WSDLException in case of errors
     */
    public void addPortTypes(Definition definition) throws WSDLException {
        Assert.notNull(getPortTypeName(), "'portTypeName' is required");
        PortType portType = definition.createPortType();
        populatePortType(definition, portType);
        createOperations(definition, portType);
        portType.setUndefined(false);
        definition.addPortType(portType);
    }

    /**
     * Called after the {@link PortType} has been created.
     * <p/>
     * Default implementation sets the name of the port type to the defined value.
     *
     * @param portType the WSDL4J <code>PortType</code>
     * @throws WSDLException in case of errors
     * @see #setPortTypeName(String)
     */
    protected void populatePortType(Definition definition, PortType portType) throws WSDLException {
        portType.setQName(new QName(definition.getTargetNamespace(), getPortTypeName()));
    }

    private void createOperations(Definition definition, PortType portType) throws WSDLException {
        Map operations = new HashMap();
        for (Iterator iterator = definition.getMessages().values().iterator(); iterator.hasNext();) {
            Message message = (Message) iterator.next();
            String operationName = getOperationName(message);
            if (StringUtils.hasText(operationName)) {
                List messages = (List) operations.get(operationName);
                if (messages == null) {
                    messages = new ArrayList();
                    operations.put(operationName, messages);
                }
                messages.add(message);
            }
        }
        for (Iterator iterator = operations.keySet().iterator(); iterator.hasNext();) {
            String operationName = (String) iterator.next();
            Operation operation = definition.createOperation();
            operation.setName(operationName);
            List messages = (List) operations.get(operationName);
            for (Iterator messagesIterator = messages.iterator(); messagesIterator.hasNext();) {
                Message message = (Message) messagesIterator.next();
                if (isInputMessage(message)) {
                    Input input = definition.createInput();
                    input.setMessage(message);
                    populateInput(definition, input);
                    operation.setInput(input);
                }
                else if (isOutputMessage(message)) {
                    Output output = definition.createOutput();
                    output.setMessage(message);
                    populateOutput(definition, output);
                    operation.setOutput(output);
                }
                else if (isFaultMessage(message)) {
                    Fault fault = definition.createFault();
                    fault.setMessage(message);
                    populateFault(definition, fault);
                    operation.addFault(fault);
                }
            }
            operation.setStyle(getOperationType(operation));
            operation.setUndefined(false);
            portType.addOperation(operation);
        }
    }

    /**
     * Template method that returns the name of the operation coupled to the given {@link Message}. Subclasses can
     * return <code>null</code> to indicate that a message should not be coupled to an operation.
     *
     * @param message the WSDL4J <code>Message</code>
     * @return the operation name; or <code>null</code>
     */
    protected abstract String getOperationName(Message message);

    /**
     * Indicates whether the given name name should be included as {@link Input} message in the definition.
     *
     * @param message the message
     * @return <code>true</code> if to be included as input; <code>false</code> otherwise
     */
    protected abstract boolean isInputMessage(Message message);

    /**
     * Called after the {@link javax.wsdl.Input} has been created, but it's added to the operation. Subclasses can
     * override this method to define the input name.
     * <p/>
     * Default implementation sets the input name to the message name.
     *
     * @param definition the WSDL4J <code>Definition</code>
     * @param input      the WSDL4J <code>Input</code>
     */
    protected void populateInput(Definition definition, Input input) {
        input.setName(input.getMessage().getQName().getLocalPart());
    }

    /**
     * Indicates whether the given name name should be included as {@link Output} message in the definition.
     *
     * @param message the message
     * @return <code>true</code> if to be included as output; <code>false</code> otherwise
     */
    protected abstract boolean isOutputMessage(Message message);

    /**
     * Called after the {@link javax.wsdl.Output} has been created, but it's added to the operation. Subclasses can
     * override this method to define the output name.
     * <p/>
     * Default implementation sets the output name to the message name.
     *
     * @param definition the WSDL4J <code>Definition</code>
     * @param output     the WSDL4J <code>Output</code>
     */
    protected void populateOutput(Definition definition, Output output) {
        output.setName(output.getMessage().getQName().getLocalPart());
    }

    /**
     * Indicates whether the given name name should be included as {@link Fault} message in the definition.
     *
     * @param message the message
     * @return <code>true</code> if to be included as fault; <code>false</code> otherwise
     */
    protected abstract boolean isFaultMessage(Message message);

    /**
     * Called after the {@link javax.wsdl.Fault} has been created, but it's added to the operation. Subclasses can
     * override this method to define the fault name.
     * <p/>
     * Default implementation sets the fault name to the message name.
     *
     * @param definition the WSDL4J <code>Definition</code>
     * @param fault      the WSDL4J <code>Fault</code>
     */
    protected void populateFault(Definition definition, Fault fault) {
        fault.setName(fault.getMessage().getQName().getLocalPart());
    }

    /**
     * Returns the {@link OperationType} for the given operation.
     * <p/>
     * Default implementation returns {@link OperationType#REQUEST_RESPONSE} if both input and output are set; {@link
     * OperationType#ONE_WAY} if only input is set, or {@link OperationType#NOTIFICATION} if only output is set.
     *
     * @param operation the WSDL4J <code>Operation</code>
     * @return the operation type for the operation
     */
    protected OperationType getOperationType(Operation operation) {
        if (operation.getInput() != null && operation.getOutput() != null) {
            return OperationType.REQUEST_RESPONSE;
        }
        else if (operation.getInput() != null && operation.getOutput() == null) {
            return OperationType.ONE_WAY;
        }
        else if (operation.getInput() == null && operation.getOutput() != null) {
            return OperationType.NOTIFICATION;
        }
        else {
            return null;
        }
    }


}



