package org.eclipse.dataspaceconnector.ion.util;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.TreeMap;

public class SortingNodeFactory extends JsonNodeFactory {
    @Override
    public ObjectNode objectNode() {
        return new ObjectNode(this, new TreeMap<>());
    }
}
