package com.koerber.inventory.factory;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory class for obtaining the appropriate InventoryHandler implementation.
 * New handler strategies can be added by implementing the InventoryHandler interface
 * and annotating with @Component - they will be automatically registered here.
 */
@Component
public class InventoryHandlerFactory {

    private final Map<String, InventoryHandler> handlers;

    public InventoryHandlerFactory(List<InventoryHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(InventoryHandler::getHandlerType, Function.identity()));
    }

    /**
     * Returns a handler by its type key.
     *
     * @param type the handler type (e.g., "DEFAULT")
     * @return the matching InventoryHandler
     * @throws IllegalArgumentException if no handler is found for the given type
     */
    public InventoryHandler getHandler(String type) {
        InventoryHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No inventory handler found for type: " + type);
        }
        return handler;
    }

    /**
     * Returns the default inventory handler.
     *
     * @return the DefaultInventoryHandler
     */
    public InventoryHandler getDefaultHandler() {
        return getHandler(DefaultInventoryHandler.HANDLER_TYPE);
    }
}
