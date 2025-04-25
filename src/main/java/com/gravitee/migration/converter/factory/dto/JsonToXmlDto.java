package com.gravitee.migration.converter.factory.dto;

public record JsonToXmlDto(
        String name,
        String nullValue,
        String namespaceBlockName,
        String defaultNamespaceNodeName,
        String namespaceSeparator,
        String textNodeName,
        String attributeBlockName,
        String attributePrefix,
        String invalidCharsReplacement,
        String objectRootElementName,
        String arrayRootElementName,
        String arrayItemElementName
) {}