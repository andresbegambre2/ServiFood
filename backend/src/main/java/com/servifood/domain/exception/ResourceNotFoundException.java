package com.servifood.domain.exception;
public class ResourceNotFoundException extends DomainException { public ResourceNotFoundException(String resource, Object id) { super(resource + " not found: " + id); } }
